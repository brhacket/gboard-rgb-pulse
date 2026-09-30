package dev.rgbpulse.gboard;
public class LegendPolicyTest {
 public static void main(String[] args){int n=0;
 for(String s:new String[]{"q","space","Enter","ABC","123","Čćšžđ"}){if(!LegendPolicy.eligible(s))throw new AssertionError(s);n++;}
 for(String s:new String[]{""," ","\ue000","🙂","⌫","العربية","漢","a\u0301","a\u200d","a\ufe0f","x\n","#","abcdefghijklmnopqrstuvwxyz"}){if(LegendPolicy.eligible(s))throw new AssertionError(s);n++;}
 System.out.println("PASS: "+n+" legend eligibility guards");
 }
}
