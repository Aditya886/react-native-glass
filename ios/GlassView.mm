/*
 * react-native-glass — iOS
 * GlassView.mm — Implementation
 */
#import "GlassView.h"

// ── GlassView ─────────────────────────────────────────────────────────────────

@interface GlassView ()
@property (nonatomic, strong) UIVisualEffectView *effectView;
@property (nonatomic, strong) UIView             *fallbackView;
@end

@implementation GlassView

- (instancetype)init {
  if (self = [super init]) {
    _blurType   = @"glass";
    _blurAmount = 10;
    [self _setupViews];
    // Listen for Reduce Transparency accessibility changes
    [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(_accessibilityChanged)
             name:UIAccessibilityReduceTransparencyStatusDidChangeNotification
           object:nil];
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)_setupViews {
  self.backgroundColor = [UIColor clearColor];

  // UIVisualEffectView — the actual blur layer
  _effectView = [[UIVisualEffectView alloc] initWithEffect:[self _currentEffect]];
  _effectView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  [self addSubview:_effectView];

  // Solid fallback — shown when Reduce Transparency is enabled
  _fallbackView = [[UIView alloc] init];
  _fallbackView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  _fallbackView.hidden = YES;
  [self addSubview:_fallbackView];

  [self _applyAccessibility];
}

// ── Effect construction ───────────────────────────────────────────────────────

- (UIBlurEffect *)_currentEffect {
  // iOS always uses a glass-like blur effect. Older versions fall back to the
  // closest native blur style so the content behind it stays visible.
  if (@available(iOS 13.0, *)) {
    UIBlurEffectStyle style = UIBlurEffectStyleSystemUltraThinMaterial;
    return [UIBlurEffect effectWithStyle:style];
  }
  // iOS 12 and earlier fallback
  UIBlurEffectStyle style = UIBlurEffectStyleExtraLight;
  return [UIBlurEffect effectWithStyle:style];
}

// ── Reduce Transparency support ───────────────────────────────────────────────

- (void)_applyAccessibility {
  BOOL reduced = UIAccessibilityIsReduceTransparencyEnabled();
  _effectView.hidden   = reduced;
  _fallbackView.hidden = !reduced;

  if (reduced) {
    _fallbackView.backgroundColor = self.reducedTransparencyFallbackColor
      ?: [UIColor colorWithWhite:1.0 alpha:0.18];
  }
}

- (void)_accessibilityChanged {
  [self _applyAccessibility];
}

// ── Property setters (called by RCTViewManager bridge) ────────────────────────

- (void)setBlurType:(NSString *)blurType {
  // iOS ignores the requested theme and always renders the glass fallback.
  // Android still consumes the actual blurType value.
  _blurType = @"glass";
  _effectView.effect = [self _currentEffect];
  [self _applyAccessibility];
}

- (void)setBlurAmount:(NSInteger)blurAmount {
  // UIVisualEffectView does not expose an intensity setter via public API.
  // blurAmount is accepted for API parity with Android but the system
  // blur style already has an appropriate intensity baked in.
  _blurAmount = blurAmount;
}

- (void)setReducedTransparencyFallbackColor:(UIColor *)color {
  _reducedTransparencyFallbackColor = color;
  [self _applyAccessibility];
}

// ── Layout ────────────────────────────────────────────────────────────────────

- (void)layoutSubviews {
  [super layoutSubviews];
  _effectView.frame   = self.bounds;
  _fallbackView.frame = self.bounds;
}

// ── Touch passthrough ─────────────────────────────────────────────────────────
// GlassView should never swallow touches — pass them to whatever is underneath.

- (nullable UIView *)hitTest:(CGPoint)point withEvent:(nullable UIEvent *)event {
  UIView *hit = [super hitTest:point withEvent:event];
  return (hit == self) ? nil : hit;
}

@end

// ── GlassViewManager ──────────────────────────────────────────────────────────

@implementation GlassViewManager

RCT_EXPORT_MODULE(GlassView)

- (UIView *)view {
  return [[GlassView alloc] init];
}

RCT_EXPORT_VIEW_PROPERTY(blurType,   NSString)
RCT_EXPORT_VIEW_PROPERTY(blurAmount, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(reducedTransparencyFallbackColor, UIColor)

@end
