package dev.rgbpulse.gboard;

public final class BorderFadeTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    static float sample(int step,boolean release){
        BorderFade fade=new BorderFade();fade.advance(1,0);
        for(int t=step;t<=240;t+=step)fade.advance(1,t);
        if(release)for(int t=240+step;t<=720;t+=step)fade.advance(0,t);
        return fade.value();
    }
    public static void main(String[] args){
        BorderFade fade=new BorderFade();check(fade.advance(1,0)==0);
        float first=fade.advance(1,16);check(first>0&&first<.4f);
        float second=fade.advance(1,32);check(second>first&&second<1);
        float released=fade.advance(0,48);check(released>0&&released<second);
        float previous=released;
        for(int t=64;t<1600;t+=16){float current=fade.advance(0,t);check(current>=0&&current<=previous);previous=current;}
        check(!fade.active());
        check(Math.abs(sample(8,false)-sample(16,false))<.0001f);
        check(Math.abs(sample(8,true)-sample(16,true))<.0001f);
        fade.clear();check(!fade.active()&&fade.value()==0);
        fade.advance(1,2000);check(fade.advance(1,2016)<.4f);
        System.out.println("PASS: gradual per-key attack/release, bounded opacity, refresh-rate parity and finite cleanup");
    }
}
