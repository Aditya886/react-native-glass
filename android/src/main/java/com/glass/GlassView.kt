/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 * Unauthorized commercial use is prohibited.
 */
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

class GlassView(context: Context) : View(context) {

    private var blurAmount: Int     = 10
    private var explicitRadius: Int = 0
    private var downsample: Int     = 2
    private var overlayColor: Int   = Color.argb(10, 0, 0, 0)
    private var enabled: Boolean    = true
    private var autoUpdate: Boolean = false
    private var blurType: String    = "dark"

    private var sourceBitmap:  Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private var isReady     = false
    private var isCapturing = false

    private val bitmapPaint  = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint()

    @Volatile private var jobGeneration = 0

    private var preDrawListener:      ViewTreeObserver.OnPreDrawListener? = null
    private var initialFrameListener: ViewTreeObserver.OnPreDrawListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var capturing = false

    private val INITIAL_FRAME_SKIP = 6

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }

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

    fun setBlurType(type: String?) {
        blurType     = type ?: "dark"
        overlayColor = if (blurType == "light") Color.argb(20, 255, 255, 255)
                       else                      Color.argb(10, 0, 0, 0)
        reBlurSource()
    }

    fun setBlurAmount(amount: Int) {
        blurAmount     = amount.coerceIn(0, 100)
        explicitRadius = 0
        reBlurSource()
    }

    fun setBlurRadius(radius: Int) {
        explicitRadius = radius.coerceAtLeast(1)
        reBlurSource()
    }

    fun setDownsampleFactor(factor: Int) {
        val clamped = factor.coerceIn(1, 4)
        if (clamped == downsample) return
        downsample = clamped
        recycleSource()
        scheduleInitialCapture()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoUpdate) startAutoUpdate()
        scheduleInitialCapture()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelInitialFrameListener(); stopAutoUpdate()
        recycleSource(); recycleBlurred()
        isReady = false; isCapturing = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recycleSource(); recycleBlurred()
        isReady = false; isCapturing = false
        scheduleInitialCapture()
    }

    override fun onDraw(canvas: Canvas) {
        if (!enabled) { super.onDraw(canvas); return }
        if (isCapturing || !isReady) return
        blurredBitmap?.takeIf { !it.isRecycled }?.let { bmp ->
            canvas.drawBitmap(bmp, null, Rect(0, 0, width, height), bitmapPaint)
        }
        overlayPaint.color = overlayColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    private fun scheduleInitialCapture() {
        cancelInitialFrameListener()
        if (!isAttachedToWindow || width <= 0 || height <= 0) return
        var skipped = 0
        initialFrameListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (++skipped >= INITIAL_FRAME_SKIP) {
                    cancelInitialFrameListener()
                    mainHandler.post { captureScreen() }
                }
                return true
            }
        }.also { if (viewTreeObserver.isAlive) viewTreeObserver.addOnPreDrawListener(it) }
    }

    private fun cancelInitialFrameListener() {
        initialFrameListener?.let { if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(it) }
        initialFrameListener = null
    }

    private fun captureScreen() {
        if (!enabled || width <= 0 || height <= 0 || capturing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) captureWithPixelCopy()
        else captureWithSoftwareDraw()
    }

    private fun captureWithPixelCopy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val window = getActivityWindow() ?: return
        val loc = IntArray(2); getLocationInWindow(loc)
        if (loc[0] < 0 || loc[1] < 0) { mainHandler.postDelayed({ captureScreen() }, 100); return }
        val srcRect = Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
        val sw = (width  / downsample).coerceAtLeast(2)
        val sh = (height / downsample).coerceAtLeast(2)
        val dest = try { Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888) } catch (e: Exception) { return }
        capturing = true; isCapturing = true; invalidate()
        mainHandler.post {
            PixelCopy.request(window, srcRect, dest, { result ->
                capturing = false; isCapturing = false
                if (result == PixelCopy.SUCCESS) {
                    recycleSource(); sourceBitmap = dest; processSource(computeParams())
                } else { dest.recycle(); mainHandler.postDelayed({ captureScreen() }, 200) }
            }, mainHandler)
        }
    }

    private fun captureWithSoftwareDraw() {
        val root = rootView ?: return
        try {
            val sw = (width  / downsample).coerceAtLeast(2)
            val sh = (height / downsample).coerceAtLeast(2)
            val raw = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            val cvs = Canvas(raw)
            val rl = IntArray(2); val sl = IntArray(2)
            root.getLocationInWindow(rl); getLocationInWindow(sl)
            cvs.scale(sw.toFloat() / width, sh.toFloat() / height)
            cvs.translate((rl[0] - sl[0]).toFloat(), (rl[1] - sl[1]).toFloat())
            isCapturing = true; invalidate(); root.draw(cvs); isCapturing = false
            capturing = false; recycleSource(); sourceBitmap = raw; processSource(computeParams())
        } catch (e: Exception) { isCapturing = false; capturing = false; invalidate() }
    }

    private fun reBlurSource() {
        if (sourceBitmap == null || sourceBitmap!!.isRecycled) return
        processSource(computeParams())
    }

    private fun processSource(params: BlurParams) {
        val src = sourceBitmap?.takeIf { !it.isRecycled } ?: return
        val myGen = ++jobGeneration
        val copy  = src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        Thread {
            try {
                val processed = applyBlur(copy, params); copy.recycle()
                mainHandler.post {
                    if (myGen != jobGeneration) { processed.recycle(); return@post }
                    recycleBlurred()
                    blurredBitmap = Bitmap.createScaledBitmap(processed, width.coerceAtLeast(1), height.coerceAtLeast(1), true)
                    processed.recycle(); isReady = true; invalidate()
                }
            } catch (e: Exception) { copy.recycle() }
        }.start()
    }

    private fun applyBlur(src: Bitmap, params: BlurParams): Bitmap {
        if (params.alpha <= 0f) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val blurred = boxBlur(src, params.radius)
        if (params.alpha >= 1f) return blurred
        val result = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        Canvas(result).drawBitmap(blurred, 0f, 0f, Paint().apply { alpha = (params.alpha * 255f).toInt().coerceIn(0, 255) })
        blurred.recycle(); return result
    }

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h); blurH(px, tmp, w, h, radius); blurV(tmp, px, w, h, radius)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); out.setPixels(px, 0, w, 0, 0, w, h); return out
    }

    private fun blurH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w; var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) { val p = src[row + i.coerceIn(0, w-1)]; rs += (p shr 16) and 0xff; gs += (p shr 8) and 0xff; bs += p and 0xff }
            dst[row] = pack(rs/div, gs/div, bs/div)
            for (x in 1 until w) {
                val rem = src[row + (x-r-1).coerceIn(0, w-1)]; val add = src[row + (x+r).coerceIn(0, w-1)]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr 8)  and 0xff) - ((rem shr 8)  and 0xff)
                bs += ( add         and 0xff) - ( rem         and 0xff)
                dst[row + x] = pack(rs/div, gs/div, bs/div)
            }
        }
    }

    private fun blurV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) { val p = src[i.coerceIn(0, h-1)*w+x]; rs += (p shr 16) and 0xff; gs += (p shr 8) and 0xff; bs += p and 0xff }
            dst[x] = pack(rs/div, gs/div, bs/div)
            for (y in 1 until h) {
                val rem = src[(y-r-1).coerceIn(0, h-1)*w+x]; val add = src[(y+r).coerceIn(0, h-1)*w+x]
                rs += ((add shr 16) and 0xff) - ((rem shr 16) and 0xff)
                gs += ((add shr 8)  and 0xff) - ((rem shr 8)  and 0xff)
                bs += ( add         and 0xff) - ( rem         and 0xff)
                dst[y*w+x] = pack(rs/div, gs/div, bs/div)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun pack(r: Int, g: Int, b: Int): Int = (0xff000000.toInt()) or (r shl 16) or (g shl 8) or b

    private fun getActivityWindow(): Window? {
        var ctx = context
        while (ctx is ContextWrapper) { if (ctx is Activity) return ctx.window; ctx = ctx.baseContext }
        return null
    }

    private fun recycleSource()  { sourceBitmap?.takeIf  { !it.isRecycled }?.recycle(); sourceBitmap  = null }
    private fun recycleBlurred() { blurredBitmap?.takeIf { !it.isRecycled }?.recycle(); blurredBitmap = null }

    private fun startAutoUpdate() {
        if (viewTreeObserver.isAlive) {
            preDrawListener = ViewTreeObserver.OnPreDrawListener {
                if (!capturing) post { captureScreen() }; true
            }.also { viewTreeObserver.addOnPreDrawListener(it) }
        }
    }

    private fun stopAutoUpdate() {
        preDrawListener?.let { if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(it) }
        preDrawListener = null
    }
}
