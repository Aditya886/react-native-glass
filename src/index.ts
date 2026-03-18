/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
import type { View } from 'react-native';
import type { GlassViewProps, BlurType } from './types';

// Metro resolves GlassView.ios.tsx on iOS, GlassView.android.tsx on Android
// eslint-disable-next-line @typescript-eslint/no-var-requires
const GlassViewModule = require('./GlassView');

const GlassView = GlassViewModule.default as React.ForwardRefExoticComponent<
  GlassViewProps & React.RefAttributes<View>
>;

export default GlassView;
export type { GlassViewProps, BlurType };
