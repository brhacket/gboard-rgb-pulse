package dev.rgbpulse.gboard;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Android 13+ GPU path. Only instantiated when Build.VERSION.SDK_INT >= 33. */
final class FieldFx {
    private final LinkedHashMap<Integer,RuntimeShader> shaders=new LinkedHashMap<Integer,RuntimeShader>(8,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer,RuntimeShader> eldest){return size()>6;}
    };
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] xs=new float[4], ys=new float[4], hues=new float[4];
    private final long[] starts={-1,-1,-1,-1};
    private final int[] styles=new int[4];
    private final float[] uniforms=new float[16],keyUniforms=new float[16];
    private final RectF[] keyBounds={new RectF(),new RectF(),new RectF(),new RectF()};
    private long epoch=-1;
    private int next;
    private String error;
    String issue(){return error;}
    void clear(){for(int i=0;i<4;i++)starts[i]=-1;}
    boolean active(long now,int duration){for(long start:starts)if(start>=0 && now-start<duration)return true;return false;}
    void tap(int style,float x,float y,RectF play,RectF key,float hue,long now) {
        if(epoch<0)epoch=now;
        if(style>=8)clear(); // Wide pulses replace one another instead of stacking.
        keyBounds[next].set((key.left-play.left)/Math.max(1,play.width()),(key.top-play.top)/Math.max(1,play.height()),(key.right-play.left)/Math.max(1,play.width()),(key.bottom-play.top)/Math.max(1,play.height()));
        xs[next]=(x-play.left)/Math.max(1,play.width());
        ys[next]=(y-play.top)/Math.max(1,play.height());
        hues[next]=hue/360f;starts[next]=now;styles[next]=style;
        next=(next+1)%3;
    }
    boolean draw(Canvas canvas,RectF play,Config cfg,long now) {
        if(error!=null)return false;
        try {
            int save=canvas.save();
            try {
                canvas.clipRect(play);canvas.translate(play.left,play.top);
                for(int style=0;style<Config.GPU_COUNT;style++) {
                    boolean any=false;
                    for(int i=0;i<4;i++) {
                        int u=i*4;
                        uniforms[u]=xs[i]*play.width();uniforms[u+1]=ys[i]*play.height();uniforms[u+3]=hues[i];
                        float age=starts[i]<0 ? -1f : (now-starts[i])/(float)cfg.pulseDuration;
                        uniforms[u+2]=styles[i]==style && age<1 ? age : -1f;
                        any|=uniforms[u+2]>=0;
                        RectF box=keyBounds[i];keyUniforms[u]=box.left*play.width();keyUniforms[u+1]=box.top*play.height();keyUniforms[u+2]=box.right*play.width();keyUniforms[u+3]=box.bottom*play.height();
                    }
                    if(!any)continue;
                    RuntimeShader shader=shaders.get(style);
                    if(shader==null){
                        // Specialize each program so other styles are dead-code eliminated.
                        String source=ShaderCode.FIELD.replace("uniform float style;","const float style="+style+".0;");
                        shader=new RuntimeShader(source);shaders.put(style,shader);
                    }
                    shader.setFloatUniform("resolution",play.width(),play.height());
                    shader.setFloatUniform("time",(now-epoch)/1000f);
                    shader.setFloatUniform("strength",(cfg.refined?cfg.backgroundOpacity:cfg.opacity)/100f);
                    shader.setFloatUniform("effectSize",cfg.size/100f);
                    shader.setFloatUniform("thickness",cfg.thickness/100f);
                    shader.setFloatUniform("colorMode",(float)cfg.colorMode);
                    shader.setFloatUniform("hue1",cfg.hue1/360f);shader.setFloatUniform("hue2",cfg.hue2/360f);
                    shader.setFloatUniform("saturation",cfg.sat/100f);
                    shader.setFloatUniform("taps",uniforms);
                    shader.setFloatUniform("keyBounds",keyUniforms);
                    paint.setShader(shader);
                    canvas.drawRect(0,0,play.width(),play.height(),paint);
                }
            } finally {canvas.restoreToCount(save);paint.setShader(null);}
            return true;
        } catch(RuntimeException e) {
            error=e.getClass().getSimpleName()+": "+e.getMessage();clear();return false;
        }
    }
}
