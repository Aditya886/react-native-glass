import { requireNativeComponent } from 'react-native';
import type { ViewProps, ColorValue } from 'react-native';

export interface NativeGlassViewAndroidProps extends ViewProps {
  blurType?: 'dark' | 'light' | 'glass';
  blurAmount?: number;
  blurRadius?: number;
  overlayColor?: ColorValue;
  enabled?: boolean;
}

export default requireNativeComponent<NativeGlassViewAndroidProps>('GlassView');
