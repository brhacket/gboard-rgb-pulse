package dev.rgbpulse.gboard;

public final class WavePolicyTest {
    private static void equal(float expected,float actual){
        if(!Float.isFinite(actual)||Math.abs(expected-actual)>.0001f)throw new AssertionError(expected+" != "+actual);
    }
    private static float glow(float x,int row,float progress){
        return WavePolicy.glow(x,row,0,0,WavePolicy.radius(progress,300),1,progress);
    }
    public static void main(String[] args){
        equal(0,glow(0,0,0));equal(0,glow(0,0,1));
        equal(0,WavePolicy.progress(-10,720));equal(1,WavePolicy.progress(900,720));
        equal(1,WavePolicy.progress(1,0));
        equal(0,WavePolicy.radius(0,300));equal(300,WavePolicy.radius(1,300));
        // Real row zero is isolated, including compact keyboard rows.
        equal(0,glow(100,25,.2f));
        for(int step=1;step<100;step++){
            float p=step/100f;
            equal(glow(100,0,p),glow(-100,0,p));
            for(int x=-400;x<=400;x+=10){
                float alpha=glow(x,0,p);
                if(!Float.isFinite(alpha)||alpha<0||alpha>1)throw new AssertionError("Unbounded opacity");
            }
        }
        // Ripple movement is linear at every progress sample, independent of fade.
        for(int step=0;step<=100;step++)equal(step*3,WavePolicy.radius(step/100f,300));
        for(int style=0;style<8;style++)for(int step=0;step<=100;step++){
            float value=WavePolicy.glowStyle(100,0,0,0,WavePolicy.radius(step/100f,300),1,step/100f,style);
            if(!Float.isFinite(value)||value<0||value>1)throw new AssertionError("Invalid style opacity");
        }
        if(WavePolicy.glowStyle(0,60,0,0,60,1,.2f,4)<=.8f)throw new AssertionError("Full keyboard must reach other rows");
        equal(0,WavePolicy.glowStyle(-60,0,0,0,60,1,.2f,5));
        if(WavePolicy.glowStyle(60,0,0,0,60,1,.2f,5)<=.8f)throw new AssertionError("Rightward chase missing");
        for(int style=0;style<8;style++){
            equal(0,WavePolicy.glowStyle(60,0,0,0,0,1,0,style));
            equal(0,WavePolicy.glowStyle(60,0,0,0,300,1,1,style));
        }
        // Traveling front lights a neighboring key, then leaves it dark.
        if(glow(WavePolicy.radius(.2f,300),0,.2f)<.8f)throw new AssertionError("Missing wavefront");
        equal(0,glow(100,0,.8f));
        if(glow(0,0,.04f)<=0)throw new AssertionError("Missing initial response");
        equal(0,glow(0,0,.5f));
        // The wavefront's lifetime envelope smoothly dies before expiry.
        float a=WavePolicy.glow(0,0,0,0,0,1,.90f);
        float b=WavePolicy.glow(0,0,0,0,0,1,.99f);
        if(!(a>b&&b<.002f))throw new AssertionError("Abrupt ending");
        System.out.println("PASS: smooth attack/release, symmetric traveling band, compact-row isolation, bounded opacity");
    }
}
