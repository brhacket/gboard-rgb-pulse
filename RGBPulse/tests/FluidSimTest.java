package dev.rgbpulse.gboard;

public final class FluidSimTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        // At rest the fluid is exactly one bubble near the panel centre.
        FluidSim sim=new FluidSim();
        sim.bounds(1080,640,0);
        for(int frame=0;frame<60;frame++)sim.step(16+frame*16,0,0);
        check(sim.share[0]>.9f,"one bubble owns all the mass at rest");
        for(int i=1;i<FluidSim.DROPS;i++)check(sim.share[i]<.1f,"no stray bubbles at rest");
        check(Math.abs(sim.x[0]-540)<60&&Math.abs(sim.y[0]-320)<60,"resting bubble near centre");
        check(sim.rad[0]>0&&sim.rad[0]<640*.5f,"plausible resting radius");
        // One touch: the whole bubble darts straight to the finger.
        float targetX=880,targetY=320;
        for(int frame=0;frame<90;frame++){
            long now=1000+frame*16;
            sim.pointer(7,targetX,targetY,true,now);
            sim.step(now,0,0);
        }
        check(Math.hypot(sim.x[0]-targetX,sim.y[0]-targetY)<40,"bubble goes directly to the touch");
        check(sim.share[0]>.9f,"single touch keeps all mass in one bubble");
        // Two touches: the bubble splits into two equal parts.
        FluidSim split=new FluidSim();split.bounds(1080,640,0);
        for(int frame=0;frame<30;frame++)split.step(16+frame*16,0,0);
        for(int frame=0;frame<120;frame++){
            long now=1000+frame*16;
            split.pointer(1,240,320,true,now);
            split.pointer(2,840,320,true,now);
            split.step(now,0,0);
        }
        check(Math.abs(split.share[0]-.5f)<.08f&&Math.abs(split.share[1]-.5f)<.08f,"two touches split the fluid in half");
        check(Math.abs(split.rad[0]-split.rad[1])<Math.max(1,split.rad[0])*.08f,"equal parts have equal radii");
        check(Math.hypot(split.x[0]-240,split.y[0]-320)<40&&Math.hypot(split.x[1]-840,split.y[1]-320)<40,"parts sit on their fingers");
        check(split.rad[0]<split.baseRadius()*.8f,"split parts are smaller than the whole");
        // Release one finger: its part flows back into the other.
        split.pointer(2,840,320,false,2960);
        for(int frame=0;frame<150;frame++)split.step(2960+frame*16,0,0);
        check(split.share[0]>.9f,"released part merges back into one bubble");
        check(split.share[1]<.1f,"released part is fully absorbed");
        // Three touches: three equal thirds.
        FluidSim third=new FluidSim();third.bounds(1080,640,0);
        for(int frame=0;frame<30;frame++)third.step(16+frame*16,0,0);
        for(int frame=0;frame<150;frame++){
            long now=1000+frame*16;
            third.pointer(1,240,200,true,now);
            third.pointer(2,540,450,true,now);
            third.pointer(3,840,200,true,now);
            third.step(now,0,0);
        }
        for(int i=0;i<3;i++)check(Math.abs(third.share[i]-1f/3f)<.08f,"three touches split into equal thirds");
        // Tilt slides the resting bubble toward the lower edge.
        FluidSim left=new FluidSim();left.bounds(1080,640,0);left.step(16,0,0);
        FluidSim right=new FluidSim();right.bounds(1080,640,0);right.step(16,0,0);
        for(int frame=0;frame<90;frame++){long now=16+frame*16;left.step(now,-1,0);right.step(now,1,0);}
        check(right.x[0]>left.x[0]+100,"tilt slides the bubble toward the tipped edge");
        // A touchdown flashes the answering drop.
        FluidSim flash=new FluidSim();flash.bounds(1080,640,0);flash.step(16,0,0);
        flash.pointer(4,540,320,true,32);flash.impulse(540,320,32);
        check(flash.energy[0]>.5f,"touchdown charges the drop");
        // Touches, tilt and hitches never push the fluid outside the panel.
        FluidSim play=new FluidSim();play.bounds(1080,640,0);
        for(int frame=0;frame<400;frame++){
            long now=100+frame*16;
            play.pointer(1,1080*(frame%97)/96f,640*(frame%53)/52f,frame<300,now);
            play.step(now,(frame%41)/20f-1f,(frame%29)/14f-1f);
        }
        for(int i=0;i<FluidSim.DROPS;i++)
            check(play.x[i]>=0&&play.x[i]<=1080&&play.y[i]>=0&&play.y[i]<=640,"drops stay inside while playing");
        play.step(60_000,1,1); // A long hitch never integrates a giant jump.
        for(int i=0;i<FluidSim.DROPS;i++)check(play.x[i]>=0&&play.x[i]<=1080&&play.y[i]>=0&&play.y[i]<=640);
        // Released finger pressure fades; uniforms stay finite.
        FluidSim fade=new FluidSim();fade.bounds(1080,640,0);fade.step(16,0,0);
        fade.pointer(3,500,300,true,32);fade.step(48,0,0);
        check(fade.pressure[0]>0);
        fade.pointer(3,500,300,false,64);
        for(int frame=0;frame<150;frame++)fade.step(64+frame*16,0,0);
        check(fade.pressure[0]==0,"released touch fades out");
        float[] blobs=new float[FluidSim.COUNT*4],touches=new float[FluidSim.TOUCHES*4],vels=new float[FluidSim.TOUCHES*4];
        fade.fillBlobs(blobs);fade.fillTouches(touches);fade.fillTouchVel(vels);
        for(float value:blobs)check(Float.isFinite(value),"finite blob uniform");
        for(float value:vels)check(Float.isFinite(value),"finite velocity uniform");
        check(blobs[2]>0,"resting bubble has a radius in the uniforms");
        check(blobs[6]<=0,"parked slots carry no second bubble");
        // clear() releases touches and drains energy.
        flash.clear();
        for(int i=0;i<FluidSim.DROPS;i++)check(flash.energy[i]==0);
        for(int id:flash.pointerId)check(id<0);
        System.out.println("PASS: one bubble at rest, darts to one touch, splits into equal parts, merges back");
    }
}
