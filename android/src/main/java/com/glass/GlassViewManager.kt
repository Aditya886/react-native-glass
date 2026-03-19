package com.glass

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

/**
 * GlassViewManager
 * Bridges GlassView to the React Native renderer.
 * Compatible with Old Architecture (Paper) and New Architecture (Fabric interop).
 */
class GlassViewManager : SimpleViewManager<GlassView>() {

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): GlassView =
        GlassView(context)

    @ReactProp(name = "blurType")
    fun setBlurType(view: GlassView, type: String?) {
        view.setBlurType(type)
    }

    /**
     * blurAmount 0–100 (platform-agnostic intensity).
     * Scaling to pixel radius happens inside GlassView.
     */
    @ReactProp(name = "blurAmount", defaultInt = 10)
    fun setBlurAmount(view: GlassView, amount: Int) {
        view.setBlurAmount(amount)
    }

    /**
     * blurRadius — explicit pixel radius override (Android only).
     * When > 0, bypasses blurAmount scaling.
     */
    @ReactProp(name = "blurRadius", defaultInt = 0)
    fun setBlurRadius(view: GlassView, radius: Int) {
        if (radius > 0) view.setBlurRadius(radius)
    }

    /**
     * overlayColor — RGBA tint painted over the blurred surface.
     * Passed as a Color int by the React Native bridge.
     */
    @ReactProp(name = "overlayColor", customType = "Color")
    fun setOverlayColor(view: GlassView, color: Int?) {
        color?.let { view.setOverlayColor(it) }
    }

    @ReactProp(name = "enabled", defaultBoolean = true)
    fun setEnabled(view: GlassView, enabled: Boolean) {
        view.setBlurEnabled(enabled)
    }

    /**
     * autoUpdate — re-captures the background on every frame.
     * Use when the background content changes (e.g. animations, video).
     * Has performance cost — only enable when necessary.
     */
    @ReactProp(name = "autoUpdate", defaultBoolean = false)
    fun setAutoUpdate(view: GlassView, autoUpdate: Boolean) {
        view.setAutoUpdate(autoUpdate)
    }

    companion object {
        const val REACT_CLASS = "GlassView"
    }
}
