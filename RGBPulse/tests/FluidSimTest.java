package dev.rgbpulse.gboard;

public final class FluidSimTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        FluidSim sim=new FluidSim();
        sim.bounds(1080,640,0);
        check(sim.width==1080&&sim.height==640);
        for(int i=0;i<FluidSim.COUNT;i++){
            check(sim.x[i]>0&&sim.x[i]<1080&&sim.y[i]>0&&sim.y[i]<640,"seeded blob inside panel");
            check(sim.baseR[i]>0&&sim.baseR[i]<640*.2f,"plausible blob radius");
            check(sim.phase[i]>=0&&sim.phase[i]<=1);
        }
        // Rescaling keeps blobs inside and never jumps time.
        sim.bounds(900,500,10);
        for(int i=0;i<FluidSim.COUNT;i++)check(sim.x[i]>0&&sim.x[i]<900&&sim.y[i]>0&&sim.y[i]<500);
        sim.bounds(1080,640,20);
        // Touches and tilt over five simulated seconds never push a blob outside.
        for(int frame=0;frame<300;frame++){
            long now=20+frame*16;
            sim.pointer(1,1080*(frame%97)/96f,640*(frame%53)/52f,frame<220,now);
            sim.step(now,(frame%41)/20f-1f,(frame%29)/14f-1f);
        }
        for(int i=0;i<FluidSim.COUNT;i++)
            check(sim.x[i]>=0&&sim.x[i]<=1080&&sim.y[i]>=0&&sim.y[i]<=640,"blob stays inside while playing");
        // Tilt pulls the whole field sideways.
        FluidSim left=new FluidSim();left.bounds(1080,640,0);left.step(16,0,0);
        FluidSim right=new FluidSim();right.bounds(1080,640,0);right.step(16,0,0);
        for(int frame=0;frame<60;frame++){long now=16+frame*16;left.step(now,-1,0);right.step(now,1,0);}
        check(mean(right.x)>mean(left.x)+20,"tilt moves the fluid toward the lower edge");
        // A held finger magnetically pulls the nearest blob closer.
        FluidSim magnet=new FluidSim();magnet.bounds(1080,640,0);magnet.step(16,0,0);
        int nearest=0;float best=Float.MAX_VALUE;
        float targetX=640,targetY=320;
        for(int i=0;i<FluidSim.COUNT;i++){
            float d=(float)Math.hypot(magnet.x[i]-targetX,magnet.y[i]-targetY);
            if(d<best){best=d;nearest=i;}
        }
        float before=best;
        for(int frame=0;frame<90;frame++){long now=16+frame*16;magnet.pointer(7,targetX,targetY,true,now);magnet.step(now,0,0);}
        float after=(float)Math.hypot(magnet.x[nearest]-targetX,magnet.y[nearest]-targetY);
        check(after<before||after<40,"finger attracts the nearest blob");
        // A tap splashes the closest blob away from the touch.
        FluidSim splash=new FluidSim();splash.bounds(1080,640,0);splash.step(16,0,0);
        int close=0;float closeD=Float.MAX_VALUE;
        for(int i=0;i<FluidSim.COUNT;i++){
            float d=(float)Math.hypot(splash.x[i]-540,splash.y[i]-320);
            if(d<closeD){closeD=d;close=i;}
        }
        float splashBefore=(float)Math.hypot(splash.x[close]-540,splash.y[close]-320);
        splash.impulse(540,320,32);
        for(int frame=0;frame<6;frame++)splash.step(32+frame*16,0,0);
        float splashAfter=(float)Math.hypot(splash.x[close]-540,splash.y[close]-320);
        check(splashAfter>splashBefore,"tap pushes the nearest blob away");
        check(splash.energy[close]>0,"tap charges the nearest blob");
        // Releasing a finger fades its pressure to zero. A fresh sim assigns the
        // first pointer to slot 0; pressure is indexed by slot, not pointer id.
        FluidSim fade=new FluidSim();fade.bounds(1080,640,0);fade.step(16,0,0);
        fade.pointer(3,500,300,true,32);fade.step(48,0,0);
        check(fade.pressure[0]>0);
        fade.pointer(3,500,300,false,64);
        for(int frame=0;frame<150;frame++)fade.step(64+frame*16,0,0);
        check(fade.pressure[0]==0,"released touch fades out");
        // A moving finger reports a nonzero normalized speed for shader streaks.
        FluidSim speed=new FluidSim();speed.bounds(1080,640,0);speed.step(16,0,0);
        for(int frame=0;frame<30;frame++){long now=16+frame*16;speed.pointer(9,200+frame*18,320,true,now);speed.step(now,0,0);}
        float[] touches=new float[FluidSim.TOUCHES*4];speed.fillTouches(touches);
        check(touches[2]>0&&touches[3]>0,"moving touch carries pressure and speed");
        // Uniform arrays are finite and radii stay positive.
        float[] blobs=new float[FluidSim.COUNT*4];speed.fillBlobs(blobs);
        for(float value:blobs)check(Float.isFinite(value),"finite uniform");
        for(int i=0;i<FluidSim.COUNT;i++)check(blobs[i*4+2]>0,"animated radius positive");
        // A long hitch never integrates a giant jump.
        FluidSim hitch=new FluidSim();hitch.bounds(1080,640,0);hitch.step(16,0,0);
        hitch.step(60_000,1,1);
        for(int i=0;i<FluidSim.COUNT;i++)check(hitch.x[i]>=0&&hitch.x[i]<=1080&&hitch.y[i]>=0&&hitch.y[i]<=640);
        // clear() releases touches and drains energy.
        splash.clear();
        for(int i=0;i<FluidSim.COUNT;i++)check(splash.energy[i]==0);
        for(int id:splash.pointerId)check(id<0);
        System.out.println("PASS: magnetic fluid physics stays bounded, follows tilt, fingers and taps");
    }
    static float mean(float[] values){float sum=0;for(float v:values)sum+=v;return sum/values.length;}
}
