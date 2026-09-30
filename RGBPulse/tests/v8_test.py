"""Thickness and v7 visual parity checks, desktop only."""
import runpy, zipfile
import numpy as np
from pathlib import Path
from PIL import Image,ImageDraw
ns=runpy.run_path(str(Path(__file__).with_name('shader_test.py')))
render=ns['render'];skia=ns['skia'];root=ns['ROOT']
with zipfile.ZipFile(root.parent/'Gboard-RGB-Pulse-7.0-source.zip') as z:
    old=skia.RuntimeEffect.MakeForShader(z.read('RGBPulse/shaders/field.agsl').decode())
for style in range(11):
    for age in [.1,.35,.7]:
        a=render(style,age);b=render(style,age,fx=old)
        assert np.array_equal(a,b),(style,age)
    for width in [.25,.5,1,2,3]:
        ar=render(style,.35,thickness=width,padded=True)
        assert not ar[:6].any() and not ar[-6:].any() and not ar[:,:6].any() and not ar[:,-6:].any()
for style in range(1,11):
    a=render(style,.35,thickness=.25);b=render(style,.35,thickness=3)
    assert np.abs(a.astype(float)-b.astype(float)).mean()>1,style
assert np.array_equal(render(0,.35,thickness=.25),render(0,.35,thickness=3))
# Both paths must use centers, not incoming event coordinates.
src=root/'src/dev/rgbpulse/gboard'
s=(src/'PulseModule.java').read_text()
assert 'px=r.exactCenterX(); py=r.exactCenterY();' in s
assert 'if (!hit || !play.contains(px,py)) return;' in s
ui=(src/'SettingsActivity.java').read_text()
assert 'tapKey(e.getX(i),e.getY(i),lastAuto)' in ui
assert 'fx.tap(x+kw/2,y+rowH/2,play,now)' in ui
assert 'fx.tap(w*.5f,(top+h-gap)/2,play,now)' in ui
sheet=Image.new('RGB',(720,205*3),(22,23,31));d=ImageDraw.Draw(sheet)
for j,width in enumerate([.25,1,3]):
    for k,style in enumerate([1,2]):
        ar=render(style,.35,thickness=width)
        im=Image.alpha_composite(Image.new('RGBA',(360,171),(22,23,31,255)),Image.fromarray(ar)).convert('RGB')
        sheet.paste(im,(k*360,j*205));d.text((k*360+12,j*205+180),f'{ns["names"][style]} / thickness {int(width*100)}%',fill='white')
sheet.save(root.parent/'RGB-Pulse-8-Thickness.jpg')
print('PASS: 33 exact v7 frame comparisons; 55 thickness clip checks; all 10 styles respond; Hologram unchanged; key-center source contracts')
