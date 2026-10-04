## Gboard RGB Pulse 1.1.0

Long-session stability, a visible quiet glow, one-button saving and a curated
catalog of layered, luminous tap animations.

### Highlights
- **Ten curated tap animations** (Touch page): each pulse is built from
  layered light — a hot near-white core, a saturated mid glow, a wide soft
  halo and a prismatic fringe that shifts hue across the feature — with eased
  motion (comet tails, spring-snapping brackets, spectral swipes, spark
  bursts with twinkle, chromatic rings). Two optional wide sweeps cross the
  whole keyboard. Fully opt-in, with strength, length, size and thickness
  controls.
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
Publication requires automated Java/source tests, desktop Skia shader tests
for the pulse field, Android compilation, archive checks, and APK signature
verification. `SHA256SUMS.txt` and `APK-verification.txt` accompany the APK.

Rendering and force-stop behavior remain device-unverified; Android may cut
closing light short. Publishing this version does not imply those device
checks have passed.
