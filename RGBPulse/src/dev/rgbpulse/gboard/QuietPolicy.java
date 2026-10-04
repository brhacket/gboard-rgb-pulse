final class QuietPolicy {
    // Long enough to be seen between two fast taps; bounded so it never lingers.
    static final int DURATION=520;
    static float radius(float width,float height,float dp){return Math.max(1,Math.min(72*dp,Math.min(width,height)*1.15f));}
    static float alpha(long elapsed,int strength){
        if(elapsed<=0||elapsed>=DURATION)return 0;
        // Peak at half strength is a quarter of full brightness: visible, never a wash.
        return (float)Math.sin(Math.PI*elapsed/DURATION)*.5f*Math.max(0,Math.min(100,strength))/100f;
    }
}
