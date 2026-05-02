import type { ViewStyle } from 'react-native';

/**
 * dark  — Android-style dark blur theme
 * light — Android-style light blur theme
 * glass — cross-platform glass theme; iOS always renders the glass fallback
 */
export type BlurType = 'dark' | 'light' | 'glass';

export interface GlassViewProps {
  /** Blur tint theme. Android uses the value; iOS always renders the glass fallback. @default 'dark' */
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
