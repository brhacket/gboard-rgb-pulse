package dev.rgbpulse.gboard;
public class LegendWaveTest {
 public static void main(String[] args){
  LegendWave w=new LegendWave();if(w.active(0))throw new AssertionError();
  w.tap(0,0,1000,1000);
  if(w.level(0,0,1099)<.99||w.level(500,0,1099)!=0)throw new AssertionError("origin first");
  if(w.level(500,0,1495)<.99||w.level(0,0,1495)!=0)throw new AssertionError("travels");
  if(w.active(2100)||w.level(500,0,2200)!=0)throw new AssertionError("ends dim");
  for(int i=0;i<20;i++)w.tap(i*10,0,1000,3000+i);
  for(int t=3000;t<4200;t+=10){float v=w.level(50,20,t);if(v<0||v>1||!Float.isFinite(v))throw new AssertionError("bounds");}
  w.clear();if(w.active(3050)||w.level(0,0,3050)!=0)throw new AssertionError("reset");
  System.out.println("PASS: legend wave origin/delayed propagation/final dim frame/20 rapid taps/120 bounded samples/reset");
 }
}
