/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
import type { ViewStyle } from 'react-native';

export type BlurType = 'dark' | 'light';

export interface GlassViewProps {
  blurType?: BlurType;
  blurAmount?: number;
  blurRadius?: number;
  overlayColor?: string;
  style?: ViewStyle;
  children?: React.ReactNode;
}
