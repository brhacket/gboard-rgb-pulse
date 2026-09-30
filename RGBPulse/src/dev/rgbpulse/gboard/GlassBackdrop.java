package dev.rgbpulse.gboard;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.List;

/** Records ONLY a supplied background Drawable + our field, never View.draw/children/editor. */
final class GlassBackdrop {
    private final RenderNode source=new RenderNode("RGB backdrop"),lens=new RenderNode("RGB diffused glass");
    private final float[] boxes=new float[64*4];
    private RuntimeShader sharpShader,blurredShader;
    private RenderEffect blur;
    private float lastRadius=-1;
    private String issue;
    String issue(){return issue;}
    boolean draw(Canvas out,View panel,Fx fx,RectF play,List<RectF> keys,float dp,long now){
        if(issue!=null||!out.isHardwareAccelerated()||keys.size()>64)return false;
        int w=Math.round(play.width()),h=Math.round(play.height());if(w<1||h<1)return false;
        CapHooks.drawing.set(true);
        try {
            if(sharpShader==null){sharpShader=new RuntimeShader(PanelGlassCode.SOURCE);blurredShader=new RuntimeShader(PanelGlassCode.SOURCE);}
            source.setPosition(0,0,w,h);source.setClipToBounds(true);
            Canvas c=source.beginRecording(w,h);
            try {
                Drawable bg=panel.getBackground();
                if(bg!=null){
                    int save=c.save();c.translate(-play.left,-play.top);
                    // Draw under the same coordinates used by View's background draw path.
                    c.translate(panel.getScrollX(),panel.getScrollY());bg.draw(c);c.restoreToCount(save);
                }
                c.translate(-play.left,-play.top);fx.drawFields(c,play,now);
            }finally{source.endRecording();}
            float frost=fx.cfg.frost/100f;
            float radius=frost>0?dp*(.6f+11f*frost):0;
            if(lastRadius!=radius){
                blur=radius>0?RenderEffect.createBlurEffect(radius,radius,Shader.TileMode.CLAMP):null;
                lastRadius=radius;
            }
            java.util.Arrays.fill(boxes,0);
            for(int i=0;i<keys.size();i++){
                RectF b=keys.get(i);int at=i*4;
                boxes[at]=b.left-play.left+2*dp;boxes[at+1]=b.top-play.top+3*dp;
                boxes[at+2]=b.right-play.left-2*dp;boxes[at+3]=b.bottom-play.top-3*dp;
            }
            uniforms(sharpShader,w,h,dp,fx,keys.size(),0);
            uniforms(blurredShader,w,h,dp,fx,keys.size(),1);
            RenderEffect sharp=RenderEffect.createRuntimeShaderEffect(sharpShader,"backdrop");
            RenderEffect effect=sharp;
            if(blur!=null){
                // Outer shader samples the blurred input. No atlas or off-output reads.
                RenderEffect soft=RenderEffect.createChainEffect(
                    RenderEffect.createRuntimeShaderEffect(blurredShader,"backdrop"),blur);
                effect=RenderEffect.createBlendModeEffect(sharp,soft,BlendMode.PLUS);
            }
            lens.setPosition(0,0,w,h);lens.setClipToBounds(true);
            Canvas lc=lens.beginRecording(w,h);try{lc.drawRenderNode(source);}finally{lens.endRecording();}
            lens.setRenderEffect(effect);
            int save=out.save();try{out.clipRect(play);out.translate(play.left,play.top);out.drawRenderNode(lens);}finally{out.restoreToCount(save);}
            return true;
        }catch(RuntimeException e){issue=e.toString();return false;}
        finally{CapHooks.drawing.remove();}
    }
    private void uniforms(RuntimeShader shader,int w,int h,float dp,Fx fx,int count,float pass){
        shader.setFloatUniform("resolution",(float)w,(float)h);shader.setFloatUniform("density",dp);
        shader.setFloatUniform("lensAmount",fx.cfg.lens/100f);shader.setFloatUniform("textureAmount",fx.cfg.texture/100f);
        shader.setFloatUniform("frostAmount",fx.cfg.frost/100f);shader.setFloatUniform("roundness",fx.cfg.roundness/100f);
        shader.setFloatUniform("metalAmount",fx.cfg.metal/100f);
        shader.setFloatUniform("blurredPass",pass);shader.setFloatUniform("keyCount",(float)count);shader.setFloatUniform("keys",boxes);
    }
    void release(){source.discardDisplayList();lens.discardDisplayList();lens.setRenderEffect(null);blur=null;lastRadius=-1;issue=null;}
}
