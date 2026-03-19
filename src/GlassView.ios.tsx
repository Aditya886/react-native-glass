import React, { forwardRef, useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponent';
import type { GlassViewProps } from './types';

/**
 * GlassView — iOS
 * Renders a UIVisualEffectView-backed frosted glass effect.
 * blurRadius and overlayColor are Android-only — silently ignored here.
 */
const GlassView = forwardRef<View, GlassViewProps>(
  (
    {
      blurType    = 'dark',
      blurAmount  = 10,
      blurRadius:    _r,   // Android-only, ignored on iOS
      overlayColor:  _o,   // Android-only, ignored on iOS
      style,
      children,
      ...rest
    },
    ref
  ) => {
    const safeAmount = useMemo(
      () => Math.max(0, Math.min(100, Math.round(blurAmount))),
      [blurAmount]
    );

    return (
      <NativeGlassView
        {...rest}
        ref={ref as any}
        blurType={blurType}
        blurAmount={safeAmount}
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
