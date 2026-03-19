package com.glass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window

/**
 * GlassView — Native Android blur view
 *
 * Architecture:
 *   - sourceBitmap  = one clean screen capture (no blur, no overlay)
 *                     Only re-captured on: first attach, size change, autoUpdate
 *                     NEVER re-captured when props change → prevents accumulation
 *   - blurredBitmap = sourceBitmap after blur + alpha blend
 *                     Re-generated on every prop change from sourceBitmap
 *   - isReady       = false until first blurredBitmap is ready
 *                     onDraw draws nothing while false → transparent → no flash
 *   - isCapturing   = true while PixelCopy is in flight
 *                     onDraw draws nothing → GPU surface is clean for capture
 *
 * Compatibility: API 21+ (Android 5.0+)
 * Blur engine: PixelCopy (API 26+) / software draw fallback (API 21-25)
 * No RenderScript, no deprecated APIs
 */
class GlassView(context: Context) : View(context) {

    // ── Config ────────────────────────────────────────────────────────────────

    private var blurAmount: Int     = 10
    private var explicitRadius: Int = 0
    private var downsample: Int     = 2
    private var overlayColor: Int   = Color.argb(10, 0, 0, 0)
    private var enabled: Boolean    = true
    private var autoUpdate: Boolean = false
    private var blurType: String    = "dark"

    // ── State ─────────────────────────────────────────────────────────────────

    @Volatile private var sourceBitmap:  Bitmap? = null
    @Volatile private var blurredBitmap: Bitmap? = null

    private var isReady     = false
    private var isCapturing = false

    private val bitmapPaint  = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint()

    // Job generation — discards stale results when props change rapidly
    @Volatile private var jobGeneration = 0

    private var preDrawListener:      ViewTreeObserver.OnPreDrawListener? = null
    private var initialFrameListener: ViewTreeObserver.OnPreDrawListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var capturing   = false

    // Wait 6 pre-draw frames before first capture so RN has rendered its JS tree
    private val INITIAL_FRAME_SKIP = 6

    init {
        setWillNotDraw(false)
        // Critical: prevents Android filling view bounds with window background
        // color (black/gray) before onDraw runs
        setBackgroundColor(Color.TRANSPARENT)
    }

    // ── Blur parameter derivation ─────────────────────────────────────────────
    //
    //  blurAmount │ radius │ alpha  │ perceived effect
    //  ───────────┼────────┼────────┼──────────────────────────
    //      0      │   1    │   0%   │ no blur — overlay only
    //      1      │   1    │  10%   │ barely visible
    //      5      │   1    │  50%   │ subtle frost
    //     10      │   1    │ 100%   │ 1px full blur
    //     20      │  11    │ 100%   │ soft glass
    //     50      │  41    │ 100%   │ medium
    //     80      │  71    │ 100%   │ heavy
    //    100      │  91    │ 100%   │ maximum

    private data class BlurParams(val radius: Int, val alpha: Float)

    private fun computeParams(): BlurParams {
        if (explicitRadius > 0) return BlurParams(explicitRadius, 1f)
        val a = blurAmount.coerceIn(0, 100)
        return when {
            a == 0  -> BlurParams(1, 0f)
            a <= 10 -> BlurParams(1, a / 10f)
            else    -> BlurParams(1 + (a - 10), 1f)
        }
    }

    // ── Public setters (called by GlassViewManager on every prop change) ──────

    fun setBlurType(type: String?) {
        blurType     = type ?: "dark"
        overlayColor = if (blurType == "light") {
            Color.argb(20, 255, 255, 255)  // rgba(255,255,255,20)
        } else {
            Color.argb(10, 0, 0, 0)         // rgba(0,0,0,10)
        }
        reBlurSource()  // re-process existing source — NO new screen capture
    }

    fun setBlurAmount(amount: Int) {
        blurAmount     = amount.coerceIn(0, 100)
        explicitRadius = 0
        reBlurSource()  // re-process existing source — NO new screen capture
    }

    fun setBlurRadius(radius: Int) {
        explicitRadius = radius.coerceAtLeast(1)
        reBlurSource()
    }

    fun setDownsampleFactor(factor: Int) {
        val clamped = factor.coerceIn(1, 4)
        if (clamped == downsample) return  // no change → skip
        downsample = clamped
        recycleSource()
        scheduleInitialCapture()
    }

    fun setOverlayColor(color: Int) {
        overlayColor = color
        invalidate()
    }

    fun setBlurEnabled(value: Boolean) {
        enabled = value
        if (enabled) reBlurSource() else { recycleBlurred(); invalidate() }
    }

    fun setAutoUpdate(value: Boolean) {
        autoUpdate = value
        if (value) startAutoUpdate() else stopAutoUpdate()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoUpdate) startAutoUpdate()
        scheduleInitialCapture()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelInitialFrameListener()
        stopAutoUpdate()
        recycleSource()
        recycleBlurred()
        isReady     = false
        isCapturing = false
        capturing   = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        recycleSource()
        recycleBlurred()
        isReady     = false
        isCapturing = false
        scheduleInitialCapture()
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (!enabled) { super.onDraw(canvas); return }

        // Draw nothing while capturing or not yet ready → transparent view
        // This prevents: (1) initial flash, (2) PixelCopy capturing our own pixels
        if (isCapturing || !isReady) return

        blurredBitmap?.takeIf { !it.isRecycled }?.let { bmp ->
            canvas.drawBitmap(bmp, null, Rect(0, 0, width, height), bitmapPaint)
        }
        overlayPaint.color = overlayColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    // ── Initial capture scheduling ─────────────────────────────────────────────

    private fun scheduleInitialCapture() {
        cancelInitialFrameListener()
        if (!isAttachedToWindow || width <= 0 || height <= 0) return
        var skipped = 0
        initialFrameListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (++skipped >= INITIAL_FRAME_SKIP) {
                    cancelInitialFrameListener()
                    // Post to next frame so capture runs after current draw pass
                    mainHandler.post { captureScreen() }
                }
                return true
            }
        }.also { listener ->
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnPreDrawListener(listener)
            }
        }
    }

    private fun cancelInitialFrameListener() {
        initialFrameListener?.let { listener ->
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(listener)
            }
        }
        initialFrameListener = null
    }

    // ── Screen capture dispatcher ─────────────────────────────────────────────

    private fun captureScreen() {
        if (!enabled || width <= 0 || height <= 0 || capturing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureWithPixelCopy()
        } else {
            captureWithSoftwareDraw()
        }
    }

    // ── PixelCopy (API 26+) ───────────────────────────────────────────────────

    private fun captureWithPixelCopy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val window = getActivityWindow() ?: run {
            // Retry if window not available yet
            mainHandler.postDelayed({ captureScreen() }, 100)
            return
        }

        val loc = IntArray(2)
        getLocationInWindow(loc)

        // Guard: view must be fully on-screen
        if (loc[0] < 0 || loc[1] < 0 || width <= 0 || height <= 0) {
            mainHandler.postDelayed({ captureScreen() }, 100)
            return
        }

        val srcRect = Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
        val sw = (width  / downsample).coerceAtLeast(2)
        val sh = (height / downsample).coerceAtLeast(2)

        val dest = try {
            Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            return
        }

        capturing   = true
        isCapturing = true   // onDraw returns early → GPU shows clean background
        invalidate()         // flush transparent frame to GPU

        // Post to next frame — GPU must flush the transparent onDraw before
        // PixelCopy reads the surface
        mainHandler.post {
            try {
                PixelCopy.request(
                    window, srcRect, dest,
                    { result ->
                        capturing   = false
                        isCapturing = false
                        if (result == PixelCopy.SUCCESS) {
                            recycleSource()
                            sourceBitmap = dest
                            processSource(computeParams())
                        } else {
                            dest.recycle()
                            // Retry after delay — surface may not be ready yet
                            mainHandler.postDelayed({ captureScreen() }, 200)
                        }
                    },
                    mainHandler
                )
            } catch (e: Exception) {
                // PixelCopy can throw on some OEM devices
                capturing   = false
                isCapturing = false
                dest.recycle()
                // Fall back to software draw
                mainHandler.postDelayed({ captureWithSoftwareDraw() }, 100)
            }
        }
    }

    // ── Software draw fallback (API 21-25) ────────────────────────────────────

    private fun captureWithSoftwareDraw() {
        val root = rootView ?: return
        try {
            val sw = (width  / downsample).coerceAtLeast(2)
            val sh = (height / downsample).coerceAtLeast(2)
            val raw = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            val cvs = Canvas(raw)

            val rootLoc = IntArray(2)
            val selfLoc = IntArray(2)
            root.getLocationInWindow(rootLoc)
            getLocationInWindow(selfLoc)

            cvs.scale(sw.toFloat() / width, sh.toFloat() / height)
            cvs.translate(
                (rootLoc[0] - selfLoc[0]).toFloat(),
                (rootLoc[1] - selfLoc[1]).toFloat()
            )

            // isCapturing=true → onDraw draws nothing → root.draw gets clean bg
            isCapturing = true
            invalidate()
            root.draw(cvs)
            isCapturing = false

            capturing = false
            recycleSource()
            sourceBitmap = raw
            processSource(computeParams())

        } catch (e: Exception) {
            isCapturing = false
            capturing   = false
            invalidate()
        }
    }

    // ── Re-blur from stored source (prop changes) ─────────────────────────────
    // This is called for EVERY prop change — blurType, blurAmount, etc.
    // It NEVER triggers a new screen capture. This is what prevents accumulation.

    private fun reBlurSource() {
        val src = sourceBitmap
        if (src == null || src.isRecycled) {
            // No source yet — capture will call processSource when done
            return
        }
        processSource(computeParams())
    }

    // ── Process source → blurred bitmap ───────────────────────────────────────

    private fun processSource(params: BlurParams) {
        val src = sourceBitmap?.takeIf { !it.isRecycled } ?: return

        // Snapshot generation and source before entering background thread
        val myGen = ++jobGeneration
        val copy  = try {
            src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            return
        }

        Thread {
            try {
                val processed = applyBlur(copy, params)
                copy.recycle()

                mainHandler.post {
                    // Discard stale results from rapid prop changes
                    if (myGen != jobGeneration) {
                        processed.recycle()
                        return@post
                    }
                    recycleBlurred()
                    blurredBitmap = try {
                        Bitmap.createScaledBitmap(
                            processed,
                            width.coerceAtLeast(1),
                            height.coerceAtLeast(1),
                            true
                        )
                    } catch (e: Exception) {
                        processed
                    }
                    if (blurredBitmap !== processed) processed.recycle()
                    isReady = true
                    invalidate()
                }
            } catch (e: Exception) {
                copy.recycle()
            }
        }.also { it.name = "GlassBlur-${System.nanoTime()}" }.start()
    }

    // ── Blur pipeline: box blur + alpha blend ─────────────────────────────────

    private fun applyBlur(src: Bitmap, params: BlurParams): Bitmap {
        // alpha=0 → skip blur entirely, return copy of source
        if (params.alpha <= 0f) {
            return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val blurred = boxBlur(src, params.radius)

        // alpha=1 → fully blurred, no blending needed
        if (params.alpha >= 1f) return blurred

        // Partial blend: draw blurred at `alpha` opacity over sharp original
        // This gives sub-pixel blur control for small blurAmount values
        val result = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val paint  = Paint().apply {
            isAntiAlias = false
            alpha = (params.alpha * 255f).toInt().coerceIn(0, 255)
        }
        Canvas(result).drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return result
    }

    // ── Box Blur ──────────────────────────────────────────────────────────────
    // Single-pass separable box blur (horizontal then vertical).
    // Time complexity: O(w*h) per axis regardless of radius (sliding window).
    // No lookup tables, no array bounds issues, no deprecated APIs.

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w  = src.width
        val h  = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        blurH(px, tmp, w, h, radius)
        blurV(tmp, px, w, h, radius)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    private fun blurH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) {
                val p = src[row + i.coerceIn(0, w - 1)]
                rs += (p shr 16) and 0xff
                gs += (p shr 8)  and 0xff
                bs +=  p         and 0xff
            }
            dst[row] = pack(rs / div, gs / div, bs / div)
            for (x in 1 until w) {
                val rem = src[row + (x - r - 1).coerceIn(0, w - 1)]
                val add = src[row + (x + r    ).coerceIn(0, w - 1)]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr 8)  and 0xff) - ((rem shr 8)  and 0xff)
                bs += ( add         and 0xff) - ( rem         and 0xff)
                dst[row + x] = pack(rs / div, gs / div, bs / div)
            }
        }
    }

    private fun blurV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) {
                val p = src[i.coerceIn(0, h - 1) * w + x]
                rs += (p shr 16) and 0xff
                gs += (p shr 8)  and 0xff
                bs +=  p         and 0xff
            }
            dst[x] = pack(rs / div, gs / div, bs / div)
            for (y in 1 until h) {
                val rem = src[(y - r - 1).coerceIn(0, h - 1) * w + x]
                val add = src[(y + r    ).coerceIn(0, h - 1) * w + x]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr 8)  and 0xff) - ((rem shr 8)  and 0xff)
                bs += ( add         and 0xff) - ( rem         and 0xff)
                dst[y * w + x] = pack(rs / div, gs / div, bs / div)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun pack(r: Int, g: Int, b: Int): Int =
        (0xff000000.toInt()) or (r shl 16) or (g shl 8) or b

    // ── Window helper ─────────────────────────────────────────────────────────

    private fun getActivityWindow(): Window? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.window
            ctx = ctx.baseContext
        }
        return null
    }

    // ── Bitmap helpers ────────────────────────────────────────────────────────

    private fun recycleSource() {
        sourceBitmap?.takeIf { !it.isRecycled }?.recycle()
        sourceBitmap = null
    }

    private fun recycleBlurred() {
        blurredBitmap?.takeIf { !it.isRecycled }?.recycle()
        blurredBitmap = null
    }

    // ── Auto-update ───────────────────────────────────────────────────────────

    private fun startAutoUpdate() {
        stopAutoUpdate()
        if (viewTreeObserver.isAlive) {
            preDrawListener = ViewTreeObserver.OnPreDrawListener {
                if (!capturing) post { captureScreen() }
                true
            }.also { viewTreeObserver.addOnPreDrawListener(it) }
        }
    }

    private fun stopAutoUpdate() {
        preDrawListener?.let { listener ->
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(listener)
            }
        }
        preDrawListener = null
    }
}
