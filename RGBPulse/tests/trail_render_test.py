"""Desktop equivalent of the three bounded line-segment trail styles."""
import skia,numpy as np
from pathlib import Path
from PIL import Image
R=Path(__file__).resolve().parents[1];W,H=780,366;dp=2
points=[(80+i*7,160+55*np.sin(i*.12),1000+i*9) for i in range(80)]
def draw(style,now,border=0):
 s=skia.Surface(W+border*2,H+border*2);c=s.getCanvas();c.clear(0);c.translate(border,border);c.clipRect(skia.Rect.MakeWH(W,H))
 p=skia.Paint(AntiAlias=True,Style=skia.Paint.kStroke_Style,StrokeCap=skia.Paint.kRound_Cap)
 if style:
  for i in range(1,len(points)):
   x,y,t=points[i];lx,ly,_=points[i-1];fade=max(0,1-(now-t)/420)
   if fade<=0:continue
   width=2*dp*fade;color=skia.HSVToColor([(290+i*3)%360,.7,1])
   if style==3:p.setColor(0xff575c68);p.setAlphaf(210/255*fade);p.setStrokeWidth(max(.7*dp,width*.45))
   else:
    p.setColor(color);p.setAlphaf(65/255*fade);p.setStrokeWidth(width*2.5);c.drawLine(lx,ly,x,y,p);p.setAlphaf(230/255*fade);p.setStrokeWidth(width)
   if style==2:
    dx=x-lx;dy=y-ly;length=max(1,np.hypot(dx,dy));off=dp*2*fade
    for sign in [-1,1]:c.drawLine(lx-dy/length*off*sign,ly+dx/length*off*sign,x-dy/length*off*sign,y+dx/length*off*sign,p)
   else:c.drawLine(lx,ly,x,y,p)
 return s.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)
for style in [0,1,2,3]:
 for now in [1750,1800,2100,2300]:
  ar=draw(style,now,8);assert not ar[:8].any() and not ar[-8:].any() and not ar[:,:8].any() and not ar[:,-8:].any()
  if style==0 or now>=2131:assert not ar.any()
 assert not np.array_equal(draw(1,1800),draw(2,1800))
# Actual trail render over the synthetic keyboard preview, not a phone recording.
base=Image.open(R.parent/'RGB-Pulse-19-Preview.png').convert('RGBA')
frames=[]
for now in range(1750,2200,30):frames.append(Image.alpha_composite(base,Image.fromarray(draw(1,now))))
frames[0].save(R.parent/'RGB-Pulse-19-Glide.gif',save_all=True,append_images=frames[1:],duration=65,loop=0)
print('PASS: 16 trail style/fade renders; strict outer clip, off/expired transparency and distinct neon/twin geometry')
