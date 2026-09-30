package dev.rgbpulse.gboard;
/** Bounded, geometry-only travelling brightness pulses; never stores key text. */
final class LegendWave {
    final float[] xs=new float[4],ys=new float[4],spans=new float[4];
    final long[] starts={-1,-1,-1,-1};int next;
    static final long LIFE=1100;
    void tap(float x,float y,float span,long now){int i=next++%4;xs[i]=x;ys[i]=y;spans[i]=Math.max(1,span);starts[i]=now;}
    void clear(){java.util.Arrays.fill(starts,-1);next=0;}
    boolean active(long now){for(long t:starts)if(t>=0&&now>=t&&now-t<LIFE)return true;return false;}
    float level(float x,float y,long now){
        float peak=0;
        for(int i=0;i<4;i++){
            if(starts[i]<0)continue;float age=(now-starts[i])/(float)LIFE;if(age<0||age>=1)continue;
            float distance=(float)Math.hypot(x-xs[i],y-ys[i])/spans[i];
            float phase=(age-distance*.72f)/.18f;
            if(phase>=0&&phase<=1)peak=Math.max(peak,(float)Math.sin(Math.PI*phase));
        }return peak;
    }
}
