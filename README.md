# react-native-glass

A high-quality **frosted glass blur effect** for React Native — iOS and Android.

Built from scratch. Zero dependencies. Works on every React Native version.

---

## Installation

```bash
npm install react-native-glass
```

**iOS — install pods:**

```bash
cd ios && pod install
```

**Android — nothing extra needed.** Autolinking handles everything.

---

## Usage

```tsx
import GlassView from 'react-native-glass';
```

### Basic example

```tsx
import React from 'react';
import { View, Image, Text, StyleSheet } from 'react-native';
import GlassView from 'react-native-glass';

export default function Card() {
  return (
    <View style={styles.container}>
      <Image
        source={{ uri: 'https://example.com/photo.jpg' }}
        style={styles.absolute}
        resizeMode="cover"
      />
      <Text style={styles.absolute}>This text will be blurred</Text>

      {/* Everything rendered BEFORE GlassView will be blurred */}
      <GlassView
        style={styles.absolute}
        blurType="dark"
        blurAmount={20}
      />

      <Text style={styles.top}>I am NOT blurred — rendered on top</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
    height: 200,
    overflow: 'hidden',
    borderRadius: 16,
  },
  absolute: {
    position: 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
  },
  top: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 16,
    padding: 16,
  },
});
```

---

## Props

| Property | Possible Values | Default | Platform |
|---|---|---|---|
| `blurType` | `'dark'` \| `'light'` | `'dark'` | iOS + Android |
| `blurAmount` | `0` – `100` | `10` | iOS + Android |
| `blurRadius` | `0` – `25` | Derived from `blurAmount` | Android only |
| `overlayColor` | Any RGBA color string | Based on `blurType` | Android only |
| `autoUpdate` | `boolean` | `false` | Android only |

---

## blurType

| Value | Description |
|---|---|
| `dark` | Dark frosted glass — semi-transparent dark tint |
| `light` | Light frosted glass — semi-transparent light tint |

---

## blurAmount scale

| Value | Effect |
|---|---|
| `0` | No blur |
| `1` – `5` | Barely visible frost |
| `10` | Subtle glass |
| `20` – `30` | Soft frosted glass |
| `50` | Medium blur |
| `80` | Heavy blur |
| `100` | Maximum frosted glass |

---

## autoUpdate (Android only)

Re-captures the background on every frame. Use when the background content
changes dynamically — for example, a video, animation, or live camera feed.

```tsx
<GlassView
  blurType="dark"
  blurAmount={20}
  autoUpdate={true}
  style={StyleSheet.absoluteFill}
/>
```

> Has a performance cost. Only enable when the background is actively changing.

---

## Important rules

### 1. Parent needs `overflow: 'hidden'` for `borderRadius` to clip the blur

```tsx
// ✅ Correct
<View style={{ borderRadius: 20, overflow: 'hidden' }}>
  <Image source={bg} style={StyleSheet.absoluteFill} />
  <GlassView blurType="dark" blurAmount={20} style={StyleSheet.absoluteFill} />
</View>

// ❌ Wrong — blur leaks outside the rounded corners
<View style={{ borderRadius: 20 }}>
  <GlassView ... />
</View>
```

### 2. GlassView needs explicit `width` and `height` when used as a partial overlay

```tsx
// ✅ Correct — explicit height
<GlassView style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 80 }} />

// ❌ Wrong — no height, GlassView is 0px tall, nothing is captured
<GlassView style={{ position: 'absolute', bottom: 0, left: 0, right: 0 }} />
```

### 3. Text and content go in a sibling View — never inside GlassView

```tsx
// ✅ Correct — GlassView and content are siblings
<GlassView blurType="dark" blurAmount={20} style={cardStyle} />
<View style={cardStyle} pointerEvents="none">
  <Text>Content on top of blur</Text>
</View>

// ❌ Wrong — content inside GlassView gets blurred too
<GlassView blurType="dark" blurAmount={20} style={cardStyle}>
  <Text>This text will also be blurred</Text>
</GlassView>
```

---

## Full example

```tsx
import React from 'react';
import { View, Image, Text, StyleSheet, ScrollView } from 'react-native';
import GlassView from 'react-native-glass';

const BG = require('./assets/bg.jpeg');

export default function App() {
  return (
    <ScrollView style={{ flex: 1, backgroundColor: '#111', padding: 16 }}>

      {/* Full overlay */}
      <View style={styles.card}>
        <Image source={BG} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <GlassView
          blurType="dark"
          blurAmount={20}
          style={StyleSheet.absoluteFill}
        />
        <Text style={styles.text}>Full overlay</Text>
      </View>

      {/* Bottom sheet */}
      <View style={styles.card}>
        <Image source={BG} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <GlassView
          blurType="light"
          blurAmount={15}
          style={styles.sheet}
        />
        <View style={styles.sheet} pointerEvents="none">
          <Text style={styles.text}>Bottom sheet</Text>
        </View>
      </View>

      {/* Floating card */}
      <View style={styles.card}>
        <Image source={BG} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <GlassView
          blurType="dark"
          blurAmount={25}
          style={styles.floatBox}
        />
        <View style={styles.floatBox} pointerEvents="none">
          <Text style={styles.text}>Floating card</Text>
        </View>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 200,
    borderRadius: 18,
    overflow: 'hidden',       // required for borderRadius to clip blur
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  sheet: {
    position: 'absolute',
    bottom: 0, left: 0, right: 0,
    height: 80,               // explicit height — required for partial overlay
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  floatBox: {
    position: 'absolute',
    width: 180,
    height: 100,              // explicit height — required
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 16,
  },
});
```

---

## How it works

**iOS** — uses `UIVisualEffectView` backed by the GPU compositor. The blur is applied in real-time by the system. Zero CPU cost, no screen capture needed.

**Android** — uses `PixelCopy` (API 26+) to capture the GPU surface, then applies a separable box blur on a background thread. The view stays invisible until the first blur is ready — no black box, no flash. Automatically retries capture until real background content is detected.

---

## Platform support

| Platform | Min version | Blur engine |
|---|---|---|
| iOS | 12.0+ | `UIVisualEffectView` — real-time GPU compositing |
| Android | API 21+ (Android 5.0+) | `PixelCopy` (API 26+) · Software draw (API 21–25) |

---

## React Native compatibility

| React Native | Support |
|---|---|
| 0.60 – 0.67 | ✅ Old Architecture (Paper) |
| 0.68 – 0.72 | ✅ Old + New Architecture |
| 0.73 – 0.76+ | ✅ New Architecture (Fabric interop) |

---

## License

MIT © 2025 Aditya

---

[GitHub](https://github.com/aditya886/react-native-glass) · [npm](https://www.npmjs.com/package/react-native-glass) · [Report an issue](https://github.com/aditya886/react-native-glass/issues)
