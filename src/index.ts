/**
 * react-native-glass
 * Main entry point.
 *
 * Metro resolves GlassView to:
 *   GlassView.ios.tsx   on iOS
 *   GlassView.android.tsx on Android
 *
 * Usage:
 *   import GlassView from 'react-native-glass';
 *   <GlassView blurType="dark" blurAmount={20} style={StyleSheet.absoluteFill} />
 */
import type { View } from 'react-native';
import type { GlassViewProps, BlurType } from './types';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const GlassViewModule = require('./GlassView');

const GlassView = GlassViewModule.default as React.ForwardRefExoticComponent<
  GlassViewProps & React.RefAttributes<View>
>;

export default GlassView;
export type { GlassViewProps, BlurType };
