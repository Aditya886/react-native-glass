package com.glass

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

class GlassView(context: Context) : BlurView(context) {

    private var blurAmount: Int      = 10
    private var explicitRadius: Int  = 0
    private var blurType: String     = "dark"
    private var isSetupDone: Boolean = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    // ── Blur radius ───────────────────────────────────────────────────────────
    //
    // Linear scale: blurAmount 1–100 → radius 1.0–25.0
    // Every single value produces a visibly different blur strength.
    // RenderScript max is 25 — we stay within that limit.
    //
    //  blurAmount  │  radius
    //  ────────────┼────────
    //      1       │   1.0
    //      5       │   2.0
    //      9       │   2.9
    //     10       │   3.2
    //     25       │   7.1
    //     50       │  13.1
    //     75       │  19.2
    //    100       │  25.0

    private fun computeRadius(): Float {
        if (explicitRadius > 0) return explicitRadius.coerceAtMost(25).toFloat()
        val a = blurAmount.coerceIn(0, 100)
        if (a == 0) return 0.1f
        // Linear: a=1 → 1.0,  a=100 → 25.0
        return (1f + (a - 1) * 24f / 99f)
    }

    private fun computeOverlayColor(): Int = when (blurType) {
        "light" -> Color.argb(40, 255, 255, 255)
        "glass" -> Color.TRANSPARENT
        else    -> Color.argb(40, 0, 0, 0)
    }

    // ── Public setters ────────────────────────────────────────────────────────

    fun setGlassBlurType(type: String?) {
        blurType = type ?: "dark"
        if (isSetupDone) { setOverlayColor(computeOverlayColor()); invalidate() }
    }

    fun setGlassBlurAmount(amount: Int) {
        blurAmount = amount.coerceIn(0, 100); explicitRadius = 0
        if (isSetupDone) setBlurRadius(computeRadius())
    }

    fun setGlassBlurRadius(radius: Int) {
        explicitRadius = radius.coerceAtLeast(0)
        if (isSetupDone) setBlurRadius(computeRadius())
    }

    fun setGlassOverlayColor(color: Int) {
        if (isSetupDone) setOverlayColor(color)
    }

    fun setGlassEnabled(value: Boolean) {
        setBlurEnabled(value)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupBlurView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setBlurEnabled(false)
        isSetupDone = false
    }

    private fun setupBlurView() {
        val rootView = (rootView as? ViewGroup) ?: return
        val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectBlur()
        } else {
            @Suppress("DEPRECATION")
            RenderScriptBlur(context)
        }
        setupWith(rootView, algorithm)
            .setFrameClearDrawable(rootView.background)
            .setBlurRadius(computeRadius())
        setOverlayColor(computeOverlayColor())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            clipToOutline = true
        }
        isSetupDone = true
    }
}
