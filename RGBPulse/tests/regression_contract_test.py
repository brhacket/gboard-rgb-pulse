"""Source-level guards only; these do not replace Android/device testing."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'src/dev/rgbpulse/gboard'
ui = (src / 'SettingsActivity.java').read_text()
side = (src / 'SideSweep.java').read_text()
assert 'w.rowTop!=0' not in side and 'w.originX!=0' not in side
assert 'WavePolicy.glow' in side and 'boolean body=top!=null && cx!=null' in side
assert 'SideSweep.glowAt' in ui and 'fx.side.active(now,cfg.duration)' in ui
assert 'simulate wave' not in ui and 'sweepProg=0.5f' not in ui
assert 'if(pos!=previous)' in ui
assert 'Reset all settings?' in ui
assert 'Tile background color' not in ui and 'Apply retro circular tiles' not in ui
assert 'if(cfg.enabled && running && isShown())' in ui
legend = ui.split('void drawLegend(', 1)[1].split('@Override', 1)[0]
assert 'new Paint' not in legend and 'new RectF' not in legend
assert 'attempted=false;issue=null;' in (src / 'Fx.java').read_text()
assert 'RGBPulse/signing/' in (root.parent / '.gitignore').read_text()
print('PASS: preview lifecycle, coordinate-space selection, settings and signing guards')
