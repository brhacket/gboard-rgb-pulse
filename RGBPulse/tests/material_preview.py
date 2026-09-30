"""Actual AGSL/compositing preview on a synthetic keyboard, not an Android recording."""
import skia,struct,numpy as np
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import colorsys
R=Path(__file__).resolve().parents[1]
field=skia.RuntimeEffect.MakeForShader((R/'shaders/field.agsl').read_text())
glass=skia.RuntimeEffect.MakeForShader((R/'shaders/glass.agsl').read_text())
letter=skia.RuntimeEffect.MakeForShader((R/'shaders/letter.agsl').read_text())
W,H=780,366;dp=2
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
(R/'tests/rendered').mkdir(exist_ok=True)
f=instantiateVariableFont(TTFont(R/'assets/fonts/manrope.ttf'),{'wght':550},inplace=False)
f.save(R/'tests/rendered/manrope-550.ttf')
font=skia.Font(skia.Typeface.MakeFromFile(str(R/'tests/rendered/manrope-550.ttf')),32)
boxes=[];gap=10;kw=(W-gap*11)/10;rh=(H-gap*5)/4
for ri,row in enumerate(['qwertyuiop','asdfghjkl','zxcvbnm']):
 x=(W-(len(row)*kw+(len(row)-1)*gap))/2;y=gap+ri*(rh+gap)
 for ch in row:
  boxes.append((x,y,x+kw,y+rh,ch));x+=kw+gap
boxes.append((W*.24,gap+3*(rh+gap),W*.76,H-gap,'space'))
def data(vals):return skia.Data.MakeWithCopy(struct.pack('='+str(len(vals))+'f',*vals))
def field_shader(age,style=0):
 taps=[W*.44,H*.38,age,.72, W*.76,H*.60,max(-1,age-.28),.5]+[0,0,-1,0]*2
 return field.makeShader(data([W,H,age*1.8,.9,1.2,1,style,0,290/360,190/360,1]+taps))
def lens(sh,b,opt,texture=.6,strength=1,press=0):
 return glass.makeShader(data(list(b)+[dp,opt,press,texture,strength]),sh,1)
def geom(b):
 l,t,r,bottom,ch=b;inner=[l+2*dp,t+3*dp,r-2*dp,bottom-3*dp];radius=min(10*dp,min(inner[2]-inner[0],inner[3]-inner[1])*.29)
 path=skia.Path();path.addRoundRect(skia.Rect.MakeLTRB(*inner),radius,radius);return inner,path

def render(age=.4,style=0,texture=.6,strength=1):
 sh=field_shader(age,style);surf=skia.Surface(W,H);c=surf.getCanvas();c.clear(skia.ColorSetRGB(21,23,30))
 c.save()
 for b in boxes:c.clipPath(geom(b)[1],skia.ClipOp.kDifference,True)
 c.drawPaint(skia.Paint(Shader=sh));c.restore()
 for b in boxes:
  inner,path=geom(b)
  c.drawRect(skia.Rect.MakeLTRB(*b[:4]),skia.Paint(Shader=lens(sh,inner,1,texture,strength)))
  c.drawRect(skia.Rect.MakeLTRB(*b[:4]),skia.Paint(Shader=lens(sh,inner,0,texture,strength)))
  text=b[4];x=(b[0]+b[2])/2-font.measureText(text)/2;m=font.getMetrics();y=(b[1]+b[3])/2-(m.fAscent+m.fDescent)/2
  tint=colorsys.hsv_to_rgb(290/360,.055,1)
  ink=letter.makeShader(data([x+font.measureText(text)/2,y,32,.4]+list(tint)))
  shadow=skia.Paint(Color=skia.ColorSetARGB(56,240,163,255),AntiAlias=True)
  shadow.setMaskFilter(skia.MaskFilter.MakeBlur(skia.BlurStyle.kNormal_BlurStyle,.73))
  c.drawString(text,x,y,font,shadow)
  c.drawString(text,x,y,font,skia.Paint(Shader=ink,AntiAlias=True))
 return Image.fromarray(surf.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)).convert('RGB')
if __name__=='__main__':
 folder=R/'tests/rendered';folder.mkdir(exist_ok=True)
 render().save(folder/'material-keyboard.png')
 render(-1).save(folder/'material-idle.png')
 render(.4,1).save(folder/'material-neon.png')
 render().crop((230,80,485,220)).resize((1020,560)).save(folder/'material-detail.png')
 frames=[render(i/25,0) for i in range(26)]
 frames[0].save(R.parent/'RGB-Pulse-11-Material.gif',save_all=True,append_images=frames[1:],duration=72,loop=0)
 print('Rendered material, idle, neon and animation')
