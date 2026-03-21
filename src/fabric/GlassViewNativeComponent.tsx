import { requireNativeComponent } from 'react-native';
import type { ViewProps, ColorValue } from 'react-native';

export interface NativeGlassViewProps extends ViewProps {
  blurType?: 'dark' | 'light' | 'glass';
  blurAmount?: number;
  reducedTransparencyFallbackColor?: ColorValue;
}

export default requireNativeComponent<NativeGlassViewProps>('GlassView');
