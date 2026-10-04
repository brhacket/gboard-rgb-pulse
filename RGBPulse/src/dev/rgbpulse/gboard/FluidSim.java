package dev.rgbpulse.gboard;

/**
 * CPU physics for the magnetic fluid: a fixed pool of blobs that spring back to
 * home anchors, slide with device tilt, get pulled and swirled by fingertips and
 * splash outward on taps. Pure Java with deterministic seeding so CI can test
 * the motion without Android, a GPU or a device.
 */
public final class FluidSim {
    public static final int COUNT=12, TOUCHES=4;
    static final float SPRING=3.0f, DAMP=2.6f, TILT_GAIN=3.5f, ATTRACT=8.0f, SWIRL=.55f;
    final float[] x=new float[COUNT], y=new float[COUNT], vx=new float[COUNT], vy=new float[COUNT];
    final float[] homeX=new float[COUNT], homeY=new float[COUNT];
    final float[] baseR=new float[COUNT], phase=new float[COUNT], energy=new float[COUNT];
    final int[] pointerId={-1,-1,-1,-1};
    final float[] touchX=new float[TOUCHES], touchY=new float[TOUCHES];
    final float[] pressure=new float[TOUCHES], velX=new float[TOUCHES], velY=new float[TOUCHES];
    final long[] touchTime=new long[TOUCHES];
    float width, height;
    long lastStep=-1;
    float simTime;

    /** Adopt a new panel size. First call seeds the blob field; later calls rescale it. */
    public void bounds(float w,float h,long now){
        if(w<2||h<2)return;
        if(width==w&&height==h)return;
        boolean first=width<=0||height<=0;
        float sx=first?1f:w/width, sy=first?1f:h/height;
        width=w;height=h;
        if(first)seed();
        else for(int i=0;i<COUNT;i++){
            homeX[i]*=sx;homeY[i]*=sy;x[i]*=sx;y[i]*=sy;
        }
        lastStep=now; // A resize never produces a giant integration jump.
    }
    private void seed(){
        // Deterministic low-dispersion layout: golden-ratio jitter, no RNG state.
        float g=.61803398875f, p=.31f;
        float minSide=Math.min(width,height);
        float cellW=width/4f, cellH=height/3f;
        for(int i=0;i<COUNT;i++){
            p=fract(p+g);float p2=fract(p*7.13f), p3=fract(p*3.71f), p4=fract(p*9.37f);
            homeX[i]=(i%4+.5f)*cellW+(p2-.5f)*cellW*.5f;
            homeY[i]=(i/4+.5f)*cellH+(p3-.5f)*cellH*.5f;
            baseR[i]=minSide*(.10f+.05f*p4);
            phase[i]=fract(p*5.19f);
            x[i]=homeX[i];y[i]=homeY[i];vx[i]=0;vy[i]=0;energy[i]=0;
        }
    }
    private static float fract(float v){return v-(float)Math.floor(v);}

    /** Track one pointer slot. down=false releases it; its glow fades in step(). */
    public void pointer(int id,float nx,float ny,boolean down,long now){
        if(!Float.isFinite(nx)||!Float.isFinite(ny))return;
        int slot=-1;
        for(int i=0;i<TOUCHES;i++)if(pointerId[i]==id){slot=i;break;}
        if(slot<0){
            if(!down)return;
            for(int i=0;i<TOUCHES;i++)if(pointerId[i]<0){slot=i;break;}
            if(slot<0)return; // Four simultaneous touches are plenty.
            pointerId[slot]=id;touchTime[slot]=0;velX[slot]=0;velY[slot]=0;pressure[slot]=0;
        }
        if(!down){pointerId[slot]=-1;return;}
        float oldX=touchX[slot], oldY=touchY[slot]; long oldT=touchTime[slot];
        touchX[slot]=nx;touchY[slot]=ny;touchTime[slot]=now;pressure[slot]=1;
        float dt=(now-oldT)/1000f;
        if(oldT>0&&dt>0.004f&&dt<.25f){
            float cap=width*6f+height*6f;
            velX[slot]=clamp(lerp(velX[slot],(nx-oldX)/dt,.5f),-cap,cap);
            velY[slot]=clamp(lerp(velY[slot],(ny-oldY)/dt,.5f),-cap,cap);
        }
    }
    public void cancelTouches(){for(int i=0;i<TOUCHES;i++)pointerId[i]=-1;}
    public void clear(){cancelTouches();for(int i=0;i<COUNT;i++)energy[i]=0;for(int i=0;i<TOUCHES;i++)pressure[i]=0;}

    /** A tap splashes nearby blobs outward and charges them. */
    public void impulse(float nx,float ny,long now){
        if(width<=0)return;
        float reach=Math.min(width,height)*.30f, r2=reach*reach;
        for(int i=0;i<COUNT;i++){
            float dx=x[i]-nx, dy=y[i]-ny, d2=dx*dx+dy*dy;
            float hit=(float)Math.exp(-d2/(r2*.5f));
            if(hit<=.01f)continue;
            float d=(float)Math.sqrt(d2)+.001f;
            float kick=height*2.6f*hit;
            vx[i]+=dx/d*kick;vy[i]+=dy/d*kick;
            energy[i]=Math.min(1f,energy[i]+hit*.9f);
        }
    }

    /** Advance the simulation. tiltX/tiltY are the screen-plane gravity direction, -1..1. */
    public void step(long now,float tiltX,float tiltY){
        if(width<=0)return;
        if(lastStep<0){lastStep=now;return;}
        float dt=(now-lastStep)/1000f;lastStep=now;
        if(dt<=0)return;
        if(dt>.05f)dt=.05f; // A hitch never teleports the fluid.
        simTime+=dt;
        float minSide=Math.min(width,height);
        float soft2=minSide*minSide*.045f;
        for(int s=0;s<TOUCHES;s++){
            if(pointerId[s]<0){
                pressure[s]*=Math.exp(-dt*3.0f);
                if(pressure[s]<.01f)pressure[s]=0;
            }
            float decay=(float)Math.exp(-dt*4.0f);velX[s]*=decay;velY[s]*=decay;
        }
        float maxV=minSide*3f;
        for(int i=0;i<COUNT;i++){
            float ax=(homeX[i]-x[i])*SPRING, ay=(homeY[i]-y[i])*SPRING;
            ax+=clamp(tiltX,-1,1)*height*TILT_GAIN;
            ay+=clamp(tiltY,-1,1)*height*TILT_GAIN;
            // Idle breathing keeps the surface alive between interactions.
            ax+=(float)Math.sin(simTime*.8f+phase[i]*6.2831f)*minSide*.14f;
            ay+=(float)Math.cos(simTime*.63f+phase[i]*4.13f)*minSide*.11f;
            for(int s=0;s<TOUCHES;s++){
                float pr=pressure[s];
                if(pr<=.01f)continue;
                float dx=touchX[s]-x[i], dy=touchY[s]-y[i];
                float d2=dx*dx+dy*dy+soft2;
                float d=(float)Math.sqrt(d2);
                float a=pr*height*ATTRACT*soft2/d2;
                if(a>height*10f)a=height*10f;
                float dir=phase[i]>.5f?1f:-1f; // Each blob swirls its own way.
                ax+=dx/d*a+(-dy/d)*a*SWIRL*dir;
                ay+=dy/d*a+(dx/d)*a*SWIRL*dir;
                energy[i]=Math.min(1f,energy[i]+soft2/d2*pr*dt*3f);
            }
            vx[i]+=ax*dt;vy[i]+=ay*dt;
            float damp=(float)Math.exp(-DAMP*dt);vx[i]*=damp;vy[i]*=damp;
            float v2=vx[i]*vx[i]+vy[i]*vy[i];
            if(v2>maxV*maxV){float scale=maxV/(float)Math.sqrt(v2);vx[i]*=scale;vy[i]*=scale;}
            x[i]+=vx[i]*dt;y[i]+=vy[i]*dt;
            float m=baseR[i]*.4f;
            if(x[i]<m){x[i]=m;vx[i]=Math.abs(vx[i])*.4f;}
            else if(x[i]>width-m){x[i]=width-m;vx[i]=-Math.abs(vx[i])*.4f;}
            if(y[i]<m){y[i]=m;vy[i]=Math.abs(vy[i])*.4f;}
            else if(y[i]>height-m){y[i]=height-m;vy[i]=-Math.abs(vy[i])*.4f;}
            energy[i]*=Math.exp(-dt*1.6f);
        }
    }
    private static float clamp(float v,float lo,float hi){return v<lo?lo:(v>hi?hi:v);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}

    /** Blob uniforms: x, y, animated radius, stored energy. */
    public void fillBlobs(float[] out){
        for(int i=0;i<COUNT;i++){
            int u=i*4;
            out[u]=x[i];out[u+1]=y[i];
            out[u+2]=baseR[i]*(1f+.22f*(float)Math.sin(simTime*1.3f+phase[i]*6.2831f)+.45f*energy[i]);
            out[u+3]=energy[i];
        }
    }
    /** Touch uniforms: x, y, fading pressure, normalized speed. */
    public void fillTouches(float[] out){
        float speedScale=Math.max(1f,(width+height)*2f);
        for(int s=0;s<TOUCHES;s++){
            int u=s*4;
            out[u]=touchX[s];out[u+1]=touchY[s];out[u+2]=pressure[s];
            out[u+3]=Math.min(1f,(float)Math.hypot(velX[s],velY[s])/speedScale);
        }
    }
    /** Velocity uniforms: vx, vy in px/s and normalized speed for drag streaks. */
    public void fillTouchVel(float[] out){
        float speedScale=Math.max(1f,(width+height)*2f);
        for(int s=0;s<TOUCHES;s++){
            int u=s*4;
            out[u]=velX[s];out[u+1]=velY[s];
            out[u+2]=Math.min(1f,(float)Math.hypot(velX[s],velY[s])/speedScale);
            out[u+3]=0f;
        }
    }
}
