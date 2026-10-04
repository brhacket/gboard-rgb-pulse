package dev.rgbpulse.gboard;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;

/** Android 13+ renderer for the magnetic fluid. One shader; the CPU sim drives it. */
final class FluidFx {
    private final FluidSim sim=new FluidSim();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] blobUniforms=new float[FluidSim.COUNT*4];
    private final float[] touchUniforms=new float[FluidSim.TOUCHES*4];
    private RuntimeShader shader;
    private String error;
    private long epoch=-1;

    String issue(){return error;}
    void pointer(int id,float x,float y,boolean down,long now){sim.pointer(id,x,y,down,now);}
    void impulse(float x,float y,long now){sim.impulse(x,y,now);}
    void cancelTouches(){sim.cancelTouches();}
    void clear(){sim.clear();}

    void draw(Canvas canvas,RectF play,Config cfg,long now,float tiltX,float tiltY){
        float w=play.width(),h=play.height();
        if(w<2||h<2)return;
        if(error!=null)return;
        try{
            if(shader==null)shader=new RuntimeShader(ShaderCode.FLUID);
        }catch(RuntimeException e){error=e.getClass().getSimpleName()+": "+e.getMessage();clear();return;}
        if(epoch<0)epoch=now;
        sim.bounds(w,h,now);
        sim.step(now,tiltX,tiltY);
        sim.fillBlobs(blobUniforms);
        sim.fillTouches(touchUniforms);
        shader.setFloatUniform("resolution",w,h);
        shader.setFloatUniform("time",(now-epoch)/1000f);
        shader.setFloatUniform("strength",cfg.fluidStrength/100f);
        shader.setFloatUniform("hue",cfg.hue1/360f);
        shader.setFloatUniform("hue2",cfg.hue2/360f);
        shader.setFloatUniform("saturation",cfg.sat/100f);
        shader.setFloatUniform("tiltX",tiltX);
        shader.setFloatUniform("tiltY",tiltY);
        shader.setFloatUniform("ripple",1f);
        shader.setFloatUniform("glow",1f);
        shader.setFloatUniform("detail",1f);
        shader.setFloatUniform("blobs",blobUniforms);
        shader.setFloatUniform("touches",touchUniforms);
        paint.setShader(shader);
        int save=canvas.save();
        try{
            canvas.translate(play.left,play.top);
            canvas.drawRect(0,0,w,h,paint);
        }finally{canvas.restoreToCount(save);paint.setShader(null);}
    }
}
