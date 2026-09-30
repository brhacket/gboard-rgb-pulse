package dev.rgbpulse.gboard;
public class CutoutPolicyTest {
 public static void main(String[] args){
  if(!CutoutPolicy.mainLegend(30,90)||CutoutPolicy.mainLegend(12,90)||CutoutPolicy.mainLegend(80,90)||CutoutPolicy.mainLegend(Float.NaN,90))throw new AssertionError("legend gate");
  if(!CapPolicy.offsetPlate(42,4,101,80,106,84)||!CapPolicy.offsetPlate(5,4,64,80,106,84))throw new AssertionError("asymmetric edge plates");
  if(CapPolicy.offsetPlate(42,24,62,44,106,84)||CapPolicy.offsetPlate(-1,4,80,80,106,84)||CapPolicy.offsetPlate(4,4,101,80,106,84))throw new AssertionError("icon/outside/full-size exclusions");
  System.out.println("PASS: 9 main-letter and offset A/L-shaped plate guards (synthetic geometry, not device proof)");
 }
}
