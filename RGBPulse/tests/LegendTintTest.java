package dev.rgbpulse.gboard;

public final class LegendTintTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        int original=0xffeeeeee;
        int idle=LegendTint.color(original,0),active=LegendTint.color(original,1);
        check((idle&255)<(original&255)&&(idle&255)>190);
        check(active==0xffffffff);
        int previous=idle&255;
        for(int step=1;step<=100;step++){
            int color=LegendTint.color(original,step/100f);check((color>>>24)==255);
            check((color&255)>=previous);previous=color&255;
        }
        check((LegendTint.color(0x7feeeeee,.4f)>>>24)==127);
        check((LegendTint.color(0x00eeeeee,.8f)>>>24)==0);
        check(LegendTint.color(0xff111111,1)==0xff000000);
        check((LegendTint.color(0xff111111,0)&255)<50); // no white text on a light native theme
        check(LegendTint.color(original,-1)==idle&&LegendTint.color(original,2)==active);
        System.out.println("PASS: subtle idle tint, monotonic ripple tint, native alpha and light-theme contrast preserved");
    }
}
