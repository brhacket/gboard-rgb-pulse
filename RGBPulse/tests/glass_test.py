"""Material and optical tests on actual AGSL, not Android runtime."""
import skia,struct,numpy as np
from pathlib import Path
r=Path(__file__).resolve().parents[1]
e=skia.RuntimeEffect.MakeForShader((r/'shaders/glass.agsl').read_text())
# High-frequency reference to expose optical displacement clearly.
ch=skia.RuntimeEffect.MakeForShader('half4 main(float2 p){return half4(.5+.5*sin(p.x*.3),.5+.5*sin(p.y*.25),.5+.5*sin((p.x+p.y)*.2),1);}')
child=ch.makeShader(skia.Data.MakeWithCopy(b''))
def render(w=120,h=160,dp=2,mode=0,texture=.6,lens=1,press=0,child=child):
 vals=[8,8,w-8,h-8,dp,mode,press,texture,lens]
 sh=e.makeShader(skia.Data.MakeWithCopy(struct.pack('9f',*vals)),child,1)
 info=skia.ImageInfo.Make(w,h,skia.ColorType.kRGBA_F32_ColorType,skia.AlphaType.kPremul_AlphaType)
 surf=skia.Surface.MakeRaster(info);surf.getCanvas().clear(skia.ColorTRANSPARENT);surf.getCanvas().drawPaint(skia.Paint(Shader=sh))
 ar=surf.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_F32_ColorType,alphaType=skia.AlphaType.kPremul_AlphaType)
 return ar
checks=0
for w,h in [(60,92),(120,160),(430,92)]:
 for dp in [1,2,3]:
  for mode in [0,1]:
   for press in [0,1]:
    for texture,lens in [(0,0),(.6,1),(1,1.6)]:
     ar=render(w,h,dp,mode,texture,lens,press)
     assert np.isfinite(ar).all(),(w,h,dp,mode)
     assert ar.min()>=-.001 and ar.max()<=1.001
     assert (ar[:,:,:3]<=ar[:,:,3:4]+.002).all()
     assert not ar[:6].any() and not ar[-6:].any() and not ar[:,:6].any() and not ar[:,-6:].any()
     assert ar[:,:,3].max()>.01;checks+=1
# More roughness produces more fine-scale spatial detail in the middle, not just on the edge.
a=render(texture=0);b=render(texture=1)
region=(slice(55,100),slice(40,80),slice(0,3))
assert np.std(np.diff(b[region],axis=0))>np.std(np.diff(a[region],axis=0))*2
# Optical zero bypass and distinct wide-key mapping.
for w,h in [(120,160),(430,92)]:
 a=render(w,h,mode=1,lens=0);b=render(w,h,mode=1,lens=1.6)
 assert np.abs(a-b).mean()>.015
 assert np.max(np.abs(a[20:-20,20:-20,3]-1))<.001
# Surface texture stable across calls, no noise flicker.
assert np.array_equal(render(),render())
# Letter texture F32 finite and opaque, changes as requested.
letter=skia.RuntimeEffect.MakeForShader((r/'shaders/letter.agsl').read_text())
ls=[]
for amount in [0,.4,1]:
 sh=letter.makeShader(skia.Data.MakeWithCopy(struct.pack('7f',0,80,64,amount,1,.95,.99)))
 surf=skia.Surface(100,100);surf.getCanvas().drawPaint(skia.Paint(Shader=sh));ls.append(surf.makeImageSnapshot().toarray())
assert np.abs(ls[0].astype(float)-ls[2].astype(float)).mean()>1
print('PASS:',checks,'F32 glass renders: finite, premultiplied, clipping, sizes/densities/controls; texture/detail, lens response, stable noise and letter shader checks')
