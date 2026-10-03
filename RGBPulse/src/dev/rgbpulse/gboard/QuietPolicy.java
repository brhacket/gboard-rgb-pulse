package dev.rgbpulse.gboard;
final class QuietPolicy {
    static final int DURATION=240;
    static float radius(float width,float height,float dp){return Math.max(1,Math.min(40*dp,Math.min(width,height)*.85f));}
    static float alpha(long elapsed,int strength){
        if(elapsed<=0||elapsed>=DURATION)return 0;
        return (float)Math.sin(Math.PI*elapsed/DURATION)*.12f*Math.max(0,Math.min(100,strength))/100f;
    }
}
