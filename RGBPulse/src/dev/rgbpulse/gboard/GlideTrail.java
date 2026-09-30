package dev.rgbpulse.gboard;
import android.graphics.*;
final class GlideTrail {
 final TrailPoints points=new TrailPoints();
 void draw(Canvas c,Config cfg,float dp,long now){
  if(cfg.trailStyle==0||!points.active(now,cfg.trailLife))return;
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);
  for(int i=1;i<points.count;i++){
   float fade=Math.max(0,1-(now-points.t[i])/(float)cfg.trailLife);if(fade<=0)continue;
   float width=cfg.trailWidth*dp*fade;
   int color=Color.HSVToColor(new float[]{(cfg.hue1+i*3)%360,.70f,1});
   if(cfg.trailStyle==3){p.setColor(cfg.accentColor);p.setAlpha(Math.round(210*fade));p.setStrokeWidth(Math.max(.7f*dp,width*.45f));}
   else {p.setColor(color);p.setAlpha(Math.round(65*fade));p.setStrokeWidth(width*2.5f);c.drawLine(points.x[i-1],points.y[i-1],points.x[i],points.y[i],p);p.setAlpha(Math.round(230*fade));p.setStrokeWidth(width);}
   if(cfg.trailStyle==2){float dx=points.x[i]-points.x[i-1],dy=points.y[i]-points.y[i-1];float len=Math.max(1,(float)Math.hypot(dx,dy));float off=dp*2*fade;for(int sign=-1;sign<=1;sign+=2)c.drawLine(points.x[i-1]-dy/len*off*sign,points.y[i-1]+dx/len*off*sign,points.x[i]-dy/len*off*sign,points.y[i]+dx/len*off*sign,p);}
   else c.drawLine(points.x[i-1],points.y[i-1],points.x[i],points.y[i],p);
  }
 }
}
