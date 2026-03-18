/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
#import <UIKit/UIKit.h>
#import <React/RCTViewManager.h>

NS_ASSUME_NONNULL_BEGIN

// ── Native UIVisualEffectView wrapper ─────────────────────────────────────────

@interface GlassView : UIView
@property (nonatomic, copy)   NSString  *blurType;
@property (nonatomic, assign) NSInteger  blurAmount;
@property (nonatomic, strong, nullable) UIColor *reducedTransparencyFallbackColor;
@end

// ── React Native view manager ─────────────────────────────────────────────────

@interface GlassViewManager : RCTViewManager
@end

NS_ASSUME_NONNULL_END
