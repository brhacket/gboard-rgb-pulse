"""Desktop path-op/field rendering, not Android hook execution."""
from pathlib import Path
import skia,struct,numpy as np
from PIL import Image
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
R=Path(__file__).resolve().parents[1];W,H=780,366
folder=R/'tests/rendered';folder.mkdir(exist_ok=True)
f=instantiateVariableFont(TTFont(R/'assets/fonts/manrope.ttf'),{'wght':550},inplace=False);f.save(folder/'manrope.ttf')
face=skia.Typeface.MakeFromFile(str(folder/'manrope.ttf'));font=skia.Font(face,32)
boxes=[];gap=10;kw=(W-gap*11)/10;rh=(H-gap*5)/4
for row,letters in enumerate(['qwertyuiop','asdfghjkl','zxcvbnm']):
 x=(W-(len(letters)*kw+(len(letters)-1)*gap))/2;y=gap+row*(rh+gap)
 for ch in letters:boxes.append((x,y,x+kw,y+rh,ch));x+=kw+gap
boxes.append((187,287,593,356,'space'))
shapes=[];holes=[]
for l,t,r,b,ch in boxes:
 cx=(l+r)/2;cy=(t+b)/2
 fm=font.getMetrics();baseline=cy-(fm.fAscent+fm.fDescent)/2
 ids=font.textToGlyphs(ch);paths=font.getPaths(ids);left=cx-font.measureText(ch)/2
 hole=skia.Path()
 for path,x in zip(paths,font.getXPos(ids,left)):
  if path is not None:path.offset(x,baseline);hole.addPath(path)
 tile=skia.Rect.MakeLTRB(l+6,t+8,r-6,b-8)
 if len(ch)==1:
  side=min(tile.width(),tile.height());tile=skia.Rect.MakeLTRB(cx-side/2,cy-side/2,cx+side/2,cy+side/2)
 silhouette=skia.Path();silhouette.addRoundRect(tile,tile.height()/2,tile.height()/2)
 assert skia.Op(hole,silhouette,skia.PathOp.kDifference_PathOp).isEmpty(),ch
 shape=skia.Op(silhouette,hole,skia.PathOp.kDifference_PathOp);shapes.append(shape);holes.append(hole)
# Full-resolution alpha masks. Interior hole pixels must remain exactly the underlying source.
def mask(paths):
 s=skia.Surface(W,H);s.getCanvas().clear(0)
 for path in paths:s.getCanvas().drawPath(path,skia.Paint(Color=skia.ColorWHITE,AntiAlias=True))
 return s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)[:,:,3]
def interior(m):
 m=m==255
 out=m.copy()
 for dy in [-1,0,1]:
  for dx in [-1,0,1]:out &= np.roll(np.roll(m,dy,axis=0),dx,axis=1)
 return out
holemask=(mask(holes)>128)&(mask(shapes)==0);whitemask=interior(mask(shapes))
assert holemask.sum()>1000 and whitemask.sum()>10000,(holemask.sum(),whitemask.sum())
field=skia.RuntimeEffect.MakeForShader((R/'shaders/field.agsl').read_text())
def render(age):
 s=skia.Surface(W,H);c=s.getCanvas();c.clear(0xff121219)
 if age>=0:
  v=[W,H,age*2,.9,1.2,1,1,3,290/360,190/360,1]+[180,155,age,.7]+[0,0,-1,0]*3
  c.drawPaint(skia.Paint(Shader=field.makeShader(skia.Data.MakeWithCopy(struct.pack('='+str(len(v))+'f',*v)))))
 base=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
 for shape in shapes:c.drawPath(shape,skia.Paint(Color=skia.ColorWHITE,AntiAlias=True))
 out=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
 assert np.array_equal(out[holemask],base[holemask]);assert (out[whitemask]==255).all()
 return Image.fromarray(out),out
idle,a=render(-1);frames=[];change=0
for age in np.linspace(.05,.9,16):
 im,b=render(float(age));frames.append(im);change=max(change,float(np.abs(a[holemask,:3].astype(float)-b[holemask,:3]).mean()))
assert change>1,change
frames[6].save(R.parent/'RGB-Pulse-17-Preview.png')
frames[0].save(R.parent/'RGB-Pulse-17-Cutouts.gif',save_all=True,append_images=frames[1:]+[idle]*3,duration=85,loop=0)
print('PASS: 27 glyphs fit their tile paths; 17 frame checks prove exact backdrop pixels in holes and pure white tile interiors; RGB changes inside holes without a letter animation')
