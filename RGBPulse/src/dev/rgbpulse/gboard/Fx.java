package dev.rgbpulse.gboard;

import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.Random;

/** Tap-only procedural waves. No sprites, particles, software bitmap or classic fallback. */
public final class Fx {
    public Config cfg = new Config();
    final GlideTrail glide=new GlideTrail();
    final SideSweep side=new SideSweep();
    private final Random rnd = new Random();
    private FieldFx fields;
    private FluidFx fluid;
    private boolean fluidFailed;
    private final QuietBackground quiet;
    private String issue;
    private boolean attempted;
    public Fx(float density) {quiet=new QuietBackground(density);}
    private void init() {
        if(attempted)return;
        attempted=true;
        try {fields=new FieldFx();}catch(LinkageError|RuntimeException e){issue=e.toString();}
    }
    private FluidFx fluid(){
        if(fluid==null&&!fluidFailed){
            try{fluid=new FluidFx();}catch(LinkageError|RuntimeException e){fluidFailed=true;if(issue==null)issue=e.toString();}
        }
        return fluid;
    }
    public String shaderIssue(){
        if(issue!=null)return issue;
        if(fields!=null&&fields.issue()!=null)return fields.issue();
        return fluid!=null?fluid.issue():null;
    }
    public boolean active(long now){return cfg.enabled&&(cfg.fluid||(cfg.quietBackground&&quiet.active(now))||(cfg.tapEffects&&fields!=null&&fields.active(now,cfg.pulseDuration))||(cfg.trailStyle>0&&glide.points.active(now,cfg.trailLife)));}
    public void tap(float x,float y,RectF play,RectF key,long now) {
        if(!cfg.enabled)return;
        if(cfg.quietBackground)quiet.tap(key,now);
        if(!cfg.tapEffects)return;
        init();
        if(fields==null||fields.issue()!=null)return; // Never resurrect a removed effect as fallback.
        int style=cfg.effect;
        float hue;
        if(cfg.colorMode==1)hue=rnd.nextFloat()*360;
        else if(cfg.colorMode==4)hue=(x-play.left)/Math.max(1,play.width())*360;
        else hue=(now%4000L)*.09f;
        fields.tap(style,x,y,play,key,hue,now);
    }
    /** One pointer event for the magnetic fluid, in panel coordinates. */
    public void fluidPointer(int id,float x,float y,boolean down,long now){
        if(!cfg.enabled||!cfg.fluid)return;
        FluidFx fx=fluid();
        if(fx!=null)fx.pointer(id,x,y,down,now);
    }
    /** A tap splash: the fluid bursts away from the pressed point. */
    public void fluidImpulse(float x,float y,long now){
        if(!cfg.enabled||!cfg.fluid)return;
        FluidFx fx=fluid();
        if(fx!=null)fx.impulse(x,y,now);
    }
    public void fluidCancel(){if(fluid!=null)fluid.cancelTouches();}
    final java.util.ArrayList<RectF> lensBoxes=new java.util.ArrayList<RectF>();
    public void lenses(java.util.List<RectF> boxes,float dp){lensBoxes.clear();lensBoxes.addAll(boxes);}
    public void drawFields(Canvas canvas,RectF play,long now){
        if(cfg.enabled&&cfg.fluid){FluidFx fx=fluid();if(fx!=null&&fx.issue()==null)fx.draw(canvas,play,cfg,now,FluidMotion.tiltX(),FluidMotion.tiltY());}
        if(cfg.enabled&&cfg.quietBackground)quiet.draw(canvas,play,cfg,now);
        if(cfg.enabled&&cfg.tapEffects&&fields!=null&&fields.active(now,cfg.pulseDuration))fields.draw(canvas,play,cfg,now);
    }
    public void clear(){quiet.clear();glide.points.clear();side.clear();if(fields!=null)fields.clear();if(fluid!=null)fluid.clear();}
    public void dispose(){clear();fields=null;fluid=null;fluidFailed=false;attempted=false;issue=null;}
}
