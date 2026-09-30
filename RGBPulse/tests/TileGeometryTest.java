package dev.rgbpulse.gboard;
public class TileGeometryTest {
 public static void main(String[] args){int n=0;
  for(float dp:new float[]{1,2,3})for(float cw:new float[]{25,38,60})for(float ch:new float[]{24,32,48})for(int scale:new int[]{60,78,95}){
   float w=cw*dp,h=ch*dp,size=TileGeometry.diameter(w,h,38*dp,dp,scale);
   if(size<0||size>w-4*dp+.001f||size>h-4*dp+.001f)throw new AssertionError("clip fit");
   for(float requested:new float[]{0,h*.4f,h}){
    float cy=TileGeometry.center(requested,2*dp,h-2*dp,size);
    if(cy-size/2<2*dp-.001f||cy+size/2>h-2*dp+.001f)throw new AssertionError("cut bottom");n++;
   }
  }
  if(TileGeometry.diameter(Float.NaN,100,40,2,78)!=0)throw new AssertionError("nan");n++;
  System.out.println("PASS: "+n+" tile fit/centering guards including shortened child clips and density/size variations");
 }
}
