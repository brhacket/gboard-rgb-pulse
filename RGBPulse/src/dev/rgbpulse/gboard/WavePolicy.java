package dev.rgbpulse.gboard;

/** Pure motion curve: a traveling soft band, not a permanently illuminated row. */
final class WavePolicy {
    private static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*x*(x*(x*6-15)+10);}
    static float progress(long elapsed,int duration){return Math.max(0,Math.min(1,elapsed/(float)Math.max(1,duration)));}
    static float radius(float progress,float travel){float p=Math.max(0,Math.min(1,progress));return (1-(1-p)*(1-p)*(1-p))*travel;}
    static float glow(float cx,int top,float origin,int rowTop,float radius,float dp,float progress){
        if(progress<=0||progress>=1||Math.abs((float)top-rowTop)>12*dp)return 0;
        float width=Math.max(1,80*dp);
        float band=smooth(1-Math.abs(Math.abs(cx-origin)-radius)/width);
        float envelope=smooth(progress/.09f)*(1-smooth((progress-.48f)/.52f));
        return band*envelope;
    }
}
