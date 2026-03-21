# react-native-glass

A high-quality **frosted glass blur effect** for React Native — iOS and Android.

Powered by `UIVisualEffectView` on iOS and [Dimezis/BlurView](https://github.com/Dimezis/BlurView) on Android. Zero custom blur code — just the best engines available on each platform.

---

<table>
  <tr>
    <td align="center">
      <img
        src="https://raw.githubusercontent.com/aditya886/react-native-glass/main/screenshots/dark.png"
        width="220"
        alt="Dark blur"
      />
      <br /><sub><b>blurType="dark"</b></sub>
    </td>
    <td align="center">
      <img
        src="https://raw.githubusercontent.com/aditya886/react-native-glass/main/screenshots/light.png"
        width="220"
        alt="Light blur"
      />
      <br /><sub><b>blurType="light"</b></sub>
    </td>
  </tr>
</table>

> **Add your screenshots:**
> 1. Take screenshots on a real device
> 2. Save as `screenshots/dark.png` and `screenshots/light.png`
> 3. Push to GitHub — images appear here automatically

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

### Basic

```tsx
import React from 'react';
import { View, Image, Text, StyleSheet } from 'react-native';
import GlassView from 'react-native-glass';

export default function Card() {
  return (
    <View style={styles.container}>
      <Image
        source={{ uri: 'https://example.com/photo.jpg' }}
        style={StyleSheet.absoluteFill}
        resizeMode="cover"
      />

      {/* Everything rendered BEFORE GlassView will be blurred */}
      <GlassView
        style={StyleSheet.absoluteFill}
        blurType="dark"
        blurAmount={20}
      />

      {/* Content rendered AFTER GlassView is NOT blurred */}
      <Text style={styles.text}>On top of blur</Text>
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
  text: { color: '#fff', fontWeight: '700', fontSize: 16, padding: 16 },
});
```

---

## Props

| Property | Values | Default | Platform |
|---|---|---|---|
| `blurType` | `'dark'` `'light'` `'glass'` | `'dark'` | iOS + Android |
| `blurAmount` | `0` – `100` | `10` | iOS + Android |
| `blurRadius` | `0` – `25` | — | Android only |
| `overlayColor` | RGBA color string | Based on `blurType` | Android only |
| `enabled` | `boolean` | `true` | iOS + Android |

---

## blurType

| Value | Description |
|---|---|
| `dark` | Blur + subtle dark tint — for light backgrounds |
| `light` | Blur + subtle light tint — for dark backgrounds |
| `glass` | Pure blur — zero tint, transparent overlay |

---

## blurAmount scale

| `blurAmount` | Radius | Effect |
|---|---|---|
| `1` | `1.0px` | barely visible |
| `5` | `2.0px` | very subtle |
| `10` | `3.2px` | light frost |
| `25` | `7.1px` | soft glass |
| `50` | `13.1px` | medium blur |
| `75` | `19.2px` | heavy blur |
| `100` | `25.0px` | maximum |

---

## Important rules

### 1. Parent needs `overflow: 'hidden'` for `borderRadius`

```tsx
<View style={{ borderRadius: 20, overflow: 'hidden' }}>
  <Image source={bg} style={StyleSheet.absoluteFill} />
  <GlassView blurType="dark" blurAmount={20} style={StyleSheet.absoluteFill} />
</View>
```

### 2. Give `GlassView` explicit `width` and `height` for partial overlays

```tsx
// ✅ Correct
<GlassView style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 80 }} />

// ❌ No height — GlassView is 0px tall
<GlassView style={{ position: 'absolute', bottom: 0, left: 0, right: 0 }} />
```

### 3. Content goes in a sibling View — not inside GlassView

```tsx
// ✅ Correct
<GlassView blurType="dark" blurAmount={20} style={glassStyle} />
<View style={glassStyle} pointerEvents="none">
  <Text>Unblurred content</Text>
</View>

// ❌ Content inside GlassView gets blurred too
<GlassView blurType="dark" blurAmount={20} style={glassStyle}>
  <Text>This will be blurred</Text>
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
        <GlassView blurType="dark" blurAmount={20} style={StyleSheet.absoluteFill} />
        <Text style={styles.text}>Full overlay</Text>
      </View>

      {/* Bottom sheet */}
      <View style={styles.card}>
        <Image source={BG} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <GlassView blurType="light" blurAmount={15} style={styles.sheet} />
        <View style={styles.sheet} pointerEvents="none">
          <Text style={styles.text}>Bottom sheet</Text>
        </View>
      </View>

      {/* Pure glass — no tint */}
      <View style={styles.card}>
        <Image source={BG} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <GlassView blurType="glass" blurAmount={30} style={StyleSheet.absoluteFill} />
        <Text style={styles.text}>Glass — pure blur</Text>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 200,
    borderRadius: 18,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  sheet: {
    position: 'absolute',
    bottom: 0, left: 0, right: 0,
    height: 80,
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  text: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
```

---

## How it works

**iOS** — `UIVisualEffectView` backed by the system GPU compositor. Real-time blur, zero CPU cost.

**Android API 31+** — `RenderEffectBlur` via [Dimezis/BlurView](https://github.com/Dimezis/BlurView). GPU compositor handles blur in the render pipeline.

**Android API 21–30** — `RenderScriptBlur` via [Dimezis/BlurView](https://github.com/Dimezis/BlurView). Hardware-accelerated RenderScript blur.

Works with images, videos, GIFs, Lottie animations — any content behind the view.

---

## Platform support

| Platform | Min version | Engine |
|---|---|---|
| iOS | 12.0+ | `UIVisualEffectView` |
| Android | API 21+ (Android 5.0+) | `RenderEffectBlur` (API 31+) · `RenderScriptBlur` (API 21–30) |

## React Native compatibility

| React Native | Support |
|---|---|
| 0.60 – 0.72 | ✅ Old Architecture |
| 0.73 – 0.76+ | ✅ New Architecture (Fabric interop) |

---

## License

MIT © 2025 Aditya

[GitHub](https://github.com/aditya886/react-native-glass) · [npm](https://www.npmjs.com/package/react-native-glass) · [Issues](https://github.com/aditya886/react-native-glass/issues)
