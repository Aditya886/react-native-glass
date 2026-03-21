import type { ViewStyle } from 'react-native';

/**
 * dark  — blur + subtle black tint
 * light — blur + subtle white tint
 * glass — pure blur, zero tint (true frosted glass look)
 */
export type BlurType = 'dark' | 'light' | 'glass';

export interface GlassViewProps {
  /** Blur tint theme. @default 'dark' */
  blurType?: BlurType;
  /** Blur intensity 0–100. @default 10 */
  blurAmount?: number;
  /** Raw blur radius override. Android only. */
  blurRadius?: number;
  /** Custom RGBA tint color. Android only. */
  overlayColor?: string;
  style?: ViewStyle;
  children?: React.ReactNode;
}
