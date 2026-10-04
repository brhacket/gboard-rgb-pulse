package dev.rgbpulse.gboard;

/** Per-key attack/release smoothing, using elapsed time rather than frame count. */
final class BorderFade {
    private static final float EPSILON=.001f;
    private float value;
    private long last=-1;
    float advance(float target,long now){
        target=Math.max(0,Math.min(1,target));
        if(last<0||now<last){last=now;return value;}
        long elapsed=now-last;last=now;
        // A quick but gradual arrival; a longer release keeps borders from snapping off.
        float tau=target>value?45f:130f;
        float weight=(float)(1-Math.exp(-elapsed/tau));
        value+=(target-value)*weight;
        if(target==0&&value<EPSILON)value=0;
        return value;
    }
    float value(){return value;}
    boolean active(){return value>0;}
    void clear(){value=0;last=-1;}
}
