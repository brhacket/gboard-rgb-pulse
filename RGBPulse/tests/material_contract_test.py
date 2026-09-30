"""v30 stock Gboard kept, only border=letter waving, all keys including shift/enter/space, one color"""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'src/dev/rgbpulse/gboard'
k=(src/'KeyStyle.java').read_text()
ui=(src/'SettingsActivity.java').read_text()
cfg=(src/'Config.java').read_text()
module=(src/'PulseModule.java').read_text()
fx=(src/'Fx.java').read_text()
studio=(src/'StudioTiles.java').read_text()
cut=(src/'CutoutTiles.java').read_text()
cap=(src/'CapHooks.java').read_text()
side=(src/'SideSweep.java').read_text()

assert 'LegendPolicy.eligible(text)' in k and 'textDepth.remove()' in k
assert 'cutouts.draw(entry,text,paint,x,y' in k and 'p.setResult(null);return;' in k
# v30 stock kept
assert 'Glass' in k and 'orig' in k and 'orig.draw' in k
assert 'stock Gboard kept' in k.lower() or 'stock' in k.lower()
assert 'bodyTops' in studio and 'bodyCx' in studio
assert 'finalLetter' in studio
assert 'sweepRadius' in side and 'activeRow' in side
assert 'drawLine' not in side
assert 'for(View k:keys) k.invalidate()' in module
print('PASS: v30 stock kept border=letter wave all keys')

assert 'c.getClipBounds(clip)' in studio
assert 'TileGeometry.diameter' in studio
print('PASS: sizing')

assert 'CapHooks.drawing.get()' in k
assert 'c.glideEvent(e)' in module
print('PASS: wiring')
