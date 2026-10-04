## Gboard RGB Pulse 1.0.2

Publishes the latest Pulse Studio implementation on `main`.

### Highlights
- Sticky, collapsible preview and separate Ripple, Touch and Keyboard controls.
- Independent key feedback and quiet background lighting. The everyday preset stages Soft press / 160 ms plus a small, non-expanding glow, with traveling ripples and opening/closing effects off.
- Ten selectable tap effects, multi-row ripple patterns, configurable colors, and optional opening/closing lighting.
- Key fills and ripple borders are independent; all-off restores stock styling.
- Explicit Apply with signed settings snapshots and application receipts. Restart is a separate, confirmed root action with failure feedback.

### Requirements and installation
Android 13+ with LSPosed/Vector; scope the module to Gboard (`com.google.android.inputmethod.latin`). Keep a backup keyboard enabled.

**Signing:** This build uses a new build-generated signing key. It cannot update prior test/original installs in place. Uninstall the previous module first (module settings are lost), install `Gboard-RGB-Pulse-1.0.2.apk`, enable the module for Gboard, and reboot once to unload older hooks.

Choose your options, open the actual test keyboard, press **Apply changes**, and check for confirmation. **Restart…** requires confirmation and root permission; Apply alone does not force-stop Gboard.

### Verification and limitations
Publication requires automated Java/source tests, desktop shader tests, Android compilation, archive checks, and APK signature verification. `SHA256SUMS.txt` and `APK-verification.txt` accompany the APK.

Actual Gboard/LSPosed delivery, rendering, and force-stop behavior remain device-unverified; the earlier settings-delivery issue has not yet been confirmed resolved on the user's phone. Android may cut closing light short. Publishing this version does not imply those device checks have passed.
