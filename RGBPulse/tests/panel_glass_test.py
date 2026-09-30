"""Same full-panel shader input as Android compositor; synthetic background, not device proof."""
import skia,struct,numpy as np
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
R=Path(__file__).resolve().parents[1]
e=skia.RuntimeEffect.MakeForShader((R/'shaders/panelglass.agsl').read_text())
W,H=780,366;dp=2
boxes=[];gap=10;kw=(W-gap*11)/10;rh=(H-gap*5)/4
for ri,row in enumerate(['qwertyuiop','asdfghjkl','zxcvbnm']):
 x=(W-(len(row)*kw+(len(row)-1)*gap))/2;y=gap+ri*(rh+gap)
 for ch in row:boxes.append((x+4,y+6,x+kw-4,y+rh-6,ch));x+=kw+gap
boxes.extend([(187,287,593,350,'space'),(642,287,761,350,'enter')])
field=skia.RuntimeEffect.MakeForShader((R/'shaders/field.agsl').read_text())
def data(v):return skia.Data.MakeWithCopy(struct.pack('='+str(len(v))+'f',*v))
def source(pattern=False,age=.35):
 surf=skia.Surface(W,H);c=surf.getCanvas();c.clear(skia.ColorSetRGB(18,22,30))
 if pattern:
  for x in range(0,W,18):c.drawLine(x,0,x,H,skia.Paint(Color=0xff768d9e,StrokeWidth=2))
  for y in range(0,H,18):c.drawLine(0,y,W,y,skia.Paint(Color=0xff768d9e,StrokeWidth=2))
 else:
  c.drawPaint(skia.Paint(Shader=skia.GradientShader.MakeLinear([(0,0),(W,H)],[0xff152932,0xff191829,0xff111922])))
 if age>=0:
  vals=[W,H,age*2,.9,1.2,1,1,3,290/360,190/360,1]+[340,190,age,.7]+[0,0,-1,0]*3
  c.drawPaint(skia.Paint(Shader=field.makeShader(data(vals))))
 return surf.makeImageSnapshot()
def blur_image(image,frost):
 if frost<=0:return image
 # AOSP HWUI RenderEffect uses radius -> sigma conversion (Blur.cpp).
 radius=dp*(.6+11*frost);sigma=.57735*radius+.5
 info=skia.ImageInfo.Make(W,H,skia.ColorType.kRGBA_F32_ColorType,skia.AlphaType.kPremul_AlphaType)
 surf=skia.Surface.MakeRaster(info)
 surf.getCanvas().drawImage(image,0,0,skia.SamplingOptions(),skia.Paint(ImageFilter=skia.ImageFilters.Blur(sigma,sigma,skia.TileMode.kClamp)))
 return surf.makeImageSnapshot()
def array(image):
 return image.toarray(colorType=skia.ColorType.kRGBA_F32_ColorType,alphaType=skia.AlphaType.kPremul_AlphaType)
def render(strength=1.25,pattern=False,age=.35,keys=boxes,texture=.08,frost=.35,cut=.9,image=None):
 image=source(pattern,age) if image is None else image
 result=np.zeros((H,W,4),dtype=np.float32)
 for branch in [0,1] if frost>0 else [0]:
  child=(blur_image(image,frost) if branch else image).makeShader(skia.TileMode.kClamp,skia.TileMode.kClamp)
  values=[W,H,dp,strength,texture,frost,cut,.7,branch,len(keys)]+sum([list(b[:4]) for b in keys],[])+[0]*((64-len(keys))*4)
  sh=e.makeShader(data(values),child,1)
  info=skia.ImageInfo.Make(W,H,skia.ColorType.kRGBA_F32_ColorType,skia.AlphaType.kPremul_AlphaType)
  surf=skia.Surface.MakeRaster(info);surf.getCanvas().drawPaint(skia.Paint(Shader=sh))
  result+=array(surf.makeImageSnapshot())
 return result,image
checks=0
for keyset in [[],boxes[:1],boxes,boxes+boxes+boxes[:8]]:
 for strength in [0,1.25,2]:
  for texture,frost,cut in [(t,f,c) for t in [0,1] for f in [0,.35,1] for c in [0,.6,1]]:
   ar,base=render(strength,True,keys=keyset,texture=texture,frost=frost,cut=cut)
   assert np.isfinite(ar).all();assert ar.min()>=-.001 and ar.max()<=1.001
   assert (ar[:,:,:3]<=ar[:,:,3:4]+.001).all()
   assert np.max(np.abs(ar[0,:,:3]-base.toarray(colorType=skia.ColorType.kRGBA_F32_ColorType,alphaType=skia.AlphaType.kPremul_AlphaType)[0,:,:3]))<.001
   checks+=1
# A visible lens difference on every key, including action key and space, even with no tap.
a,_=render(0,True,age=-1,frost=0);b,_=render(2,True,age=-1,frost=0)
for box in boxes:
 l,t,r,bot=map(int,box[:4]);delta=np.abs(a[t:bot,l:r,:3]-b[t:bot,l:r,:3]).mean()
 assert delta>.009,(box[-1],delta)
print('PASS:',checks,'full-panel F32 renders; lens response on',len(boxes),'keys including Enter/space without active taps; finite/premultiplied and panel-edge identity')
# Frost must soften detail inside tiles without affecting gaps or panel edges.
clear,base=render(0,True,age=-1,frost=0)
soft,_=render(0,True,age=-1,frost=.35)
strong,_=render(0,True,age=-1,frost=1)
mask=np.zeros((H,W),bool)
for l,t,r,bottom,ch in boxes:
 mask[int(t)+12:int(bottom)-12,int(l)+12:int(r)-12]=True
for image in [soft,strong]:
 before=np.abs(clear[:,1:,:3]-clear[:,:-1,:3])[mask[:,1:] & mask[:,:-1]].mean()
 after=np.abs(image[:,1:,:3]-image[:,:-1,:3])[mask[:,1:] & mask[:,:-1]].mean()
 assert after<before*.98,(before,after)
 assert image[:,:,:3][mask].std()<clear[:,:,:3][mask].std()*.9
 assert np.abs(image-clear)[mask].mean()>.003
 assert np.array_equal(image[:8],clear[:8])
# Transparent and partially transparent sources; PLUS must not double the base.
for alpha in [0,80,180]:
 surf=skia.Surface(W,H);surf.getCanvas().clear(skia.ColorSetARGB(alpha,120,80,180))
 image=surf.makeImageSnapshot()
 for frost in [0,.35,1]:
  ar,_=render(image=image,frost=frost)
  assert np.isfinite(ar).all() and ar.min()>=-.001 and ar.max()<=1.001
  assert (ar[:,:,:3]<=ar[:,:,3:4]+.001).all()
  assert np.max(np.abs(ar[:8]-array(image)[:8]))<.001
print('PASS: 9 transparent/partial source blend tests; no duplicate backdrop or premultiplication violation')
# Rounded corners differ across the silhouette, leaving the panel unchanged.
square,_=render(0,False,age=-1,cut=0)
cut,_=render(0,False,age=-1,cut=1)
assert np.abs(square-cut).mean()>.0001
# Blur input is one smooth lobe, not spaced copies of an impulse.
surf=skia.Surface(W,H);surf.getCanvas().clear(0)
surf.getCanvas().drawRect(skia.Rect.MakeXYWH(W//2,0,1,H),skia.Paint(Color=skia.ColorWHITE))
line=array(blur_image(surf.makeImageSnapshot(),.35))[H//2,:,3]
peak=int(np.argmax(line));assert np.all(np.diff(line[peak:peak+25])<=.00001)
assert np.all(np.diff(line[peak-25:peak+1])>=-.00001)
print('PASS: adjustable rounded silhouette and Gaussian impulse without secondary ghost edges')
print('PASS: light/full frost softens backdrop detail inside tiles; exterior strip unchanged')
# Side-by-side actual shader test proof, not an aesthetic mockup.
folder=R/'tests/rendered';folder.mkdir(exist_ok=True)
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
f=instantiateVariableFont(TTFont(R/'assets/fonts/manrope.ttf'),{'wght':550},inplace=False);f.save(folder/'manrope-550.ttf')
font=ImageFont.truetype(str(folder/'manrope-550.ttf'),31)
def labelled(ar):
 im=Image.fromarray((ar[:,:,:3]*255).clip(0,255).astype('uint8'));d=ImageDraw.Draw(im)
 for l,t,r,b,ch in boxes:
  if ch=='enter':
   x=(l+r)/2;y=(t+b)/2;d.line([(x+13,y-9),(x+13,y+6),(x-12,y+6)],fill='white',width=2);d.line([(x-12,y+6),(x-5,y),(x-12,y+6),(x-5,y+12)],fill='white',width=2)
  else:d.text(((l+r)/2,(t+b)/2),ch,font=font,anchor='mm',fill=(243,239,247))
 return im
labelled(render()[0]).save(R.parent/'RGB-Pulse-16-Preview.png')
proof=Image.new('RGB',(W,H*2+80),(15,18,25));proof.paste(labelled(clear),(0,30));proof.paste(labelled(soft),(0,H+70));d=ImageDraw.Draw(proof);d.text((18,8),'DIFFUSION 0% / test grid / no tap',fill='white');d.text((18,H+48),'DIFFUSION 35% / same grid / no tap',fill='white');proof.save(R.parent/'RGB-Pulse-16-Diffusion-Test.png')
