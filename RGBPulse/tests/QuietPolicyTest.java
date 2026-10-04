package dev.rgbpulse.gboard;

public final class QuietPolicyTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int density=1;density<=4;density++)for(int width=20;width<=400;width+=10){
            float radius=QuietPolicy.radius(width*density,48*density,density);
            check(radius<=72*density&&radius<=width*density*1.15f&&radius>=1);
        }
        for(int strength=0;strength<=100;strength++)for(int elapsed=-10;elapsed<600;elapsed++){
            float a=QuietPolicy.alpha(elapsed,strength);check(a>=0&&a<=.50001f&&Float.isFinite(a));
        }
        check(QuietPolicy.alpha(0,100)==0&&QuietPolicy.alpha(QuietPolicy.DURATION,100)==0);
        check(QuietPolicy.alpha(QuietPolicy.DURATION/2,100)>QuietPolicy.alpha(QuietPolicy.DURATION/4,100));
        check(QuietPolicy.alpha(QuietPolicy.DURATION/2,100)>QuietPolicy.alpha(3*QuietPolicy.DURATION/4,100));
        // The glow is meant to be seen: half strength still reaches a quarter of full alpha.
        check(QuietPolicy.alpha(QuietPolicy.DURATION/2,100)>=.45f);
        check(QuietPolicy.alpha(QuietPolicy.DURATION/2,50)>=.22f);
        check(QuietPolicy.alpha(QuietPolicy.DURATION/2,10)>=.04f);
        System.out.println("PASS: visible background glow with bounded radius, 50% maximum opacity and one soft response");
    }
}
