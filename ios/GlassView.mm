/*
 * react-native-glass
 * Copyright (c) 2025 Aditya. All rights reserved.
 */
#import "GlassView.h"

// ── GlassView implementation ──────────────────────────────────────────────────

@interface GlassView ()
@property (nonatomic, strong) UIVisualEffectView *effectView;
@property (nonatomic, strong) UIView             *fallbackView;
@end

@implementation GlassView

- (instancetype)init {
  if (self = [super init]) {
    _blurType   = @"dark";
    _blurAmount = 10;
    [self setupViews];
    [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(accessibilityChanged)
             name:UIAccessibilityReduceTransparencyStatusDidChangeNotification
           object:nil];
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)setupViews {
  self.backgroundColor = [UIColor clearColor];

  _effectView = [[UIVisualEffectView alloc] initWithEffect:[self currentEffect]];
  _effectView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  [self addSubview:_effectView];

  _fallbackView = [[UIView alloc] init];
  _fallbackView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  _fallbackView.hidden = YES;
  [self addSubview:_fallbackView];

  [self applyAccessibility];
}

- (UIBlurEffect *)currentEffect {
  if (@available(iOS 13.0, *)) {
    UIBlurEffectStyle style = [self.blurType isEqualToString:@"light"]
      ? UIBlurEffectStyleSystemMaterialLight
      : UIBlurEffectStyleSystemMaterialDark;
    return [UIBlurEffect effectWithStyle:style];
  }
  UIBlurEffectStyle style = [self.blurType isEqualToString:@"light"]
    ? UIBlurEffectStyleLight
    : UIBlurEffectStyleDark;
  return [UIBlurEffect effectWithStyle:style];
}

- (void)applyAccessibility {
  BOOL reduced = UIAccessibilityIsReduceTransparencyEnabled();
  _effectView.hidden   = reduced;
  _fallbackView.hidden = !reduced;
  if (reduced) {
    _fallbackView.backgroundColor = self.reducedTransparencyFallbackColor
      ?: ([self.blurType isEqualToString:@"light"]
           ? [UIColor colorWithWhite:1.0 alpha:0.85]
           : [UIColor colorWithWhite:0.1 alpha:0.85]);
  }
}

- (void)accessibilityChanged { [self applyAccessibility]; }

- (void)setBlurType:(NSString *)blurType {
  _blurType = blurType ?: @"dark";
  _effectView.effect = [self currentEffect];
  [self applyAccessibility];
}

- (void)setBlurAmount:(NSInteger)blurAmount { _blurAmount = blurAmount; }

- (void)setReducedTransparencyFallbackColor:(UIColor *)color {
  _reducedTransparencyFallbackColor = color;
  [self applyAccessibility];
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  UIView *hit = [super hitTest:point withEvent:event];
  return hit == self ? nil : hit;
}

- (void)layoutSubviews {
  [super layoutSubviews];
  _effectView.frame   = self.bounds;
  _fallbackView.frame = self.bounds;
}

@end

// ── GlassViewManager implementation ──────────────────────────────────────────

@implementation GlassViewManager

RCT_EXPORT_MODULE(GlassView)

- (UIView *)view { return [[GlassView alloc] init]; }

RCT_EXPORT_VIEW_PROPERTY(blurType,   NSString)
RCT_EXPORT_VIEW_PROPERTY(blurAmount, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(reducedTransparencyFallbackColor, UIColor)

@end
