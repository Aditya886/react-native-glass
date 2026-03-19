import type { ViewStyle } from 'react-native';

export type BlurType = 'dark' | 'light';

export interface GlassViewProps {
  /** Blur tint theme. @default 'dark' */
  blurType?: BlurType;
  /** Blur intensity 0–100. 0 = no blur, 100 = maximum. @default 10 */
  blurAmount?: number;
  /** Raw blur radius override 0–25. Android only. Overrides blurAmount when set. */
  blurRadius?: number;
  /** Custom RGBA tint over the blur. Android only. */
  overlayColor?: string;
  /** Standard React Native ViewStyle */
  style?: ViewStyle;
  children?: React.ReactNode;
}
