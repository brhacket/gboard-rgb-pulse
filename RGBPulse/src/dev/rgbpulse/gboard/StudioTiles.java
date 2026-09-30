package dev.rgbpulse.gboard;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** v30 stock kept – only letter same color as border, waving, all keys */
final class StudioTiles {
 final IdentityHashMap<View,String> hints=new IdentityHashMap<View,String>();
 final IdentityHashMap<View,RectF> tiles=new IdentityHashMap<View,RectF>();
 final IdentityHashMap<View,String> mains=new IdentityHashMap<View,String>();
 final IdentityHashMap<View,Integer> tops=new IdentityHashMap<View,Integer>();
 static final IdentityHashMap<View,Integer> bodyTops=new IdentityHashMap<View,Integer>();
 static final IdentityHashMap<View,Float> bodyCx=new IdentityHashMap<View,Float>();
 float typicalWidth;
 void bind(List<View> keys){
  hints.keySet().retainAll(keys);tiles.keySet().retainAll(keys);mains.keySet().retainAll(keys);tops.keySet().retainAll(keys);
  bodyTops.keySet().retainAll(keys);bodyCx.keySet().retainAll(keys);
  float[] widths=new float[keys.size()];int n=0;
  for(View k:keys){if(k.getWidth()>0)widths[n++]=k.getWidth();tops.put(k,k.getTop());}
  Arrays.sort(widths,0,n);typicalWidth=n==0?0:widths[n/2];
 }
 void updateBody(ViewGroup body, List<View> keys){
  for(View k:keys){
   Rect r=new Rect(0,0,k.getWidth(),k.getHeight());
   try{body.offsetDescendantRectToMyCoords(k,r);}catch(Throwable ignored){continue;}
   bodyTops.put(k,r.top);bodyCx.put(k,r.exactCenterX());
  }
 }
 boolean hint(CapHooks.Entry e,String text,Paint p){
  if(text.length()!=1||text.charAt(0)<'0'||text.charAt(0)>'9'||p.getTextSize()>=e.key.getHeight()*.22f)return false;
  if(!text.equals(hints.get(e.key))){hints.put(e.key,text);e.key.post(()->KeyStyle.invalidateTree(e.key));}
  return tiles.containsKey(e.key);
 }

 boolean draw(CapHooks.Entry e,String text,Paint p,float x,float y,Canvas c){
  if(e.key.getWidth()<=0||e.key.getHeight()<=0)return false;
  boolean hasText=text!=null && !text.trim().isEmpty();
  if(hasText){
   String tt=text.trim();
   if(tt.length()>12)return false;
   if(!CutoutPolicy.mainLegend(p.getTextSize(),e.key.getHeight()) && !tt.equalsIgnoreCase("space"))return false;
  }
  // Only take over drawing when in active multi-wave, otherwise keep stock Gboard letter
  float glowAlpha=SideSweep.computeGlowAlpha(e.key, e.key.getResources().getDisplayMetrics().density);
  if(glowAlpha<=0.01f) return false; // stock letter visible
  Matrix localToKey=new Matrix();c.getMatrix(localToKey);
  CapHooks.Anchor a=CapHooks.anchor.get();
  if(a!=null&&a.canvas==c){Matrix inv=new Matrix();if(!a.matrix.invert(inv))return false;Matrix relative=new Matrix();relative.setConcat(inv,localToKey);localToKey=relative;}
  float[] v=new float[9];localToKey.getValues(v);if(!CapPolicy.axisAligned(v))return false;
  float w=e.key.getWidth(),h=e.key.getHeight(),dp=e.key.getResources().getDisplayMetrics().density;
  Rect clip=new Rect();if(!c.getClipBounds(clip)||clip.isEmpty())return false;
  RectF available=new RectF(clip);localToKey.mapRect(available);
  if(!available.intersect(0,0,w,h))return false;
  float size=TileGeometry.diameter(available.width(),available.height(),typicalWidth>0?typicalWidth:w,dp,KeyStyle.cfg.tileScale);
  if(size<12*dp)return false;
  RectF safe=new RectF(available);safe.inset(2*dp,2*dp);
  boolean single=text!=null && text.codePointCount(0,text.length())==1;
  float tw=single?size:Math.min(safe.width(),w*.86f);
  float cx=TileGeometry.center(available.centerX(),safe.left,safe.right,tw);
  float cy=TileGeometry.center(available.centerY(),safe.top,safe.bottom,size);
  RectF tile=new RectF(cx-tw/2,cy-size/2,cx+tw/2,cy+size/2);
  String prev=mains.put(e.key,text);
  if(prev!=null&&!prev.equals(text))hints.remove(e.key);
  tiles.put(e.key,new RectF(tile));
  Matrix inv=new Matrix();if(!localToKey.invert(inv))return false;
  Boolean prior=CapHooks.drawing.get();CapHooks.drawing.set(true);
  int save=c.save();
  try{c.concat(inv);paintSolid(c,e.key,tile,text,p,dp);}
  finally{c.restoreToCount(save);if(prior==null)CapHooks.drawing.remove();else CapHooks.drawing.set(prior);}
  return true;
 }
 void clear(){hints.clear();tiles.clear();mains.clear();tops.clear();bodyTops.clear();bodyCx.clear();}
 static int legendColorFor(int tileColor){return 0xffe8e8ec;}
 static int blend(int a,int b,float t){
  int ar=(a>>16)&0xff, ag=(a>>8)&0xff, ab=a&0xff;
  int br=(b>>16)&0xff, bg=(b>>8)&0xff, bb=b&0xff;
  int rr=Math.round(ar*(1-t)+br*t), rg=Math.round(ag*(1-t)+bg*t), rb=Math.round(ab*(1-t)+bb*t);
  return 0xff000000|(rr<<16)|(rg<<8)|rb;
 }



 void paintSolid(Canvas c, View key, RectF tile, String text, Paint orig, float dp){
  int base=0xffe8e8ec;
  int finalLetter=SideSweep.computeFinalBorder(key, base, dp);
  if(text!=null && !text.trim().isEmpty() && !text.trim().equalsIgnoreCase("space")){
   Paint leg=new Paint(orig);leg.setColor(finalLetter);leg.setTextAlign(Paint.Align.CENTER);leg.setShader(null);leg.clearShadowLayer();
   float tw=leg.measureText(text);
   float maxW=tile.width()*(text.codePointCount(0,text.length())==1?.56f:.84f);
   if(tw>maxW && tw>0)leg.setTextSize(leg.getTextSize()*maxW/tw);
   Paint.FontMetrics fm=leg.getFontMetrics();
   float baseline=tile.centerY()-(fm.ascent+fm.descent)/2;
   c.drawText(text,tile.centerX(),baseline,leg);
  }
  String hint=hints.get(key);
  if(hint!=null){
   Paint hp=new Paint(Paint.ANTI_ALIAS_FLAG);hp.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));hp.setColor(finalLetter);hp.setTextSize(tile.height()*.20f);hp.setTextAlign(Paint.Align.CENTER);
   float hx=tile.centerX()+tile.width()*.22f, hy=tile.top+tile.height()*.25f;
   c.drawText(hint,hx,hy-(hp.ascent()+hp.descent())/2,hp);
  }
 }



 static void fitHole(Path hole,RectF tile,boolean single){}
 static void fitHintHole(Path hole,RectF tile,boolean single){}
 static Path shape(RectF tile,Path hole){return null;}
 static void paint(Canvas c,RectF tile,Path shape,float dp,int alpha){}
 static void paintHint(Canvas c,RectF tile,String hint,Config cfg){
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setColor(cfg.accentColor);p.setTextSize(tile.height()*.20f);p.setTextAlign(Paint.Align.CENTER);
  float x=tile.centerX()+tile.width()*.22f,y=tile.top+tile.height()*.25f;
  c.drawText(hint,x,y-(p.ascent()+p.descent())/2,p);
 }
}
