package dev.rgbpulse.gboard;
import android.graphics.*;
import android.view.View;
import java.util.*;
/** v31 stock kept, multi-wave row expanding both ways, all keys, one color */
final class SideSweep {
 static final class Wave {
  float originX, originXParent;
  int rowTop, rowTopParent;
  long start;
  float radius, progress;int style;
  Wave(float xp,int tp,long now,float xb,int tb){originXParent=xp;rowTopParent=tp;originX=xb;rowTop=tb;start=now;radius=0;}
 }
 static final List<Wave> waves=new ArrayList<Wave>();
 static boolean activeRow=false; // kept for compat, true if any wave active
 static float originX, originXParent, sweepRadius, sweepX, sweepXParent;
 static int rowTop, rowTopParent;
 static long start;
 float localOriginX; long localStart; boolean active; final RectF tmp=new RectF();

 void tapParent(float xParent,int topParent,long now, float xBody,int topBody){
  synchronized(waves){
   waves.add(new Wave(xParent,topParent,now,xBody,topBody));
   if(waves.size()>8) waves.remove(0); // limit to 8 simultaneous waves
  }
  localOriginX=xBody;localStart=now;active=true;
  originX=xBody; originXParent=xParent;
  rowTop=topBody; rowTopParent=topParent;
  start=now; activeRow=true;
  sweepRadius=0; sweepX=xBody; sweepXParent=xParent;
 }
 void tap(float x,int top,long now){ tapParent(x,top,now,x,top); }
 void clear(){active=false; activeRow=false; sweepRadius=0; synchronized(waves){waves.clear();}}
 boolean active(long now,int duration){
  synchronized(waves){
   Iterator<Wave> it=waves.iterator();
   while(it.hasNext()){
    Wave w=it.next();
    if(now-w.start>=duration) it.remove();
   }
   activeRow=!waves.isEmpty();
   if(waves.isEmpty()){active=false; sweepRadius=0; return false;}
   // keep latest as compat
   Wave last=waves.get(waves.size()-1);
   originX=last.originX; originXParent=last.originXParent;
   rowTop=last.rowTop; rowTopParent=last.rowTopParent;
   start=last.start;
   localOriginX=last.originX; localStart=last.start; active=true;
   return true;
  }
 }
 void draw(Canvas c,Config cfg,RectF play,float dp,long now){advance(cfg,play,now);}
 // Advance before invalidating cached key display lists as well as before a panel draw.
 void advance(Config cfg,RectF play,long now){
  synchronized(waves){
   if(waves.isEmpty())return;
   for(Wave w:waves){
    w.style=cfg.rippleStyle;
    w.progress=WavePolicy.progress(now-w.start,cfg.duration);
    float maxToEdge=Math.max(w.originX-play.left,play.right-w.originX);
    w.radius=WavePolicy.radius(w.progress,Math.max(maxToEdge,play.width()*.60f));
   }
   // compat: set radius to max of waves for old code
   float maxR=0;
   for(Wave w:waves) if(w.radius>maxR) maxR=w.radius;
   sweepRadius=maxR;
  }
 }
 // Body coordinates may legitimately be zero. Choose the coordinate space by
 // map presence, never by the numeric value of the tapped row or origin.
 static float computeGlowAlpha(View view, float dp){
  Integer top=StudioTiles.bodyTops.get(view);
  Float cx=StudioTiles.bodyCx.get(view);
  boolean body=top!=null && cx!=null;
  return glowAt(body?cx:view.getLeft()+view.getWidth()/2f,
      body?top:view.getTop(),dp,body);
 }
 static float glowAt(float cx,int top,float dp,boolean body){
  synchronized(waves){
   float best=0;
   for(int i=0;i<waves.size();i++){
    Wave w=waves.get(i);
    int rt=body?w.rowTop:w.rowTopParent;
    float origin=body?w.originX:w.originXParent;
    best=Math.max(best,WavePolicy.glowStyle(cx,top,origin,rt,w.radius,dp,w.progress,w.style));
   }
   return best;
  }
 }
 static int computeFinalBorder(View view,int base,float dp){
  float fade=computeGlowAlpha(view,dp);
  return fade>0.01f?StudioTiles.blend(base,0xffffffff,0.2f+0.8f*fade):base;
 }
}
