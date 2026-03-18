/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
package com.glass

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class GlassViewManager : SimpleViewManager<GlassView>() {

    override fun getName() = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext) = GlassView(context)

    @ReactProp(name = "blurType")
    fun setBlurType(view: GlassView, type: String?) = view.setBlurType(type)

    @ReactProp(name = "blurAmount", defaultInt = 10)
    fun setBlurAmount(view: GlassView, amount: Int) = view.setBlurAmount(amount)

    @ReactProp(name = "blurRadius", defaultInt = 0)
    fun setBlurRadius(view: GlassView, radius: Int) { if (radius > 0) view.setBlurRadius(radius) }

    @ReactProp(name = "overlayColor", customType = "Color")
    fun setOverlayColor(view: GlassView, color: Int?) { color?.let { view.setOverlayColor(it) } }

    @ReactProp(name = "enabled", defaultBoolean = true)
    fun setEnabled(view: GlassView, enabled: Boolean) = view.setBlurEnabled(enabled)

    @ReactProp(name = "autoUpdate", defaultBoolean = false)
    fun setAutoUpdate(view: GlassView, auto: Boolean) = view.setAutoUpdate(auto)

    companion object {
        const val REACT_CLASS = "GlassView"
    }
}
