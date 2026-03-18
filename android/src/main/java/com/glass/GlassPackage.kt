/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
package com.glass

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class GlassPackage : ReactPackage {
    override fun createNativeModules(ctx: ReactApplicationContext): List<NativeModule> = emptyList()
    override fun createViewManagers(ctx: ReactApplicationContext): List<ViewManager<*, *>> = listOf(GlassViewManager())
}
