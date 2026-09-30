package dev.rgbpulse.gboard;
/** Pure fit math: visual tiles fit the effective canvas clip, without changing hit targets. */
final class TileGeometry {
 static float diameter(float clipWidth,float clipHeight,float typicalWidth,float density,int percent){
  if(!Float.isFinite(clipWidth)||!Float.isFinite(clipHeight)||!Float.isFinite(typicalWidth)||density<=0)return 0;
  float limit=Math.min(clipWidth-4*density,clipHeight-4*density);
  return Math.max(0,Math.min(limit,Math.max(0,typicalWidth)*Math.max(60,Math.min(95,percent))/100f));
 }
 static float center(float requested,float lo,float hi,float size){return Math.max(lo+size/2,Math.min(hi-size/2,requested));}
}
