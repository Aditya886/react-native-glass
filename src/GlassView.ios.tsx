/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
import React, { forwardRef, useMemo } from 'react';
import { StyleSheet, View } from 'react-native';
import type { ViewStyle } from 'react-native';
import NativeGlassView from './fabric/GlassViewNativeComponent';
import type { GlassViewProps } from './types';

const GlassView = forwardRef<View, GlassViewProps>(
  ({ blurType = 'dark', blurAmount = 10, blurRadius: _r, overlayColor: _o, style, children, ...rest }, ref) => {
    const safeAmount = useMemo(() => Math.max(0, Math.min(100, Math.round(blurAmount))), [blurAmount]);
    return (
      // @ts-ignore
      <NativeGlassView
        {...rest}
        ref={ref}
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
const styles = StyleSheet.create<{ base: ViewStyle }>({ base: { backgroundColor: 'transparent' } });
export default GlassView;
