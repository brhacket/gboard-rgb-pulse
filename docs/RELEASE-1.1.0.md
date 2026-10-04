## Gboard RGB Pulse 1.1.0

Long-session stability, a visible quiet glow, one-button saving and a new
magnetic fluid that plays with your whole keyboard.

### Highlights
- **Magnetic fluid** (Touch page): liquid metal that pools between the keys. It
  chases your fingertips (multi-touch), splashes on every tap, and leans when
  you tilt the phone using the gravity sensor. Idle breathing keeps it alive
  between interactions. Rendered as a glossy pseudo-3D surface: dome lighting,
  fresnel rim, sharp specular, a moving sheen and velocity drag-streaks, with
  no banding artifacts. Adjustable intensity; fully opt-in.
- **One button replaces Apply/Restart**: *Save & restart Gboard* saves every
  setting and force-stops Gboard in a single tap, so the keyboard always
  reopens with exactly what you saved. The old Apply changes button is gone.
- **Effects no longer vanish during long sessions**: failed settings pulls are
  retried, confirmed settings are never blanked by a slow module app, and the
  keyboard re-syncs every 30 seconds while it is open.
- **Subtle background is now visible**: the quiet glow is larger (up to 72 dp),
  lasts 520 ms and reaches up to 50% intensity instead of an almost-invisible
  12%, with a gentle downward drift while it fades.

### Requirements and installation
Android 13+ with LSPosed/Vector; scope the module to Gboard
(`com.google.android.inputmethod.latin`). Keep a backup keyboard enabled.

**Signing:** This build uses a new build-generated signing key. It cannot
update prior installs in place. Uninstall the previous module first (module
settings are lost), install `Gboard-RGB-Pulse-1.1.0.apk`, enable the module
for Gboard, and reboot once to unload older hooks.

Choose your options, then press **Save & restart Gboard**. The force-stop
needs root; if it is denied, simply opening the keyboard picks up the saved
settings instead.

### Verification and limitations
Publication requires automated Java/source tests (including new fluid physics
tests), desktop Skia shader tests for both the pulse field and the fluid,
Android compilation, archive checks, and APK signature verification.
`SHA256SUMS.txt` and `APK-verification.txt` accompany the APK.

Tilt response uses the gravity sensor (accelerometer fallback); exact tilt
feel depends on the device. Rendering and force-stop behavior remain
device-unverified; Android may cut closing light short. Publishing this
version does not imply those device checks have passed.
