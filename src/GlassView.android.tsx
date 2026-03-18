/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
import React, { forwardRef, useEffect } from 'react';
import { DeviceEventEmitter, StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponentAndroid';
import type { GlassViewProps } from './types';

const GlassView = forwardRef<View, GlassViewProps>(
  ({ blurType = 'dark', blurAmount = 10, blurRadius, overlayColor, style, children, ...rest }, ref) => {
    useEffect(() => {
      const sub = DeviceEventEmitter.addListener('ReactNativeBlurError', (msg: string) => {
        throw new Error(`[GlassView] ${msg}`);
      });
      return () => sub.remove();
    }, []);

    return (
      // @ts-ignore
      <NativeGlassView
        {...rest}
        ref={ref}
        blurType={blurType}
        blurAmount={blurAmount}
        {...(blurRadius   != null ? { blurRadius }   : {})}
        {...(overlayColor != null ? { overlayColor } : {})}
        pointerEvents="none"
        style={StyleSheet.compose(styles.base, style as ViewStyle)}
      >
        {children}
      </NativeGlassView>
    );
  }
);

GlassView.displayName = 'GlassView';
const styles = StyleSheet.create<{ base: ViewStyle }>({ base: { backgroundColor: 'transparent' } });
export default GlassView;
