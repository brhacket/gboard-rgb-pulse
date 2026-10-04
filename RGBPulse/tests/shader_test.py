"""Desktop Skia checks of production shader: not Android/Gboard device verification."""
import struct,sys
from pathlib import Path
import numpy as np
import skia
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
src=(ROOT/'shaders/field.agsl').read_text()
effect=skia.RuntimeEffect.MakeForShader(src)
W,H=360,180
names=['rainbow-ring','corner-pulse','underline-wave','diagonal-wave','quad-pulse','rainbow-ripple','side-waves','breathing-glow','bloom-wave','pulse-cross','orbit-glow','liquid-rise','wave-sweep','aura-ripple','color-wash','curtain-wave']
def render(style,age,mode=2,origins=None,boxes=None,strength=.9,padded=False,special=None):
    if origins is None:origins=[(180,90,age,.7)]
    if boxes is None:boxes=[(161,67,199,113)]*len(origins)
    taps=sum((list(t) for t in origins),[])+[0,0,-1,0]*(4-len(origins))
    bounds=sum((list(b) for b in boxes),[])+[0,0,1,1]*(4-len(boxes))
    values=[W,H,.4,strength,1,1]+([] if special else [style])+[mode,.75,.5,.5]+taps+bounds
    shader=(special or effect).makeShader(skia.Data.MakeWithCopy(struct.pack('='+str(len(values))+'f',*values)))
    pad=6 if padded else 0
    surface=skia.Surface(W+2*pad,H+2*pad);canvas=surface.getCanvas();canvas.clear(skia.ColorTRANSPARENT)
    canvas.translate(pad,pad);canvas.clipRect(skia.Rect.MakeWH(W,H));canvas.drawPaint(skia.Paint(Shader=shader))
    return surface.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
for style in range(16):
    compiled=skia.RuntimeEffect.MakeForShader(src.replace('uniform float style;',f'const float style={style}.0;'))
    for mode in range(5):
        for age in [-1,0,.03,.2,.5,.85,1]:
            a=render(style,age,mode,padded=True)
            assert not a[:6].any() and not a[-6:].any() and not a[:,:6].any() and not a[:,-6:].any()
            if age<=0 or age>=1:assert not a[:,:,3].any()
            elif .1<age<.8:assert a[:,:,3].max()>8,(style,age)
            assert a[:,:,3].max()<=int((.48 if style<12 else .40)*.9*255)+1
    assert not render(style,.3,strength=0).any()
    assert np.abs(render(style,.2)[:,:,3].astype(float)-render(style,.6)[:,:,3].astype(float)).sum()>30
    assert np.abs(render(style,.3).astype(float)-render(style,.3,special=compiled).astype(float)).max()<=1
    for x,y in [(2,2),(W-2,2),(2,H-2),(W-2,H-2)]:
        a=render(style,.3,origins=[(x,y,.3,.7)],boxes=[(x-19,y-23,x+19,y+23)],padded=True,special=compiled)
        assert not a[:6].any() and not a[-6:].any() and not a[:,:6].any() and not a[:,-6:].any()
    # Four same-position touches must not multiply brightness beyond the total cap.
    a=render(style,.25,origins=[(180,90,.25,.7)]*4)
    assert a[:,:,3].max()<=int((.48 if style<12 else .40)*.9*255)+1
    if style<12:
        yy,xx=np.mgrid[0:H,0:W]
        outside=(np.abs(xx+.5-180)>19*1.2)|(np.abs(yy+.5-90)>23*1.2)
        for age in [.1,.3,.6,.9]:assert not render(style,age)[:,:,3][outside].any(),style
        assert np.count_nonzero(a[:,:,3])/(W*H)<.045
    else:
        assert np.count_nonzero(render(style,.3)[:,:,3])/(W*H)>.045
# Distinct alpha geometry, not merely recolored copies, compared within key neighborhood.
for a in range(12):
    for b in range(a+1,8):
        delta=np.mean([np.abs(render(a,t)[60:120,150:210,3].astype(float)-render(b,t)[60:120,150:210,3].astype(float)).mean() for t in [.2,.4,.6]])
        assert delta>.5,(a,b,delta)
print('PASS: sixteen specialized pulse programs, lifecycle/clipping, twelve bounded local footprints, rapid-typing alpha caps and distinct local geometry')
folder=ROOT/'tests/rendered';folder.mkdir(exist_ok=True)
for style,name in enumerate(names):
    ages=[.15,.3,.6] if '--stills' in sys.argv else [(i+1)/25 for i in range(24)]
    frames=[]
    for age in ages:
        image=Image.alpha_composite(Image.new('RGBA',(W,H),(12,20,25,255)),Image.fromarray(render(style,age)))
        draw=ImageDraw.Draw(image)
        for y in [42,90,138]:
            for x in range(28,350,38):draw.text((x,y),'a',anchor='mm',fill=(215,220,224),font_size=15)
        frames.append(image.convert('RGB'))
    frames[1].save(folder/(name+'.png'))
    if '--stills' not in sys.argv:frames[0].save(folder/(name+'.gif'),save_all=True,append_images=frames[1:],duration=12,loop=0)
