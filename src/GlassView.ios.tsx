import React, { forwardRef, useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponent';
import type { GlassViewProps } from './types';

const GlassView = forwardRef<View, GlassViewProps>(
  (
    {
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
        // iOS always uses the glass path; dark/light are Android-only themes.
        blurType="glass"
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
