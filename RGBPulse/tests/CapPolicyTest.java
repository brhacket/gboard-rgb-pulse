package dev.rgbpulse.gboard;
public class CapPolicyTest {
 public static void main(String[] args){int n=0;
 for(float[] r:new float[][]{{0,0,100,100},{8,8,92,92},{12,12,88,88}}){if(!CapPolicy.cap(r[0],r[1],r[2],r[3],100,100))throw new AssertionError();n++;}
 for(float[] r:new float[][]{{30,30,70,70},{0,0,200,100},{-20,0,80,100},{0,0,100,15},{0,0,15,100},{Float.NaN,0,100,100},{20,30,80,70}}){if(CapPolicy.cap(r[0],r[1],r[2],r[3],100,100))throw new AssertionError();n++;}
 if(!CapPolicy.cap(8,4,292,76,300,80))throw new AssertionError("space");n++;
 if(!CapPolicy.axisAligned(new float[]{1,0,30,0,1,20,0,0,1}))throw new AssertionError("translation");n++;
 if(CapPolicy.axisAligned(new float[]{1,1,0,0,1,0,0,0,1}))throw new AssertionError("skew");n++;
 float[] circle=new float[48];for(int i=0;i<24;i++){circle[2*i]=50+45*(float)Math.cos(i*Math.PI/12);circle[2*i+1]=50+45*(float)Math.sin(i*Math.PI/12);}
 if(!CapPolicy.plate(circle,90,90))throw new AssertionError("round action plate");n++;
 if(CapPolicy.plate(new float[]{0,0,100,0,100,20,40,20,40,80,100,80,100,100,0,100},100,100))throw new AssertionError("concave icon");n++;
 if(CapPolicy.plate(new float[]{0,50,10,40,40,40,40,0,60,0,60,40,100,40,100,60,60,60,60,100,40,100,40,60,10,60},100,100))throw new AssertionError("plus icon");n++;
 if(CapPolicy.cap(40,8,120,92,128,100))throw new AssertionError("old guard must reject asymmetric A fixture");n++;
 if(!CapPolicy.paddedCap(40,8,120,92,128,100,32,0,0,0))throw new AssertionError("A content area");n++;
 if(!CapPolicy.paddedCap(8,8,88,92,128,100,0,0,32,0))throw new AssertionError("L content area");n++;
 if(!CapPolicy.edgeCap(40,8,120,92,128,100,80,84))throw new AssertionError("A cohort plate");n++;
 if(!CapPolicy.edgeCap(8,8,88,92,128,100,80,84))throw new AssertionError("L cohort plate");n++;
 if(CapPolicy.edgeCap(40,30,75,65,128,100,80,84))throw new AssertionError("small icon");n++;
 if(CapPolicy.edgeCap(40,8,120,92,128,100,50,84))throw new AssertionError("unrelated width");n++;
 if(CapPolicy.paddedCap(40,30,75,65,128,100,40,30,53,35))throw new AssertionError("icon padding");n++;
 if(CapPolicy.edgeCap(-4,8,76,92,128,100,80,84))throw new AssertionError("outside key");n++;
 System.out.println("PASS: "+n+" cap geometry/transform assertions; icons/small glyphs excluded");
 }
}
