/**
 * Android native component registration.
 * requireNativeComponent works on Old Architecture (Paper) and
 * New Architecture (Fabric) via the interop layer (RN 0.68+).
 */
import { requireNativeComponent } from 'react-native';
import type { ViewProps, ColorValue } from 'react-native';

export interface NativeGlassViewAndroidProps extends ViewProps {
  blurType?: 'dark' | 'light';
  blurAmount?: number;
  blurRadius?: number;
  overlayColor?: ColorValue;
  enabled?: boolean;
  autoUpdate?: boolean;
}

export default requireNativeComponent<NativeGlassViewAndroidProps>('GlassView');
