package dev.rgbpulse.gboard;

/**
 * CPU physics for the magnetic fluid. The fluid is ONE coherent bubble at
 * rest; it darts directly to a touch, splits into equal parts when several
 * fingers are down, and the parts flow back together when they lift. Area is
 * conserved across the split (each part gets 1/N of the mass). Pure Java with
 * deterministic behavior so CI can test it without Android or a device.
 */
public final class FluidSim {
    public static final int COUNT=12;      // shader array size; only DROPS are used
    public static final int DROPS=4, TOUCHES=4;
    static final float SPRING=110f, DAMP=16f, SHARE_RATE=6f, RADIUS_RATE=8f;
    final float[] x=new float[DROPS], y=new float[DROPS], vx=new float[DROPS], vy=new float[DROPS];
    final float[] share=new float[DROPS], rad=new float[DROPS], energy=new float[DROPS];
    final int[] pointerId={-1,-1,-1,-1};
    final float[] touchX=new float[TOUCHES], touchY=new float[TOUCHES];
    final float[] pressure=new float[TOUCHES], velX=new float[TOUCHES], velY=new float[TOUCHES];
    final long[] touchTime=new long[TOUCHES];
    float width, height;
    long lastStep=-1;
    float simTime;
    private int lastN=-1;

    /** Adopt a new panel size. First call places the bubble; later calls rescale it. */
    public void bounds(float w,float h,long now){
        if(w<2||h<2)return;
        if(width==w&&height==h)return;
        boolean first=width<=0||height<=0;
        float sx=first?1f:w/width, sy=first?1f:h/height;
        width=w;height=h;
        if(first){
            x[0]=w*.5f;y[0]=h*.5f;share[0]=1f;rad[0]=baseRadius();
        }else for(int i=0;i<DROPS;i++){x[i]*=sx;y[i]*=sy;rad[i]*=Math.min(sx,sy);}
        lastStep=now; // A resize never produces a giant integration jump.
    }
    float baseRadius(){return Math.min(width,height)*.30f;}

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
            touchX[slot]=nx;touchY[slot]=ny;
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
    public void clear(){cancelTouches();for(int i=0;i<DROPS;i++)energy[i]=0;for(int i=0;i<TOUCHES;i++)pressure[i]=0;}

    /** A touchdown flashes the drop that answers it. */
    public void impulse(float nx,float ny,long now){
        if(width<=0)return;
        int best=-1;float bestD=Float.MAX_VALUE;
        for(int i=0;i<DROPS;i++){
            if(pointerId[i]<0)continue;
            float d=(touchX[i]-nx)*(touchX[i]-nx)+(touchY[i]-ny)*(touchY[i]-ny);
            if(d<bestD){bestD=d;best=i;}
        }
        if(best>=0)energy[best]=Math.min(1f,energy[best]+.7f);
    }

    /** Advance the simulation. tiltX/tiltY slide the resting bubble, -1..1. */
    public void step(long now,float tiltX,float tiltY){
        if(width<=0)return;
        if(lastStep<0){lastStep=now;return;}
        float dt=(now-lastStep)/1000f;lastStep=now;
        if(dt<=0)return;
        if(dt>.05f)dt=.05f; // A hitch never teleports the fluid.
        simTime+=dt;
        float minSide=Math.min(width,height);
        int N=0;
        for(int s=0;s<TOUCHES;s++)if(pointerId[s]>=0)N++;
        if(N!=lastN){ // Split/merge event: the surface flashes.
            for(int i=0;i<DROPS;i++)if(share[i]>.05f)energy[i]=Math.min(1f,energy[i]+.45f);
            lastN=N;
        }
        // Resting spot: panel centre, sliding with device tilt, barely wandering.
        float cx=clamp(width*.5f+clamp(tiltX,-1,1)*width*.30f,baseRadius()*.5f,width-baseRadius()*.5f);
        float cy=clamp(height*.5f+clamp(tiltY,-1,1)*height*.30f,baseRadius()*.5f,height-baseRadius()*.5f);
        cx+=(float)Math.sin(simTime*.5f)*minSide*.012f;
        cy+=(float)Math.cos(simTime*.41f)*minSide*.012f;
        float ease=(float)Math.exp(-SHARE_RATE*dt);
        float rEase=(float)Math.exp(-RADIUS_RATE*dt);
        float damp=(float)Math.exp(-DAMP*dt*.6f);
        float maxV=minSide*6f;
        for(int i=0;i<DROPS;i++){
            boolean active=pointerId[i]>=0;
            float targetShare;
            float tx,ty;
            if(N==0){
                if(i==0){targetShare=1f;tx=cx;ty=cy;}
                else{targetShare=0f;tx=x[0];ty=y[0];} // Parts flow home into the bubble.
            }else if(active){
                targetShare=1f/N;tx=touchX[i];ty=touchY[i]; // Straight to the finger.
            }else{
                targetShare=0f;tx=nearestLiveX(i,N);ty=nearestLiveY(i,N);
            }
            share[i]+=(targetShare-share[i])*(1f-ease);
            // Shared breathing so equal parts stay visibly equal.
            float breathe=1f+.06f*(float)Math.sin(simTime*2f);
            float targetRad=baseRadius()*(float)Math.sqrt(Math.max(share[i],0f))*breathe;
            rad[i]+=(targetRad-rad[i])*(1f-rEase);
            float ax=(tx-x[i])*SPRING, ay=(ty-y[i])*SPRING;
            vx[i]+=ax*dt;vy[i]+=ay*dt;
            vx[i]*=damp;vy[i]*=damp;
            float v2=vx[i]*vx[i]+vy[i]*vy[i];
            if(v2>maxV*maxV){float scale=maxV/(float)Math.sqrt(v2);vx[i]*=scale;vy[i]*=scale;}
            x[i]+=vx[i]*dt;y[i]+=vy[i]*dt;
            float m=Math.max(rad[i]*.3f,minSide*.03f);
            if(x[i]<m){x[i]=m;vx[i]=Math.abs(vx[i])*.4f;}
            else if(x[i]>width-m){x[i]=width-m;vx[i]=-Math.abs(vx[i])*.4f;}
            if(y[i]<m){y[i]=m;vy[i]=Math.abs(vy[i])*.4f;}
            else if(y[i]>height-m){y[i]=height-m;vy[i]=-Math.abs(vy[i])*.4f;}
            energy[i]*=Math.exp(-dt*1.8f);
            energy[i]=Math.min(1f,energy[i]+Math.min(1f,(float)Math.sqrt(v2)/(minSide*4f))*dt*2f);
        }
        for(int s=0;s<TOUCHES;s++){
            if(pointerId[s]<0){
                pressure[s]*=Math.exp(-dt*3.0f);
                if(pressure[s]<.01f)pressure[s]=0;
            }
            float decay=(float)Math.exp(-dt*4.0f);velX[s]*=decay;velY[s]*=decay;
        }
    }
    private float nearestLiveX(int self,int N){
        int best=-1;float bestD=Float.MAX_VALUE;
        for(int i=0;i<DROPS;i++){
            if(i==self)continue;
            boolean live=N==0?i==0:pointerId[i]>=0;
            if(!live)continue;
            float d=(x[i]-x[self])*(x[i]-x[self])+(y[i]-y[self])*(y[i]-y[self]);
            if(d<bestD){bestD=d;best=i;}
        }
        return best>=0?x[best]:width*.5f;
    }
    private float nearestLiveY(int self,int N){
        int best=-1;float bestD=Float.MAX_VALUE;
        for(int i=0;i<DROPS;i++){
            if(i==self)continue;
            boolean live=N==0?i==0:pointerId[i]>=0;
            if(!live)continue;
            float d=(x[i]-x[self])*(x[i]-x[self])+(y[i]-y[self])*(y[i]-y[self]);
            if(d<bestD){bestD=d;best=i;}
        }
        return best>=0?y[best]:height*.5f;
    }
    private static float clamp(float v,float lo,float hi){return v<lo?lo:(v>hi?hi:v);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}

    /** Drop uniforms: x, y, animated radius, energy. Unused slots are parked. */
    public void fillBlobs(float[] out){
        for(int i=0;i<COUNT;i++){
            int u=i*4;
            if(i<DROPS&&share[i]>.004f){
                out[u]=x[i];out[u+1]=y[i];out[u+2]=Math.max(rad[i],1f);out[u+3]=energy[i];
            }else{
                out[u]=0;out[u+1]=-9999;out[u+2]=0;out[u+3]=0;
            }
        }
    }
    /** Touch uniforms: x, y, fading pressure, normalized finger speed. */
    public void fillTouches(float[] out){
        float speedScale=Math.max(1f,(width+height)*2f);
        for(int s=0;s<TOUCHES;s++){
            int u=s*4;
            out[u]=touchX[s];out[u+1]=touchY[s];out[u+2]=pressure[s];
            out[u+3]=Math.min(1f,(float)Math.hypot(velX[s],velY[s])/speedScale);
        }
    }
    /** Drop velocity uniforms: vx, vy and normalized speed for motion stretch. */
    public void fillTouchVel(float[] out){
        float speedScale=Math.max(1f,(width+height)*2f);
        for(int s=0;s<TOUCHES;s++){
            int u=s*4;
            if(s<DROPS){
                out[u]=vx[s];out[u+1]=vy[s];
                out[u+2]=Math.min(1f,(float)Math.hypot(vx[s],vy[s])/speedScale);
            }else{
                out[u]=0;out[u+1]=0;out[u+2]=0;
            }
            out[u+3]=0f;
        }
    }
}
