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
import android.view.Choreographer
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window

/**
 * GlassView — Professional frosted glass for React Native.
 *
 * Strategy:
 *  - visibility = INVISIBLE until the first blur bitmap is ready.
 *    The user sees the content behind the view. No black box. Ever.
 *  - Wait 250ms after attach before the first capture attempt.
 *    This gives Image components (local or network) time to decode
 *    and paint to the GPU surface before PixelCopy reads it.
 *  - Choreographer ensures the capture fires after a real frame commit.
 *  - Prop changes (blurType, blurAmount) re-blur the stored source bitmap
 *    directly — no new screen capture, no accumulation, instant update.
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
    private var isFirstCaptureDone = false

    private val bitmapPaint  = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint()

    @Volatile private var jobGeneration = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private var capturing   = false

    private var preDrawListener:    ViewTreeObserver.OnPreDrawListener? = null
    private var autoUpdateListener: ViewTreeObserver.OnPreDrawListener? = null
    private var frameCallback:      Choreographer.FrameCallback? = null

    // Runnable for the initial delayed capture — stored so we can cancel it
    private val firstCaptureRunnable = Runnable { scheduleChoreographerCapture() }

    // How long to wait after view attaches before first capture.
    // 250ms is enough for local require() images and most network images
    // to decode and commit to the GPU surface. The view is INVISIBLE
    // during this time so the user sees zero black box or delay.
    private val FIRST_CAPTURE_DELAY_MS = 250L

    init {
        setWillNotDraw(false)
        // INVISIBLE at OS level — Android does not composite this view at all.
        // No GPU surface, no black fill, nothing shown to the user until
        // the blur bitmap is ready and we call visibility = VISIBLE.
        visibility = INVISIBLE
    }

    // ── Blur params ───────────────────────────────────────────────────────────

    private data class BlurParams(val radius: Int)

    private fun computeParams(): BlurParams {
        if (explicitRadius > 0) return BlurParams(explicitRadius)
        val a = blurAmount.coerceIn(0, 100)
        if (a == 0) return BlurParams(0)
        // Linear: blurAmount 1–100 → radius 1–25
        val radius = (a * 0.25f).toInt().coerceAtLeast(1)
        return BlurParams(radius)
    }

    // ── Public setters ────────────────────────────────────────────────────────

    fun setBlurType(type: String?) {
        blurType     = type ?: "dark"
        overlayColor = if (blurType == "light") Color.argb(15, 255, 255, 255)
                       else                      Color.argb(10, 0, 0, 0)
        // Re-blur from existing source — no new capture needed
        reBlurSource()
    }

    fun setBlurAmount(amount: Int) {
        blurAmount = amount.coerceIn(0, 100)
        explicitRadius = 0
        reBlurSource()
    }

    fun setBlurRadius(radius: Int) {
        explicitRadius = radius.coerceAtLeast(1)
        reBlurSource()
    }

    fun setDownsampleFactor(factor: Int) {
        val c = factor.coerceIn(1, 4)
        if (c == downsample) return
        downsample = c
        triggerFreshCapture()
    }

    fun setOverlayColor(color: Int) { overlayColor = color; invalidate() }

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
        // Wait FIRST_CAPTURE_DELAY_MS before the first capture so images
        // have time to render. View stays INVISIBLE during this time.
        mainHandler.postDelayed(firstCaptureRunnable, FIRST_CAPTURE_DELAY_MS)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacks(firstCaptureRunnable)
        cancelPendingCapture()
        stopAutoUpdate()
        recycleSource()
        recycleBlurred()
        isFirstCaptureDone = false
        capturing = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        if (isFirstCaptureDone && (w != oldw || h != oldh)) {
            recycleSource()
            recycleBlurred()
            isFirstCaptureDone = false
            visibility = INVISIBLE
            mainHandler.removeCallbacks(firstCaptureRunnable)
            mainHandler.postDelayed(firstCaptureRunnable, FIRST_CAPTURE_DELAY_MS)
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (!enabled) { super.onDraw(canvas); return }
        // At this point visibility == VISIBLE so blur is always ready
        blurredBitmap?.takeIf { !it.isRecycled }?.let { bmp ->
            canvas.drawBitmap(bmp, null, Rect(0, 0, width, height), bitmapPaint)
        }
        overlayPaint.color = overlayColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    // ── Fresh capture ─────────────────────────────────────────────────────────

    private fun triggerFreshCapture() {
        mainHandler.removeCallbacks(firstCaptureRunnable)
        recycleSource()
        recycleBlurred()
        isFirstCaptureDone = false
        visibility = INVISIBLE
        mainHandler.postDelayed(firstCaptureRunnable, FIRST_CAPTURE_DELAY_MS)
    }

    // ── Choreographer capture ─────────────────────────────────────────────────
    //
    // OnPreDrawListener fires just BEFORE a frame is drawn.
    // Choreographer.FrameCallback fires at VSYNC — after the previous frame
    // has been committed to the GPU surface.
    // PixelCopy at this point reads real committed pixel content.

    private fun scheduleChoreographerCapture() {
        if (!isAttachedToWindow || width <= 0 || height <= 0) return
        cancelPendingCapture()

        preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(this)
                preDrawListener = null
                val cb = Choreographer.FrameCallback {
                    frameCallback = null
                    captureScreen()
                }
                frameCallback = cb
                Choreographer.getInstance().postFrameCallback(cb)
                return true
            }
        }.also { listener ->
            if (viewTreeObserver.isAlive) viewTreeObserver.addOnPreDrawListener(listener)
        }
    }

    private fun cancelPendingCapture() {
        preDrawListener?.let { if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(it) }
        preDrawListener = null
        frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        frameCallback = null
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    private fun captureScreen() {
        if (!enabled || width <= 0 || height <= 0 || capturing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) captureWithPixelCopy()
        else captureWithSoftwareDraw()
    }

    private fun captureWithPixelCopy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val window = getActivityWindow() ?: run {
            scheduleChoreographerCapture(); return
        }

        val loc = IntArray(2)
        getLocationInWindow(loc)
        if (loc[0] < 0 || loc[1] < 0) {
            scheduleChoreographerCapture(); return
        }

        val srcRect = Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
        val sw = (width  / downsample).coerceAtLeast(2)
        val sh = (height / downsample).coerceAtLeast(2)

        val dest = try {
            Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) { return }

        capturing = true

        try {
            PixelCopy.request(window, srcRect, dest, { result ->
                capturing = false
                if (result == PixelCopy.SUCCESS) {
                    recycleSource()
                    sourceBitmap = dest
                    processSource(computeParams())
                } else {
                    dest.recycle()
                    // Retry on next frame
                    scheduleChoreographerCapture()
                }
            }, mainHandler)
        } catch (e: Exception) {
            capturing = false
            dest.recycle()
            scheduleChoreographerCapture()
        }
    }

    private fun captureWithSoftwareDraw() {
        val root = rootView ?: return
        try {
            val sw = (width  / downsample).coerceAtLeast(2)
            val sh = (height / downsample).coerceAtLeast(2)
            val raw = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            val cvs = Canvas(raw)
            val rl  = IntArray(2); val sl = IntArray(2)
            root.getLocationInWindow(rl)
            getLocationInWindow(sl)
            cvs.scale(sw.toFloat() / width, sh.toFloat() / height)
            cvs.translate((rl[0] - sl[0]).toFloat(), (rl[1] - sl[1]).toFloat())
            root.draw(cvs)
            capturing = false
            recycleSource()
            sourceBitmap = raw
            processSource(computeParams())
        } catch (e: Exception) {
            capturing = false
            scheduleChoreographerCapture()
        }
    }

    // ── Re-blur from stored source (prop changes only) ────────────────────────

    private fun reBlurSource() {
        val src = sourceBitmap
        if (src == null || src.isRecycled) return
        processSource(computeParams())
    }

    // ── Process source → blurred bitmap ───────────────────────────────────────

    private fun processSource(params: BlurParams) {
        val src   = sourceBitmap?.takeIf { !it.isRecycled } ?: return
        val myGen = ++jobGeneration
        val copy  = try {
            src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) { return }

        Thread {
            try {
                val processed = if (params.radius > 0) boxBlur(copy, params.radius)
                                else copy
                if (processed !== copy) copy.recycle()

                mainHandler.post {
                    if (myGen != jobGeneration) { processed.recycle(); return@post }
                    recycleBlurred()
                    blurredBitmap = try {
                        Bitmap.createScaledBitmap(
                            processed,
                            width.coerceAtLeast(1),
                            height.coerceAtLeast(1),
                            true
                        )
                    } catch (e: Exception) { processed }
                    if (blurredBitmap !== processed) processed.recycle()

                    if (!isFirstCaptureDone) {
                        isFirstCaptureDone = true
                        // Reveal the view — blur bitmap already ready,
                        // user sees final result with zero flash
                        visibility = VISIBLE
                    }
                    invalidate()
                }
            } catch (e: Exception) { copy.recycle() }
        }.also { it.name = "GlassBlur-Worker" }.start()
    }

    // ── Box Blur (3 passes ≈ Gaussian, O(w*h) regardless of radius) ──────────

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) { blurH(px, tmp, w, h, radius); blurV(tmp, px, w, h, radius) }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    private fun blurH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w; var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) {
                val p = src[row + i.coerceIn(0, w - 1)]
                rs += (p shr 16) and 0xff; gs += (p shr 8) and 0xff; bs += p and 0xff
            }
            dst[row] = pack(rs / div, gs / div, bs / div)
            for (x in 1 until w) {
                val rem = src[row + (x - r - 1).coerceIn(0, w - 1)]
                val add = src[row + (x + r    ).coerceIn(0, w - 1)]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr  8) and 0xff) - ((rem shr  8) and 0xff)
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
                rs += (p shr 16) and 0xff; gs += (p shr 8) and 0xff; bs += p and 0xff
            }
            dst[x] = pack(rs / div, gs / div, bs / div)
            for (y in 1 until h) {
                val rem = src[(y - r - 1).coerceIn(0, h - 1) * w + x]
                val add = src[(y + r    ).coerceIn(0, h - 1) * w + x]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr  8) and 0xff) - ((rem shr  8) and 0xff)
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

    // ── Auto update ───────────────────────────────────────────────────────────

    private fun startAutoUpdate() {
        stopAutoUpdate()
        if (viewTreeObserver.isAlive) {
            autoUpdateListener = ViewTreeObserver.OnPreDrawListener {
                val cb = Choreographer.FrameCallback { if (!capturing) captureScreen() }
                Choreographer.getInstance().postFrameCallback(cb)
                true
            }.also { viewTreeObserver.addOnPreDrawListener(it) }
        }
    }

    private fun stopAutoUpdate() {
        autoUpdateListener?.let {
            if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(it)
        }
        autoUpdateListener = null
    }
}
