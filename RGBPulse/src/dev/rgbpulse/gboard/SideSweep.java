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
  float radius;
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
 void draw(Canvas c,Config cfg,RectF play,float dp,long now){
  synchronized(waves){
   if(waves.isEmpty())return;
   for(Wave w:waves){
    float raw=(now-w.start)/(float)cfg.duration;
    if(raw<0) raw=0; if(raw>1) raw=1;
    float progress=1-(1-raw)*(1-raw)*(1-raw);
    float maxToEdge=Math.max(w.originX-play.left, play.right-w.originX);
    float travel=Math.max(maxToEdge, play.width()*0.60f);
    w.radius=progress*travel;
   }
   // compat: set radius to max of waves for old code
   float maxR=0;
   for(Wave w:waves) if(w.radius>maxR) maxR=w.radius;
   sweepRadius=maxR;
  }
 }
 // Helper for Glass/StudioTiles to compute final border color with multi-wave blending, one color uniform (white, no green)
 static int computeFinalBorder(View view, int base, float dp){
  synchronized(waves){
   if(waves.isEmpty()) return base;
   Integer bodyTop=StudioTiles.bodyTops.get(view);
   Float bodyCx=StudioTiles.bodyCx.get(view);
   int topCheck;
   float keyCx;
   if(bodyTop!=null && bodyCx!=null){
    topCheck=bodyTop;
    keyCx=bodyCx;
   }else{
    topCheck=view.getTop();
    keyCx=view.getLeft()+view.getWidth()/2f;
   }
   float bestFade=0;
   for(Wave w:waves){
    int rt=w.rowTop!=0?w.rowTop:w.rowTopParent;
    if(rt!=0 && Math.abs(topCheck-rt)>36*dp) continue;
    float origin=w.originX!=0?w.originX:w.originXParent;
    float dist=Math.abs(keyCx-origin);
    if(dist<=w.radius && w.radius>1){
     float fade=1f-dist/w.radius;
     if(fade>bestFade) bestFade=fade;
    }
   }
   if(bestFade>0.01f){
    // One color uniform – white glow, not green, blend base light -> white
    int white=0xffffffff;
    float t=0.2f+0.8f*bestFade;
    return StudioTiles.blend(base, white, t);
   }
   return base;
  }
 }
 static float computeGlowAlpha(View view, float dp){
  synchronized(waves){
   if(waves.isEmpty()) return 0;
   Integer bodyTop=StudioTiles.bodyTops.get(view);
   Float bodyCx=StudioTiles.bodyCx.get(view);
   int topCheck;
   float keyCx;
   if(bodyTop!=null && bodyCx!=null){
    topCheck=bodyTop;
    keyCx=bodyCx;
   }else{
    topCheck=view.getTop();
    keyCx=view.getLeft()+view.getWidth()/2f;
   }
   float best=0;
   for(Wave w:waves){
    int rt=w.rowTop!=0?w.rowTop:w.rowTopParent;
    if(rt!=0 && Math.abs(topCheck-rt)>36*dp) continue;
    float origin=w.originX!=0?w.originX:w.originXParent;
    float dist=Math.abs(keyCx-origin);
    if(dist<=w.radius && w.radius>1){
     float fade=1f-dist/w.radius;
     if(fade>best) best=fade;
    }
   }
   return best;
  }
 }
}
