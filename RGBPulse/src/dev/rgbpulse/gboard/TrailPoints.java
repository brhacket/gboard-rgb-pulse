package dev.rgbpulse.gboard;
/** RAM-only bounded visual samples. Never persisted or decoded into words. */
final class TrailPoints {
 final float[] x=new float[96],y=new float[96];final long[] t=new long[96];int count;boolean tracking,moved;float startX,startY;
 void clear(){java.util.Arrays.fill(x,0);java.util.Arrays.fill(y,0);java.util.Arrays.fill(t,0);count=0;tracking=false;moved=false;}
 void begin(float px,float py,long now){clear();if(!Float.isFinite(px)||!Float.isFinite(py))return;tracking=true;startX=px;startY=py;add(px,py,now);}
 void move(float px,float py,long now,float threshold){if(!tracking||!Float.isFinite(px)||!Float.isFinite(py))return;if(Math.hypot(px-startX,py-startY)>=threshold)moved=true;add(px,py,now);}
 void end(){tracking=false;if(!moved)clear();}
 void add(float px,float py,long now){if(!Float.isFinite(px)||!Float.isFinite(py))return;if(count==96){System.arraycopy(x,1,x,0,95);System.arraycopy(y,1, y,0,95);System.arraycopy(t,1,t,0,95);count--; }x[count]=px;y[count]=py;t[count++]=now;}
 boolean active(long now,int life){
  if(life<=0 || count==0){clear();return false;}
  long age=now-t[count-1];
  if(age<0 || age>=life){clear();return false;}
  return moved&&count>1;
 }
}
