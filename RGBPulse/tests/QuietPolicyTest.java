package dev.rgbpulse.gboard;
public final class QuietPolicyTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int density=1;density<=4;density++)for(int width=20;width<=400;width+=10){
            float radius=QuietPolicy.radius(width*density,48*density,density);
            check(radius<=40*density&&radius<=width*density*.85f);
        }
        for(int strength=0;strength<=100;strength++)for(int elapsed=-10;elapsed<300;elapsed++){
            float a=QuietPolicy.alpha(elapsed,strength);check(a>=0&&a<=.12001f&&Float.isFinite(a));
        }
        check(QuietPolicy.alpha(0,100)==0&&QuietPolicy.alpha(240,100)==0);
        check(QuietPolicy.alpha(120,100)>QuietPolicy.alpha(60,100));
        check(QuietPolicy.alpha(120,100)>QuietPolicy.alpha(180,100));
        System.out.println("PASS: bounded small background radius, 12% maximum opacity and one soft response");
    }
}
