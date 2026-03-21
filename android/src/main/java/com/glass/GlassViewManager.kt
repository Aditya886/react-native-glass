package com.glass

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class GlassViewManager : SimpleViewManager<GlassView>() {

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(ctx: ThemedReactContext): GlassView = GlassView(ctx)

    @ReactProp(name = "blurType")
    fun setBlurType(view: GlassView, type: String?) = view.setGlassBlurType(type)

    @ReactProp(name = "blurAmount", defaultInt = 10)
    fun setBlurAmount(view: GlassView, amount: Int) = view.setGlassBlurAmount(amount)

    @ReactProp(name = "blurRadius", defaultInt = 0)
    fun setBlurRadius(view: GlassView, radius: Int) {
        if (radius > 0) view.setGlassBlurRadius(radius)
    }

    @ReactProp(name = "overlayColor", customType = "Color")
    fun setOverlayColor(view: GlassView, color: Int?) {
        color?.let { view.setGlassOverlayColor(it) }
    }

    @ReactProp(name = "enabled", defaultBoolean = true)
    fun setEnabled(view: GlassView, enabled: Boolean) = view.setGlassEnabled(enabled)

    companion object {
        const val REACT_CLASS = "GlassView"
    }
}
