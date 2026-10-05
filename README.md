# Gboard RGB Pulse · Pulse Studio

**Version 1.0.3** — optional key feedback, wide background effects and expressive ripples for Gboard.

[Download 1.0.3 APK](https://github.com/brhacket/gboard-rgb-pulse/releases/download/v1.0.3/Gboard-RGB-Pulse-1.0.3.apk) · [Release notes](https://github.com/brhacket/gboard-rgb-pulse/releases/tag/v1.0.3) · [All releases](https://github.com/brhacket/gboard-rgb-pulse/releases)

## Choose your level of motion
- **Touch:** the settings studio has four tabs — Ripple, Keys, Background, Keyboard. Keys holds twelve RGB-style key responses (rainbow ring, corner pulse, underline wave, diagonal wave, quad pulse, rainbow ripple, side waves, breathing glow, bloom wave, pulse cross, orbit glow, liquid rise); Background holds four wide shows (Wave sweep, Aura ripple, Color wash, Curtain wave) with their own speed control. The two groups are independent — each with its own color modes and hue pickers — and can run at the same time. Every effect is smooth flowing color in the classic RGB lighting style. Try the everyday preset for Breathing glow at 160 ms without traveling waves.
- **Colors:** a draggable hue spectrum bar, a saturation/value square in the color dialogs, and secondary hues that appear only when a two-color gradient mode needs them — separately for key and background colors.
- **Ripple:** optional row and full-keyboard patterns, adjustable borders and letter colors. Hiding key fills does not hide ripple borders.
- **Keyboard:** optional opening/closing lighting and a real keyboard test field.

## One button
The preview stays pinned while you edit. Presets change only your draft. There is exactly one apply button: **Save & restart Gboard** saves every setting and force-stops Gboard, so the keyboard always reopens with exactly what you saved. The force-stop needs root; if it is denied, opening the keyboard picks up the saved settings anyway. **Restart…** is no longer a separate step, and the old Apply button is gone. Effects are opt-in, and all-off restores stock styling.

Long sessions stay stable: the keyboard re-syncs settings every 30 s while open, failed syncs are retried, and confirmed settings are never blanked by a slow module reply.

## Install
Requires **Android 13+** and **LSPosed/Vector** with Gboard in scope. Keep a backup keyboard enabled.

1. Uninstall the previous module first. This release uses a new build-generated signing key, so prior test/original installs cannot be updated in place. Uninstalling loses module settings.
2. Install `Gboard-RGB-Pulse-1.0.3.apk` from the release page.
3. Enable the module and scope `com.google.android.inputmethod.latin`, then reboot once.
4. Choose your options, open the actual test keyboard, and press **Save & restart Gboard**. Tap any text field to reopen Gboard with the new settings.

If the status says **no live module reply**, check scope and reboot; do not assume settings were applied. Closing light can be cut short by Android's dismissal timing.

## Verification
CI runs Java policy/source checks, desktop Skia shader checks for the pulse field, Android compilation, APK archive checks and signature verification. The release includes a checksum and signer report.

**Device behavior remains unverified.** See [release notes](docs/RELEASE-1.0.3.md) and the [source audit](docs/SOURCE-AUDIT.md) for limitations.

## Development
```bash
cd RGBPulse
bash test.sh
bash build.sh ../Gboard-RGB-Pulse-1.0.3.apk
```
The build uses JDK 11+, Android API 34, R8/D8, `aapt`, `curl`, `openssl`, `zip`, and `apksigner` or `libapksig-java`. Shader Java embedding is generated from `shaders/field.agsl`.

The `Release` workflow builds and publishes from `main`. The current CI signing identity is generated per build; it is not an update-compatible persistent signing identity. Private signing material must never be committed.
