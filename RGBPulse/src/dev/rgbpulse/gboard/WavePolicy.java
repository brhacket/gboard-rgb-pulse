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
        if(style<4&&Math.abs((float)top-rowTop)>12*dp)return 0;
        float width=Math.max(1,(style==2?128:80)*dp);
        float distance=Math.abs(cx-origin);
        if(style==4)distance=(float)Math.hypot(cx-origin,top-rowTop);
        float dx=Math.abs(cx-origin),dy=Math.abs((float)top-rowTop);
        if(style==5)distance=dx+dy;
        if(style==6)distance=Math.max(dx,dy);
        if(style==7)distance=Math.abs((cx-origin)+(top-rowTop))*.7071f;
        if(style==8)distance=(float)Math.hypot(dx,dy);
        float band=smooth(1-Math.abs(distance-radius)/width);
        if(style==1)band=Math.max(band,.55f*smooth(1-Math.abs(distance-Math.max(0,radius-90*dp))/width));
        if(style==3)band=smooth(1-distance/Math.max(1,52*dp));
        if(style==6)band*=smooth(1-Math.min(dx,dy)/Math.max(1,42*dp));
        if(style==7)band*=smooth(1-(float)Math.hypot(dx,dy)/Math.max(1,500*dp));
        if(style==8){
            int cell=(int)Math.floor((cx-origin)/(40*dp))+(int)Math.floor((top-rowTop)/(48*dp));
            float offset=(Math.abs(cell)%2)*36*dp;
            band=smooth(1-Math.abs(distance-radius+offset)/Math.max(1,55*dp));
        }
        float envelope=smooth(progress/.09f)*(1-smooth((progress-.48f)/.52f));
        return band*envelope;
    }
}
