# Gboard RGB Pulse – Stock Gboard + Waving Border & Letter Glow

**v34.0** – Stock Gboard design kept, only border = letter waves, all keys including shift/enter/space, one-color white, multi-wave.

### What it does
- Keeps Google's default dark gray keys (no permanent white pills)
- On tap, **only that row** expands **both ways** from tapped key with white border = white letter glow
- Multi-wave: tap `q` then `p` quickly → two waves work together, up to 8 simultaneous
- No traveling line, no green tint, one-color uniform white `E8E8EC → FFFFFFFF`
- All keys same style: `q-p, a-l, shift z-m delete, ?123 globe space . enter`
- Strict typing-panel clipping, toolbar excluded, font import, reversible opt-in, original signer

### Install (LSPosed / Vector)
1. Enable module in LSPosed, scope Gboard `com.google.android.inputmethod.latin`
2. Install `Gboard-RGB-Pulse-33.0.apk` (358KB Verified v2)
3. Open **RGB Pulse 33** app → turn ON **Solid tonal tiles (Pulse Studio)** → Apply / force-stop Gboard
4. Tap any text field – keys visible immediately as stock, tap any row to see white border=letter wave

### Build and test
The build is intentionally a small command-line pipeline because the module is injected into
Gboard by LSPosed rather than launched as a conventional app. It uses JDK 11+, Android API 34,
R8/D8, `aapt`, `curl`, `openssl`, `zip`, and Debian's `libapksig-java` package. The generated
`ShaderCode.java` is always rebuilt from `shaders/field.agsl` before compilation.

```bash
cd RGBPulse
bash test.sh                         # Android-free policy and source tests
bash build.sh ../Gboard-RGB-Pulse-33.0.apk
```

A missing tool now produces an actionable error instead of a partially-built APK. GitHub Actions
runs the same policy tests on Java 17 and verifies that the checked-in shader embedding is current.

### Structure
- `RGBPulse/src/dev/rgbpulse/gboard/` – Java/AGSL Vector/LSPosed hooks
  - `KeyStyle.java` – Glass drawable draws original background + border only during wave (stock kept)
  - `StudioTiles.java` – draws letter only during wave, same color as border, static body maps for row grouping
  - `SideSweep.java` – multi-wave list, radius = easeOutCubic(progress) * max(dist to edge, 0.6*width), white one-color
  - `PulseModule.java` – invalidates all keys during wave so border animates
- `RGBPulse/res`, `assets/fonts`, `shaders/field.agsl`
- `Gboard-RGB-Pulse-33.0.apk` – signed installable

### Versions
- v30-v33: stock kept, border only during wave, multi-wave white
- v24-v26: one-color all keys ivory
- v22-v23: row expanding both ways
- v20: tile background color picker

### Workspace
Cleaned to 19MB <128MB limit – tools removed (re-downloaded by build.sh)

Original signer SHA-256: `592b78f82378ea10a909a1d960a47bd1590ac84f738ad7314ecded596445dff1`
