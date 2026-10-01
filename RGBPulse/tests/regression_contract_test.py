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
assert 'ProcessBuilder' not in ui and 'restartGboard' not in ui
assert 'Browse animations' not in ui and 'Import TTF' not in ui
assert 'Enable ripple' in ui and 'Setup & troubleshooting' in ui
assert ui.count('slider(settings,')==2
assert 'copySettings(snapshot,applied).commit()' in ui
assert 'getSharedPreferences("settings_draft",Context.MODE_PRIVATE)' in ui
assert 'Apply refined ripple?' in ui and 'Discard draft changes?' in ui
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
