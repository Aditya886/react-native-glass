import React, { forwardRef, useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponent';
import type { GlassViewProps } from './types';

// iOS maps 'glass' to 'light' — UIVisualEffectView handles tint automatically
const IOS_BLUR_MAP: Record<string, 'dark' | 'light'> = {
  dark:  'dark',
  light: 'light',
  glass: 'light',
};

const GlassView = forwardRef<View, GlassViewProps>(
  (
    {
      blurType    = 'dark',
      blurAmount  = 10,
      blurRadius:   _r,
      overlayColor: _o,
      style,
      children,
      ...rest
    },
    ref
  ) => {
    const safeAmount  = useMemo(
      () => Math.max(0, Math.min(100, Math.round(blurAmount))),
      [blurAmount]
    );

    return (
      <NativeGlassView
        {...rest}
        ref={ref as any}
        blurType={IOS_BLUR_MAP[blurType] ?? 'dark'}
        blurAmount={safeAmount}
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
