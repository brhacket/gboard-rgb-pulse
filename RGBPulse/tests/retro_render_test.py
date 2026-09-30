"""Mirrors CutoutTiles path composition; desktop tests, not Gboard runtime proof."""
from pathlib import Path
import skia,struct,numpy as np
from PIL import Image
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
R=Path(__file__).resolve().parents[1];W,H=780,366;dp=2
folder=R/'tests/rendered';folder.mkdir(exist_ok=True)
field=skia.RuntimeEffect.MakeForShader((R/'shaders/field.agsl').read_text())
boxes=[];gap=10;kw=(W-gap*11)/10;rh=(H-gap*5)/4
for row,letters in enumerate(['qwertyuiop','asdfghjkl','zxcvbnm']):
 x=(W-(len(letters)*kw+(len(letters)-1)*gap))/2;y=gap+row*(rh+gap)
 for ch in letters:boxes.append((x,y,x+kw,y+rh,ch));x+=kw+gap
boxes.append((187,287,593,356,'space'))
def make_font(name):
 f=instantiateVariableFont(TTFont(R/'assets/fonts'/f'{name}.ttf'),{'wght':550 if name=='manrope' else 500},inplace=False);f.save(folder/f'{name}.ttf')
 return skia.Font(skia.Typeface.MakeFromFile(str(folder/f'{name}.ttf')),32)
def tiles(font,percent=78,clipRatio=1):
 result=[]
 for l,t,r,b,ch in boxes:
  cx=(l+r)/2;cy=(t+b)/2
  fm=font.getMetrics();baseline=cy-(fm.fAscent+fm.fDescent)/2
  ids=font.textToGlyphs(ch);assert all(ids),ch
  hole=skia.Path()
  for path,x in zip(font.getPaths(ids),font.getXPos(ids,cx-font.measureText(ch)/2)):
   if path is not None:path.offset(x,baseline);hole.addPath(path)
  clipBottom=t+(b-t)*clipRatio
  side=max(0,min(r-l-4*dp,clipBottom-t-4*dp,kw*percent/100))
  tw=side if len(ch)==1 else min(r-l-4*dp,(r-l)*.86)
  hb=hole.computeTightBounds()
  cy=max(t+2*dp+side/2,min(clipBottom-2*dp-side/2,(t+clipBottom)/2))
  tile=skia.Rect.MakeLTRB(cx-tw/2,cy-side/2,cx+tw/2,cy+side/2)
  assert tile.top()>=t+2*dp-.001 and tile.bottom()<=clipBottom-2*dp+.001
  fit=min(1,tw*(.56 if len(ch)==1 else .84)/hb.width(),side*.49/hb.height())
  hole.offset(-hb.centerX(),-hb.centerY());hole.transform(skia.Matrix.Scale(fit,fit));hole.offset(tile.centerX(),tile.centerY())
  hint=str(('qwertyuiop'.index(ch)+1)%10) if ch in 'qwertyuiop' and len(ch)==1 else None
  if hint is not None:
   hb=hole.computeTightBounds();target=tile.makeInset(tw*.10,side*.10);target.offset(-tw*.04,side*.11)
   fit=min(1,target.width()*.56/hb.width(),target.height()*.49/hb.height())
   hole.offset(-hb.centerX(),-hb.centerY());hole.transform(skia.Matrix.Scale(fit,fit));hole.offset(target.centerX(),target.centerY())
   hintFont=skia.Font(skia.Typeface.MakeDefault(),side*.20);gid=hintFont.textToGlyphs(hint)
   hintHole=skia.Path();hf=hintFont.getMetrics();baseline=tile.top()+side*.25-(hf.fAscent+hf.fDescent)/2
   for path,x in zip(hintFont.getPaths(gid),hintFont.getXPos(gid,tile.centerX()+tw*.22-hintFont.measureText(hint)/2)):
    if path is not None:path.offset(x,baseline);hintHole.addPath(path)
   assert skia.Op(hintHole,hole,skia.PathOp.kIntersect_PathOp).isEmpty(),'hint collides with hollow letter'
  outline=skia.Path();outline.addRoundRect(tile,side/2,side/2)
  assert skia.Op(hole,outline,skia.PathOp.kDifference_PathOp).isEmpty(),ch
  if hint is not None:assert skia.Op(hintHole,outline,skia.PathOp.kDifference_PathOp).isEmpty(),'number outside circle'
  shape=skia.Op(outline,hole,skia.PathOp.kDifference_PathOp)
  result.append((tile,outline,shape,hole,hint if hint is not None else None))
 return result
def masks(data):
 s=skia.Surface(W,H);c=s.getCanvas();c.clear(0)
 for _,_,_,hole,_ in data:c.drawPath(hole,skia.Paint(Color=skia.ColorWHITE,AntiAlias=True))
 h=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)[:,:,3]
 c.clear(0)
 for _,_,shape,_,_ in data:c.drawPath(shape,skia.Paint(Color=skia.ColorWHITE,AntiAlias=True))
 faces=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)[:,:,3]
 return (h>128)&(faces==0)
def render(data,age,bg=0xff121219):
 s=skia.Surface(W,H);c=s.getCanvas();c.clear(bg)
 if age>=0:
  v=[W,H,age*2,.9,1.2,1,1,3,290/360,190/360,1]+[180,155,age,.7]+[0,0,-1,0]*3
  c.drawPaint(skia.Paint(Shader=field.makeShader(skia.Data.MakeWithCopy(struct.pack('='+str(len(v))+'f',*v)))))
 base=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
 for tile,outline,shape,hole,hint in data:
  for i in [2,1]:
   shadow=skia.Path(outline);shadow.offset(0,i*.55*dp);shadow=skia.Op(shadow,outline,skia.PathOp.kDifference_PathOp)
   c.drawPath(shadow,skia.Paint(Color=skia.ColorSetARGB(round(255*(.07 if i==2 else .12)),0,0,0),AntiAlias=True))
  c.drawPath(shape,skia.Paint(Color=0xfff5f5f3,AntiAlias=True))
  inset=tile.makeInset(.3*dp,.3*dp)
  c.drawRoundRect(inset,inset.height()/2,inset.height()/2,skia.Paint(Color=0xff575c68,AntiAlias=True,Style=skia.Paint.kStroke_Style,StrokeWidth=.55*dp))
  if hint is not None:
   hintFont=skia.Font(skia.Typeface.MakeDefault(),tile.height()*.20);fm=hintFont.getMetrics()
   x=tile.centerX()+tile.width()*.22;y=tile.top()+tile.height()*.25-(fm.fAscent+fm.fDescent)/2
   c.drawString(hint,x-hintFont.measureText(hint)/2,y,hintFont,skia.Paint(Color=0xff575c68,AntiAlias=True))
 out=s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
 mask=masks(data);assert mask.sum()>300;assert np.max(np.abs(out[mask].astype(float)-base[mask]))<=1,('shadow/rim filled hole',name,scale,clip,np.max(np.abs(out[mask].astype(float)-base[mask])))
 return Image.fromarray(out)
fonts=['manrope','outfit','spacegrotesk','syne','orbitron'];checks=0
for name in fonts:
 font=make_font(name)
 for scale in [60,78,95]:
  for clip in [1,.78]:
   data=tiles(font,scale,clip);render(data,.35);checks+=len(data)
 if name in ['syne','orbitron','spacegrotesk']:render(tiles(font),.35).save(folder/f'{name}-preview.png')
data=tiles(make_font('manrope'));frames=[render(data,float(age)) for age in np.linspace(.05,.9,16)]
frames[6].save(R.parent/'RGB-Pulse-19-Preview.png')
frames[0].save(R.parent/'RGB-Pulse-19-Cutouts.gif',save_all=True,append_images=frames[1:]+[render(data,-1)]*3,duration=85,loop=0)
render(data,-1,0xfffefefe).save(R.parent/'RGB-Pulse-19-Light-Preview.png')
print(f'PASS: {checks} glyph/tile fit cases over 5 fonts, 3 sizes and full/short clips; 30 renders verify backdrop preservation within 1/255 raster rounding in hollow letters with rim/shadow; 17-frame wave preview; 300 inside-hint fit and no-collision checks')
