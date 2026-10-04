# Gboard RGB Pulse · Pulse Studio

**Version 1.1.0** — optional key feedback, a visible quiet glow, expressive ripples and a magnetic fluid for Gboard.

[Download 1.1.0](https://github.com/brhacket/gboard-rgb-pulse/releases/tag/v1.1.0)

## Choose your level of motion
- **Touch:** quick feedback on the tapped key, plus an independently controlled quiet background glow that is now clearly visible and sinks gently as it fades. Try the everyday preset for Soft press at 160 ms without traveling waves.
- **Magnetic fluid:** one living bubble of liquid metal between your keys. Touch the keyboard and it darts straight to your finger; several fingers split it into equal parts that flow back together when you let go. It leans with device tilt, stretches while it moves and flashes as it splits. Opt-in, with an intensity slider.
- **Ripple:** optional row and full-keyboard patterns, adjustable borders and letter colors. Hiding key fills does not hide ripple borders.
- **Keyboard:** optional opening/closing lighting and a real keyboard test field.

## One button
The preview stays pinned while you edit. Presets change only your draft. There is exactly one apply button: **Save & restart Gboard** saves every setting and force-stops Gboard, so the keyboard always reopens with exactly what you saved. The force-stop needs root; if it is denied, opening the keyboard picks up the saved settings anyway. **Restart…** is no longer a separate step, and the old Apply button is gone. Effects are opt-in, and all-off restores stock styling.

Long sessions stay stable: the keyboard re-syncs settings every 30 s while open, failed syncs are retried, and confirmed settings are never blanked by a slow module reply.

## Install
Requires **Android 13+** and **LSPosed/Vector** with Gboard in scope. Keep a backup keyboard enabled.

1. Uninstall the previous module first. This release uses a new build-generated signing key, so prior test/original installs cannot be updated in place. Uninstalling loses module settings.
2. Install `Gboard-RGB-Pulse-1.1.0.apk` from the release page.
3. Enable the module and scope `com.google.android.inputmethod.latin`, then reboot once.
4. Choose your options, open the actual test keyboard, and press **Save & restart Gboard**. Tap any text field to reopen Gboard with the new settings.

If the status says **no live module reply**, check scope and reboot; do not assume settings were applied. Closing light can be cut short by Android's dismissal timing.

## Verification
CI runs Java policy/source checks (including the fluid physics suite), desktop Skia shader checks for the pulse field and the magnetic fluid, Android compilation, APK archive checks and signature verification. The release includes a checksum and signer report.

**Device behavior remains unverified.** Tilt response depends on the device's gravity sensor (accelerometer fallback). See [release notes](docs/RELEASE-1.1.0.md) and the [source audit](docs/SOURCE-AUDIT.md) for limitations.

## Development
```bash
cd RGBPulse
bash test.sh
bash build.sh ../Gboard-RGB-Pulse-1.1.0.apk
```
The build uses JDK 11+, Android API 34, R8/D8, `aapt`, `curl`, `openssl`, `zip`, and `apksigner` or `libapksig-java`. Shader Java embedding is generated from `shaders/field.agsl` and `shaders/fluid.agsl`.

The `Release` workflow builds and publishes from `main`. The current CI signing identity is generated per build; it is not an update-compatible persistent signing identity. Private signing material must never be committed.
