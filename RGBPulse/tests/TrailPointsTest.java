package dev.rgbpulse.gboard;
public class TrailPointsTest {
 public static void main(String[] args){TrailPoints p=new TrailPoints();int n=0;
  p.begin(20,20,100);p.move(21,21,110,8);if(p.active(110,400))throw new AssertionError("tap shouldn't trail");n++;
  p.move(40,20,120,8);if(!p.active(120,400))throw new AssertionError("glide");n++;
  for(int i=0;i<300;i++)p.move(i,30,130+i,8);
  if(p.count!=96||p.x[95]!=299)throw new AssertionError("bounded buffer");n++;
  p.end();if(!p.active(500,400)||p.active(900,400)||p.count!=0)throw new AssertionError("fade then clear");n++;
  p.begin(0,0,1000);p.end();if(p.count!=0)throw new AssertionError("tap discard");n++;
  p.begin(Float.NaN,0,1100);if(p.tracking||p.count!=0)throw new AssertionError("invalid start");n++;
  p.begin(1,1,1200);p.move(Float.NaN,50,1210,8);if(p.count!=1)throw new AssertionError("invalid move");n++;
  p.clear();if(p.tracking||p.moved||p.count!=0)throw new AssertionError("cancel");n++;
  p.begin(0,0,1300);p.move(30,0,1305,8);p.begin(70,40,1310);if(p.count!=1||p.moved)throw new AssertionError("new gesture not bridged");n++;
  System.out.println("PASS: "+n+" glide movement/tap/buffer/expiry/cancel/nonfinite/new-gesture cases");
 }
}
