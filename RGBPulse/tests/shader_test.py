"""Desktop Skia regression tests; these are NOT Android/Gboard runtime tests.
Optional deps: skia-python, numpy, pillow. Shader source used without conversion.
"""
import skia,struct,numpy as np,sys,zipfile
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
src=(ROOT/'shaders/field.agsl').read_text()
effect=skia.RuntimeEffect.MakeForShader(src)
names=['hologram','neon','glass','aurora','metaballs','bloom','prism','sonar','silk','plasma','orbit','shuffle-reserved','material-bloom','diffused-ring','tonal-orbit','lateral-sweep','soft-spotlight','crossing-ribbons','rising-curtain']
styles=list(range(11))+list(range(12,19))
W,H=360,171

def render(style,age,mode=3,origins=None,opacity=.9,phase=.4,size=1.1,w=W,h=H,padded=False,fx=None,special=False,thickness=1.0):
    if origins is None:origins=[(w*.5,h*.48,age,.7)]
    taps=sum((list(t) for t in origins),[])+[0,0,-1,0]*(4-len(origins))
    vals=[w,h,phase,opacity,size]+([thickness] if fx is None or special else [])+([] if special else [style])+[mode,185/360,280/360,.9]+taps
    shader=(fx or effect).makeShader(skia.Data.MakeWithCopy(struct.pack('='+str(len(vals))+'f',*vals)))
    border=6 if padded else 0
    surface=skia.Surface(w+border*2,h+border*2);c=surface.getCanvas();c.clear(skia.ColorTRANSPARENT)
    c.translate(border,border);c.clipRect(skia.Rect.MakeWH(w,h))
    p=skia.Paint();p.setShader(shader);c.drawPaint(p)
    return surface.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)

checks=0
for style in styles:
    for mode in range(5):
        for age in [-1,0,.03,.2,.5,.85,1]:
            ar=render(style,age,mode,padded=True)
            assert not ar[:6].any() and not ar[-6:].any() and not ar[:,:6].any() and not ar[:,-6:].any();checks+=1
            if age<=0 or age>=1:assert not ar[:,:,3].any();checks+=1
            elif .1<age<.8:assert ar[:,:,3].max()>10;checks+=1
    assert not render(style,.2,opacity=0).any();checks+=1
    ar=render(style,.2);br=render(style,.5,phase=.9)
    assert np.abs(ar.astype(float)-br.astype(float)).mean()>.5;checks+=1
    ar=render(style,.2,origins=[(20,20,.1,.2),(150,60,.3,.8),(320,160,.6,.6),(200,110,.8,.4)])
    assert ar[:,:,3].max()>40;checks+=1
print('PASS:',checks,'field/alpha/clip assertions')

for style in styles:
    special=skia.RuntimeEffect.MakeForShader(src.replace('uniform float style;',f'const float style={style}.0;'))
    ar=render(style,.35,fx=special,special=True)
    assert ar[:,:,3].max()>10
    for size in [.3,2.5]:
        for x,y in [(2,2),(W-2,2),(2,H-2),(W-2,H-2),(W/2,H/2)]:
            ar=render(style,.35,size=size,origins=[(x,y,.35,.7)],padded=True,fx=special,special=True)
            assert not ar[:6].any() and not ar[-6:].any() and not ar[:,:6].any() and not ar[:,-6:].any()
print('PASS: 18 specialized programs + 180 edge/size clipping assertions')

# Verify fronts actually travel outwards, not just pulse opacity at fixed coordinates.
w=h=256
y,x=np.mgrid[0:h,0:w];radius=np.sqrt((x+.5-w/2)**2+(y+.5-h/2)**2)
for style in range(1,11):
    measures=[]
    for age in [.16,.38,.64]:
        alpha=render(style,age,size=.4,w=w,h=h,origins=[(w/2,h/2,age,.7)])[:,:,3].astype(float)
        measures.append((alpha*radius).sum()/max(1,alpha.sum()))
    assert measures[1]>measures[0]+8 and measures[2]>measures[1]+8,(style,measures)
    print(names[style],'mean radii',*[round(v,1) for v in measures])
print('PASS: outward propagation for all 10 liquid styles (20 comparisons)')

# New motions must produce measurably different alpha geometry, not palette swaps.
for a in range(15,19):
    for b in range(a+1,19):
        differences=[]
        for age in [.2,.4,.6]:
            aa=render(a,age)[:,:,3].astype(float);bb=render(b,age)[:,:,3].astype(float)
            differences.append(np.abs(aa-bb).mean())
        assert np.mean(differences)>3,(a,b,differences)
print('PASS: new motion geometries differ across three animation phases')

# Optional parity check against supplied v5: exact favorite at the same settings.
prior=ROOT.parent/'Gboard-RGB-Pulse-5.0-source.zip'
if prior.exists():
    with zipfile.ZipFile(prior) as z:oldsrc=z.read('RGBPulse/shaders/field.agsl').decode()
    old=skia.RuntimeEffect.MakeForShader(oldsrc)
    for age in [.08,.25,.5,.8]:
        for mode in range(5):
            a=render(0,age,mode,size=1.35)
            b=render(16,age,mode,size=1.35,fx=old)
            # toarray returns unpremultiplied RGB: near-zero-alpha rounding can inflate
            # RGB differences that do not alter the composed result. Compare premultiplied output.
            aa=a.astype(float);bb=b.astype(float)
            aa[:,:,:3]*=aa[:,:,3:4]/255;bb[:,:,:3]*=bb[:,:,3:4]/255
            diff=np.abs(aa-bb)
            assert diff.max()<=2 and diff.mean()<.02,(age,mode,diff.max(),diff.mean())
    print('PASS: Hologram tiles parity with v5 across 20 frames/modes (rounding tolerance)')

folder=ROOT/'tests/rendered';folder.mkdir(exist_ok=True)
def add_keys(bg):
    d=ImageDraw.Draw(bg)
    for ri,row in enumerate(['qwertyuiop','asdfghjkl','zxcvbnm']):
        kw=30;rh=33;gap=5;x=(W-(len(row)*kw+(len(row)-1)*gap))/2;y=5+ri*40
        for ch in row:
            d.rounded_rectangle((x,y,x+kw,y+rh),radius=6,fill=(45,47,59),outline=(57,59,70))
            d.text((x+kw/2,y+rh/2),ch,fill=(239,236,245),anchor='mm',font_size=13);x+=kw+gap
    d.rounded_rectangle((90,129,270,165),radius=6,fill=(45,47,59),outline=(57,59,70))
    d.text((180,147),'space',anchor='mm',fill=(239,236,245),font_size=13)
    return bg
for style in styles:
    name=names[style]
    ages=[.16,.35,.52] if '--stills' in sys.argv else [(i+1)/33 for i in range(32)]
    frames=[]
    for age in ages:
        ar=render(style,age,mode=0 if style in [0,3,7] else 3,phase=age*1.8)
        bg=Image.alpha_composite(Image.new('RGBA',(W,H),(22,23,31,255)),Image.fromarray(ar))
        frames.append(add_keys(bg).convert('RGB'))
    frames[len(frames)//3].save(folder/(name+'.png'))
    if '--stills' not in sys.argv:frames[0].save(folder/(name+'.gif'),save_all=True,append_images=frames[1:],duration=56,loop=0)
review=Image.new('RGB',(W*3,(H+24)*6),(14,15,22));d=ImageDraw.Draw(review)
for idx,style in enumerate(styles):
    name=names[style]
    x=(idx%3)*W;y=(idx//3)*(H+24)
    review.paste(Image.open(folder/(name+'.png')),(x,y));d.text((x+7,y+H+4),name,fill='white')
review.save(folder/'review.jpg')
