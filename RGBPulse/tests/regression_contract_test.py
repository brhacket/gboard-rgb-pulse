"""Source guardrails, not a replacement for Android/device tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'src/dev/rgbpulse/gboard'
ui=(src/'SettingsActivity.java').read_text()
side=(src/'SideSweep.java').read_text()
cfg=(src/'Config.java').read_text()
key=(src/'KeyStyle.java').read_text()
assert 'w.rowTop!=0' not in side and 'w.originX!=0' not in side
assert 'WavePolicy.glow' in side and 'boolean body=top!=null && cx!=null' in side
assert 'SideSweep.glowAt' in ui and 'fx.side.active(now,cfg.duration)' in ui
assert 'lastAuto' not in ui and 'postDelayed(tick' not in ui
assert 'ProcessBuilder' not in ui  # root work is isolated from the UI thread
assert 'Save & restart' in ui and 'Save only' in ui and 'persist(true)' in ui
assert 'ok&&restart&&GboardRestart.stop()' in ui
assert 'Browse animations' not in ui and 'Import TTF' not in ui
assert 'Enable ripple' in ui and 'Setup & troubleshooting' in ui
assert ui.count('slider(settings,')==2
assert 'copySettings(snapshot,applied).commit()' in ui
assert 'getSharedPreferences("settings_draft",Context.MODE_PRIVATE)' in ui
assert 'Apply settings / restart Gboard?' in ui and 'Discard draft changes?' in ui
assert 'enabled = false' in cfg and 'tapEffects = false' in cfg
assert 'ClassCastException' in cfg and 'value==null?fallback:value' in cfg
assert 'cfg.refined)return' in key and 'if(!c.refined){caps.apply' in key
assert 'implements Drawable.Callback' in key and 'onStateChange' in key
assert 'ripple.draw' in ui and 'ripple.draw' in key
paint=(src/'RipplePaint.java').read_text().split('void draw(',1)[1]
assert 'new Paint' not in paint and 'new RectF' not in paint
assert 'canvas.clipRect(bounds)' in paint
assert 'RGBPulse/signing/' in (root.parent/'.gitignore').read_text()
print('PASS: simple UI, explicit Apply, stock text, drawable state, shared clipped renderer, safe preferences')

assert 'Test your keyboard' in ui and 'EditText test=new EditText(this)' in ui
assert 'test.setSaveEnabled(false)' in ui
assert 'fx.drawFields(canvas,play,now)' in ui and 'fx.tap(rect.centerX(),rect.centerY(),play,now)' in ui
assert 'Enable background animations' in ui and '.setSingleChoiceItems(Config.EFFECTS' in ui
assert 'putBoolean("refined37",true).putBoolean("tapEffects36",false)' not in ui
module=(src/'PulseModule.java').read_text()
assert 'now-lastDraw>=16' not in module
frame=module.split('@Override public void run() {',1)[1].split('void render(',1)[0]
assert frame.index('fx.side.advance(config,play,now)') < frame.index('body.invalidate()')
assert 'for(View k:keys) k.invalidate();' in frame and 'root.postOnAnimation(this)' in module
assert 'c.sideStyle=1;c.tapEffects=false' not in cfg
print('PASS: real keyboard input restored, optional background pipeline, refresh-rate redraw and pre-invalidation advancement')
assert 'safeScan(layoutDirty||body==null||!body.isShown())' in module
assert 'onGlobalLayout(){layoutDirty=true;}' in module
assert 'removeOnGlobalLayoutListener(this)' in module
assert 'keyStyle.refreshRipples()' in module
assert 'key.getOverlay().add(layer)' in key and 'private void clearOverlays()' in key
fx=(src/'Fx.java').read_text()
assert 'cfg.enabled&&cfg.tapEffects&&fields!=null' in fx

restart=(src/'GboardRestart.java').read_text()
assert 'waitFor(25,TimeUnit.SECONDS)' in restart and 'destroyForcibly()' in restart
assert 'am force-stop --user current com.google.android.inputmethod.latin' in restart
assert 'Open app settings' in ui and 'if(!ok)' in ui
refined=key.split('private void applyRefined(',1)[1].split('private void clearOverlays',1)[0]
assert 'setBackground(' not in refined and '.getOverlay().remove' in refined
print('PASS: confirmed save-before-restart, bounded root process, refined overlay lifecycle')
