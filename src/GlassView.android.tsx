import React, { forwardRef, useEffect } from 'react';
import { DeviceEventEmitter, StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponentAndroid';
import type { GlassViewProps } from './types';

const GlassView = forwardRef<View, GlassViewProps>(
  (
    {
      blurType     = 'dark',
      blurAmount   = 10,
      blurRadius,
      overlayColor,
      style,
      children,
      ...rest
    },
    ref
  ) => {
    useEffect(() => {
      const sub = DeviceEventEmitter.addListener(
        'ReactNativeBlurError',
        (msg: string) => { if (__DEV__) console.error(`[GlassView] ${msg}`); }
      );
      return () => sub.remove();
    }, []);

    return (
      <NativeGlassView
        {...rest}
        ref={ref as any}
        blurType={blurType}
        blurAmount={blurAmount}
        {...(blurRadius   != null ? { blurRadius }   : {})}
        {...(overlayColor != null ? { overlayColor } : {})}
        style={StyleSheet.compose(
          { backgroundColor: 'transparent' },
          style as ViewStyle
        )}
      >
        {children}
      </NativeGlassView>
    );
  }
);

GlassView.displayName = 'GlassView';
export default GlassView;
