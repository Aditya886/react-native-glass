/**
 * react-native.config.js
 *
 * Minimal config for React Native autolinking.
 * Works with RN 0.60 through 0.76+.
 *
 * Do NOT add packageImportPath or packageInstance here —
 * those old fields break react-native config in RN 0.71+.
 * Autolinking finds GlassPackage automatically via the manifest.
 */
module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
      },
      ios: {
        podspecPath: './react-native-glass.podspec',
      },
    },
  },
};