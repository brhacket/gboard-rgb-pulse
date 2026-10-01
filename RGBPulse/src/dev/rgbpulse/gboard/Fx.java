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
    private String issue;
    private boolean attempted;
    public Fx(float ignoredDensity) { }
    private void init() {
        if(attempted)return;
        attempted=true;
        try {fields=new FieldFx();}catch(LinkageError|RuntimeException e){issue=e.toString();}
    }
    public String shaderIssue(){return issue!=null?issue:fields==null?null:fields.issue();}
    public boolean active(long now){return cfg.enabled&&((fields!=null&&fields.active(now,cfg.duration))||(cfg.trailStyle>0&&glide.points.active(now,cfg.trailLife)));}
    public void tap(float x,float y,RectF play,long now) {
        if(!cfg.enabled || !cfg.tapEffects)return;
        init();
        if(fields==null||fields.issue()!=null)return; // Never resurrect a removed effect as fallback.
        int style=cfg.effect;
        if(style==Config.LIQUID_SHUFFLE)style=1+rnd.nextInt(Config.GPU_COUNT-1);
        float hue;
        if(cfg.colorMode==1)hue=rnd.nextFloat()*360;
        else if(cfg.colorMode==4)hue=(x-play.left)/Math.max(1,play.width())*360;
        else hue=(now%4000L)*.09f;
        fields.tap(style,x,y,play,hue,now);
    }
    final java.util.ArrayList<RectF> lensBoxes=new java.util.ArrayList<RectF>();
    public void lenses(java.util.List<RectF> boxes,float dp){lensBoxes.clear();lensBoxes.addAll(boxes);}
    public void drawFields(Canvas canvas,RectF play,long now){if(cfg.enabled&&fields!=null&&fields.active(now,cfg.duration))fields.draw(canvas,play,cfg,now);}
    public void clear(){glide.points.clear();side.clear();if(fields!=null)fields.clear();}
    public void dispose(){clear();fields=null;attempted=false;issue=null;}
}
