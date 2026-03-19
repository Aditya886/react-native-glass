/**
 * iOS native component registration.
 * requireNativeComponent works on Old Architecture (Paper) and
 * New Architecture (Fabric) via the interop layer (RN 0.68+).
 */
import { requireNativeComponent } from 'react-native';
import type { ViewProps, ColorValue } from 'react-native';

export interface NativeGlassViewProps extends ViewProps {
  blurType?: 'dark' | 'light';
  blurAmount?: number;
  reducedTransparencyFallbackColor?: ColorValue;
}

export default requireNativeComponent<NativeGlassViewProps>('GlassView');
