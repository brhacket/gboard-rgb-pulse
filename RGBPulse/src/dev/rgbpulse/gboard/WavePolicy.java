package dev.rgbpulse.gboard;

/** Pure motion curve: a traveling soft band, not a permanently illuminated row. */
final class WavePolicy {
    private static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*x*(x*(x*6-15)+10);}
    static float progress(long elapsed,int duration){return Math.max(0,Math.min(1,elapsed/(float)Math.max(1,duration)));}
    static float radius(float progress,float travel){float p=Math.max(0,Math.min(1,progress));return p*travel;}
    static float glow(float cx,int top,float origin,int rowTop,float radius,float dp,float progress){
        return glowStyle(cx,top,origin,rowTop,radius,dp,progress,0);
    }
    static float glowStyle(float cx,int top,float origin,int rowTop,float radius,float dp,float progress,int style){
        if(progress<=0||progress>=1)return 0;
        if(style!=4&&Math.abs((float)top-rowTop)>12*dp)return 0;
        float width=Math.max(1,(style==2?128:80)*dp);
        float distance=Math.abs(cx-origin);
        if(style==4)distance=(float)Math.hypot(cx-origin,top-rowTop);
        if(style==5&&cx<origin-8*dp)return 0;
        if(style==6)radius=Math.max(0,radius/Math.max(.001f,progress)-radius);
        float band=smooth(1-Math.abs(distance-radius)/width);
        if(style==1)band=Math.max(band,.55f*smooth(1-Math.abs(distance-Math.max(0,radius-90*dp))/width));
        if(style==3)band=smooth(1-distance/Math.max(1,52*dp));
        if(style==7){
            band=smooth(1-distance/Math.max(1,160*dp));
            float beats=(float)Math.pow(Math.max(0,Math.sin(progress*Math.PI*2)),2);
            beats+=.65f*(float)Math.pow(Math.max(0,Math.sin((progress-.42f)*Math.PI*3)),2);
            band*=Math.min(1,beats);
        }
        float envelope=smooth(progress/.09f)*(1-smooth((progress-.48f)/.52f));
        return band*envelope;
    }
}
