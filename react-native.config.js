/**
 * react-native.config.js
 * Required for React Native autolinking.
 * This tells RN CLI where to find the native Android and iOS modules.
 */
module.exports = {
  dependency: {
    platforms: {
      ios: {
        podspecPath: './react-native-glass.podspec',
      },
      android: {
        sourceDir: './android',
        manifestPath: './android/src/main/AndroidManifest.xml',
        packageImportPath: 'import com.glass.GlassPackage;',
        packageInstance: 'new GlassPackage()',
      },
    },
  },
};
