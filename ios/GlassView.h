/*
 * react-native-glass — iOS
 * GlassView.h — UIVisualEffectView wrapper + RCTViewManager
 */
#import <UIKit/UIKit.h>
#import <React/RCTViewManager.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * GlassView
 * Wraps UIVisualEffectView to provide real-time blur backed by the GPU compositor.
 * Automatically degrades to a solid color overlay when the user has enabled
 * Reduce Transparency in Accessibility settings.
 */
@interface GlassView : UIView

@property (nonatomic, copy)   NSString  *blurType;
@property (nonatomic, assign) NSInteger  blurAmount;
@property (nonatomic, strong, nullable) UIColor *reducedTransparencyFallbackColor;

@end

/**
 * GlassViewManager
 * Registers GlassView with the React Native bridge (Old + New Architecture).
 */
@interface GlassViewManager : RCTViewManager
@end

NS_ASSUME_NONNULL_END
