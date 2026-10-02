package dev.rgbpulse.gboard;

/** RGB-only tint: preserves native text opacity, size and shaping. */
final class LegendTint {
    static int color(int original,float wave){
        wave=Math.max(0,Math.min(1,wave));
        int r=(original>>>16)&255,g=(original>>>8)&255,b=original&255;
        boolean light=(299*r+587*g+114*b)>=128000;
        int target=light?255:0;
        return (original&0xff000000)|(channel(r,target,wave,light)<<16)
            |(channel(g,target,wave,light)<<8)|channel(b,target,wave,light);
    }
    private static int channel(int value,int target,float wave,boolean light){
        // Light legends become gently dimmer at rest; dark legends retain contrast
        // on light themes by becoming slightly lighter instead of turning white.
        float idle=light?value*.86f:value+(255-value)*.10f;
        return Math.round(idle+(target-idle)*wave);
    }
}
