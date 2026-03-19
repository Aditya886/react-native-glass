import React, { forwardRef, useEffect } from 'react';
import { DeviceEventEmitter, StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponentAndroid';
import type { GlassViewProps } from './types';

/**
 * GlassView — Android
 * Renders a PixelCopy-captured + box-blurred frosted glass effect.
 *
 * IMPORTANT: Do NOT pass downsampleFactor as a prop — it is managed
 * internally by GlassView.kt and must stay fixed to prevent tint accumulation.
 */
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
        (msg: string) => {
          if (__DEV__) {
            console.error(`[GlassView] Native error: ${msg}`);
          }
        }
      );
      return () => sub.remove();
    }, []);

    return (
      <NativeGlassView
        {...rest}
        ref={ref as any}
        blurType={blurType}
        blurAmount={blurAmount}
        // Only forward these when explicitly provided
        // Never derive and pass downsampleFactor — causes tint accumulation
        {...(blurRadius   != null ? { blurRadius }   : {})}
        {...(overlayColor != null ? { overlayColor } : {})}
        style={StyleSheet.compose(styles.base, style as ViewStyle)}
      >
        {children}
      </NativeGlassView>
    );
  }
);

GlassView.displayName = 'GlassView';

const styles = StyleSheet.create<{ base: ViewStyle }>({
  base: { backgroundColor: 'transparent' },
});

export default GlassView;
export type { GlassViewProps };
