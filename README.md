# Gboard RGB Pulse · Pulse Studio

**Version 1.0.2** — optional key feedback, restrained background light and expressive ripples for Gboard.

[Download 1.0.2](https://github.com/brhacket/gboard-rgb-pulse/releases/tag/v1.0.2)

## Choose your level of motion
- **Touch:** quick feedback on the tapped key, plus an independently controlled quiet background glow. Try the everyday preset for Soft press at 160 ms without traveling waves.
- **Ripple:** optional row and full-keyboard patterns, adjustable borders and letter colors. Hiding key fills does not hide ripple borders.
- **Keyboard:** optional opening/closing lighting and a real keyboard test field.

The preview stays pinned while you edit. Presets change only your draft. Nothing is sent to Gboard until **Apply changes**; the status distinguishes a saved draft from Gboard-confirmed application. **Restart…** is separate and requires confirmation/root permission. Effects are opt-in, and all-off restores stock styling.

## Install
Requires **Android 13+** and **LSPosed/Vector** with Gboard in scope. Keep a backup keyboard enabled.

1. Uninstall the previous module first. This release uses a new build-generated signing key, so prior test/original installs cannot be updated in place. Uninstalling loses module settings.
2. Install `Gboard-RGB-Pulse-1.0.2.apk` from the release page.
3. Enable the module and scope `com.google.android.inputmethod.latin`, then reboot once.
4. Choose your options, open the actual test keyboard, and press **Apply changes**. Wait for Gboard confirmation.

If the status says **no live module reply**, check scope and reboot; do not assume settings were applied. Closing light can be cut short by Android's dismissal timing.

## Verification
CI runs Java policy/source checks, desktop Skia shader checks, Android compilation, APK archive checks and signature verification. The release includes a checksum and signer report.

**Device behavior remains unverified.** In particular, the earlier settings-delivery problem has not yet been confirmed resolved on the user's phone. See [release notes](docs/RELEASE-1.0.2.md) and the [source audit](docs/SOURCE-AUDIT.md) for limitations.

## Development
```bash
cd RGBPulse
bash test.sh
bash build.sh ../Gboard-RGB-Pulse-1.0.2.apk
```
The build uses JDK 11+, Android API 34, R8/D8, `aapt`, `curl`, `openssl`, `zip`, and `apksigner` or `libapksig-java`. Shader Java embedding is generated from `shaders/field.agsl`.

The `Release` workflow builds and publishes from `main`. The current CI signing identity is generated per build; it is not an update-compatible persistent signing identity. Private signing material must never be committed.
