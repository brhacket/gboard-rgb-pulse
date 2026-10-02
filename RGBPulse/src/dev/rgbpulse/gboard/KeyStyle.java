package dev.rgbpulse.gboard;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.*;
import java.lang.reflect.Method;
import de.robv.android.xposed.*;

final class KeyStyle {
    static final ThreadLocal<Integer> textDepth=new ThreadLocal<Integer>();
    static final ThreadLocal<Boolean> ownWrite=new ThreadLocal<Boolean>();
    static final String TAG="rgbpulse.glass.key";
    static final ThreadLocal<Canvas> scopedCanvas=new ThreadLocal<Canvas>();
    static final Map<Paint,Boolean> copies=Collections.synchronizedMap(new WeakHashMap<Paint,Boolean>());
    static final ThreadLocal<Integer> scope=new ThreadLocal<Integer>();
    static final Set<Method> hooked=new HashSet<Method>();
    static Config cfg=new Config();
    static Typeface face=Typeface.DEFAULT;
    final CutoutTiles cutouts=new CutoutTiles();
    final CapHooks caps=new CapHooks();
    final IdentityHashMap<View,Drawable> originals=new IdentityHashMap<View,Drawable>();
    final IdentityHashMap<View,RippleOverlay> overlays=new IdentityHashMap<View,RippleOverlay>();
    static void configure(Config c,android.content.Context context) {cfg=c;Typography.configure(c,context);face=Typography.face;}
    static boolean markedTree(View v){
        if(!cfg.enabled||!cfg.glass||cfg.refined)return false;
        for(int i=0;i<12 && v!=null;i++){
            if(XposedHelpers.getAdditionalInstanceField(v,TAG)!=null)return true;
            if(PulseModule.keyboardClass(v))return false;
            v=v.getParent() instanceof View?(View)v.getParent():null;
        }return false;
    }
    static void installTextHooks() {
        CapHooks.install();
        XposedHelpers.findAndHookMethod(View.class,"setBackgroundDrawable",Drawable.class,new XC_MethodHook(){
            protected void beforeHookedMethod(MethodHookParam p){
                if(Boolean.TRUE.equals(ownWrite.get()))return;
                Object owner=XposedHelpers.getAdditionalInstanceField(p.thisObject,TAG);
                if(!(owner instanceof KeyStyle)||!cfg.enabled||!cfg.glass||p.args[0] instanceof Glass)return;
                View v=(View)p.thisObject;KeyStyle style=(KeyStyle)owner;
                style.originals.put(v,(Drawable)p.args[0]);
                p.args[0]=new Glass((View)p.thisObject, v.getResources().getDisplayMetrics().density, (Drawable)p.args[0]);
            }
        });
        try {
            Method record=View.class.getDeclaredMethod("updateDisplayListIfDirty");
            XposedBridge.hookMethod(record,new XC_MethodHook(){
                protected void beforeHookedMethod(MethodHookParam p){
                    if(!markedTree((View)p.thisObject))return;
                    Integer n=scope.get();scope.set(n==null?1:n+1);p.setObjectExtra(TAG,true);
                }
                protected void afterHookedMethod(MethodHookParam p){
                    if(p.getObjectExtra(TAG)!=null){Integer n=scope.get();if(n==null||n<=1)scope.remove();else scope.set(n-1);}
                }
            });
        }catch(Throwable e){XposedBridge.log("RGBPulse 33: display-list scope unavailable");}
        Set<Method> methods=new HashSet<Method>();
        for(String name:new String[]{"android.graphics.Canvas","android.graphics.BaseCanvas","android.graphics.BaseRecordingCanvas","android.graphics.RecordingCanvas"}) {
            try { for(Method m:Class.forName(name).getDeclaredMethods()) {
                if(!(m.getName().equals("drawText")||m.getName().equals("drawTextRun")))continue;
                Class<?>[] args=m.getParameterTypes();
                if(args.length==0 || args[args.length-1]!=Paint.class || !methods.add(m))continue;
                XposedBridge.hookMethod(m,new XC_MethodHook(){
                    protected void beforeHookedMethod(MethodHookParam p){
                        Integer depth=scope.get();if(depth==null||depth==0||!cfg.enabled||!cfg.glass||cfg.refined)return;
                        Integer nested=textDepth.get();textDepth.set(nested==null?1:nested+1);p.setObjectExtra("rgbText",true);
                        if(nested!=null&&nested>0)return;
                        try {
                            int i=p.args.length-1;Paint original=(Paint)p.args[i];
                            if(copies.containsKey(original)||Boolean.TRUE.equals(CapHooks.drawing.get()))return;
                            Object value=p.args[0];String text;int start=0,end;
                            if(value instanceof char[]){start=(Integer)p.args[1];int n=(Integer)p.args[2];
                                if(n>24||n<1)return;text=new String((char[])value,start,n);
                            }else if(value instanceof CharSequence){CharSequence chars=(CharSequence)value;end=chars.length();
                                if(p.args[1] instanceof Integer){start=(Integer)p.args[1];end=(Integer)p.args[2];}
                                if(end-start>24||end<=start)return;text=chars.subSequence(start,end).toString();
                            }else return;
                            if(!LegendPolicy.eligible(text))return;
                            if(p.method.getName().equals("drawTextRun")){
                                for(Object arg:p.args)if(Boolean.TRUE.equals(arg))return;
                                int cs=(Integer)p.args[3],ce=(Integer)p.args[4];String context;
                                if(value instanceof char[]){if(ce>24||ce<1)return;context=new String((char[])value,cs,ce);}
                                else {if(ce-cs>24||ce<=cs)return;context=((CharSequence)value).subSequence(cs,ce).toString();}
                                if(!LegendPolicy.eligible(context))return;
                            }
                            CapHooks.Entry hintEntry=CapHooks.active.get();
                            if(hintEntry!=null){Object o=XposedHelpers.getAdditionalInstanceField(hintEntry.key,TAG);if(o instanceof KeyStyle&&((KeyStyle)o).cutouts.hint(hintEntry,text,original)){p.setResult(null);return;}}
                            Paint paint=new Paint(original);
                            if(cfg.font!=0||cfg.bold)paint.setTypeface(face);
                            paint.setTextSize(original.getTextSize()*cfg.letterSize/100f);
                            for(int j=0;j<text.length();){int cp=text.codePointAt(j);j+=Character.charCount(cp);
                                if(cp!=' '&&!paint.hasGlyph(new String(Character.toChars(cp))))return;
                            }
                            int xi=-1;for(int j=1;j<p.args.length;j++)if(p.args[j] instanceof Float){xi=j;break;}
                            if(xi<0 || !(p.args[xi+1] instanceof Float))return;
                            float x=(Float)p.args[xi],y=(Float)p.args[xi+1];
                            Paint.FontMetrics oldFm=original.getFontMetrics(),newFm=paint.getFontMetrics();
                            float correction=(oldFm.ascent+oldFm.descent-newFm.ascent-newFm.descent)*.5f;
                            y+=Math.max(-original.getTextSize()*.16f,Math.min(original.getTextSize()*.16f,correction));
                            if(original.getTextAlign()!=Paint.Align.CENTER){
                                float dw=(original.measureText(text)-paint.measureText(text))*.5f;
                                dw=Math.max(-original.getTextSize()*.35f,Math.min(original.getTextSize()*.35f,dw));
                                x+=original.getTextAlign()==Paint.Align.LEFT?dw:-dw;
                            }
                            CapHooks.Entry entry=CapHooks.active.get();
                            if(entry!=null){
                                Object owner=XposedHelpers.getAdditionalInstanceField(entry.key,TAG);
                                if(owner instanceof KeyStyle && ((KeyStyle)owner).cutouts.draw(entry,text,paint,x,y,(Canvas)p.thisObject)){
                                    p.setResult(null);return;
                                }
                            }
                            return;
                        }catch(Throwable ignored){}
                    }
                    protected void afterHookedMethod(MethodHookParam p){
                        if(p.getObjectExtra("rgbText")!=null){Integer n=textDepth.get();if(n==null||n<=1)textDepth.remove();else textDepth.set(n-1);}
                    }
                });
            }}catch(Throwable ignored){}
        }
    }
    void apply(java.util.List<View> keys,Config c) {
        if(!c.enabled||!c.glass){restore();return;}
        if(c.refined){applyRefined(keys);return;}
        clearOverlays();
        for(Iterator<Map.Entry<View,Drawable>> it=originals.entrySet().iterator();it.hasNext();) {
            Map.Entry<View,Drawable> e=it.next();if(!keys.contains(e.getKey())){CapHooks.clear(e.getKey());XposedHelpers.removeAdditionalInstanceField(e.getKey(),TAG);if(e.getKey().getBackground() instanceof Glass)setBackground(e.getKey(),e.getValue());invalidateTree(e.getKey());it.remove();}
        }
        for(View key:keys){
            if(!originals.containsKey(key)) {
                originals.put(key,key.getBackground());
                XposedHelpers.setAdditionalInstanceField(key,TAG,this);
                try {
                    Method m=key.getClass().getMethod("draw",Canvas.class);
                    if(hooked.add(m))XposedBridge.hookMethod(m,new XC_MethodHook(){
                        protected void beforeHookedMethod(MethodHookParam p){
                            if(!cfg.enabled||!cfg.glass||XposedHelpers.getAdditionalInstanceField(p.thisObject,TAG)==null)return;
                            Integer n=scope.get();if(n==null||n==0)scopedCanvas.set((Canvas)p.args[0]);scope.set(n==null?1:n+1);p.setObjectExtra(TAG,true);
                        }
                        protected void afterHookedMethod(MethodHookParam p){
                            if(p.getObjectExtra(TAG)!=null){Integer n=scope.get();if(n==null||n<=1){scope.remove();scopedCanvas.remove();}else scope.set(n-1);}
                        }
                    });
                }catch(Throwable ignored){}
            }
            if(!(key.getBackground() instanceof Glass)){originals.put(key,key.getBackground());setBackground(key,new Glass(key, key.getResources().getDisplayMetrics().density, key.getBackground()));invalidateTree(key);}
        }
        if(!c.refined){caps.apply(keys);cutouts.bind(keys);}else {caps.restore();cutouts.studio.bind(keys);}
    }
    static void invalidateTree(View v){v.invalidate();if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)invalidateTree(g.getChildAt(i));}}
    void refresh(){for(View v:originals.keySet())invalidateTree(v);}
    static void setBackground(View v,Drawable d){int l=v.getPaddingLeft(),t=v.getPaddingTop(),r=v.getPaddingRight(),b=v.getPaddingBottom();try{ownWrite.set(true);v.setBackground(d);v.setPadding(l,t,r,b);}finally{ownWrite.remove();}}
    void restore(){clearOverlays();cutouts.studio.clear();cutouts.clear();caps.restore();for(Map.Entry<View,Drawable> e:originals.entrySet()){CapHooks.clear(e.getKey());XposedHelpers.removeAdditionalInstanceField(e.getKey(),TAG);if(e.getKey().getBackground() instanceof Glass)setBackground(e.getKey(),e.getValue());invalidateTree(e.getKey());}originals.clear();}




    // Draw above native key paint. A background wrapper can be painted over by
    // Gboard's own key fill; ViewOverlay remains in the key's local clipped space.
    private void applyRefined(java.util.List<View> keys){
        if(!originals.isEmpty())restore();
        for(Iterator<Map.Entry<View,RippleOverlay>> it=overlays.entrySet().iterator();it.hasNext();){
            Map.Entry<View,RippleOverlay> entry=it.next();
            if(!keys.contains(entry.getKey())){entry.getKey().getOverlay().remove(entry.getValue());it.remove();}
        }
        for(View key:keys){
            RippleOverlay layer=overlays.get(key);
            if(layer==null){layer=new RippleOverlay(key);overlays.put(key,layer);key.getOverlay().add(layer);}
            layer.setBounds(0,0,key.getWidth(),key.getHeight());
        }
        cutouts.studio.bind(keys);
    }
    private void clearOverlays(){
        for(Map.Entry<View,RippleOverlay> entry:overlays.entrySet())entry.getKey().getOverlay().remove(entry.getValue());
        overlays.clear();
    }
    void refreshRipples(){for(RippleOverlay layer:overlays.values())layer.invalidateSelf();}
    static final class RippleOverlay extends Drawable {
        final View key;final float dp;
        final RipplePaint ripple=new RipplePaint();final RectF bounds=new RectF();
        private int alpha=255;
        RippleOverlay(View key){this.key=key;dp=key.getResources().getDisplayMetrics().density;}
        @Override public void draw(Canvas canvas){
            if(!cfg.enabled||!cfg.refined||!SideSweep.activeRow)return;
            bounds.set(0,0,key.getWidth(),key.getHeight());
            ripple.draw(canvas,bounds,dp,SideSweep.computeGlowAlpha(key,dp),cfg.opacity/100f*alpha/255f,6*dp);
        }
        @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){} // White-only effect.
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }

    /** v33 stock default always visible, border only during multi-wave, white one-color */
    static final class Glass extends Drawable implements Drawable.Callback {
        final RipplePaint ripple=new RipplePaint();
        final RectF rippleBounds=new RectF();
        final View view;
        final float dp;
        final Drawable orig;
        Glass(View v,float d,Drawable o){view=v;dp=d;orig=o;if(orig!=null)orig.setCallback(this);}
        Glass(View v,float d){this(v,d,null);}
        Glass(float d){this(null,d,null);}
        Glass(float d,Drawable o){this(null,d,o);}
        public void draw(Canvas c){
            if(view==null){
                if(orig!=null){orig.setBounds(getBounds());orig.draw(c);}
                return;
            }
            int w=view.getWidth(), h=view.getHeight();
            Rect b=getBounds();
            if(w<=0) w=b.width()>0?b.width():80;
            if(h<=0) h=b.height()>0?b.height():60;
            if(orig!=null){
                orig.setBounds(getBounds());
                orig.draw(c);
            }
            if(!cfg.enabled||!cfg.glass||cfg.sideStyle==0||!SideSweep.activeRow) return;
            float glowAlpha=SideSweep.computeGlowAlpha(view, dp);
            if(glowAlpha<=0.01f) return;
            if(cfg.refined){
                rippleBounds.set(getBounds());
                ripple.draw(c,rippleBounds,dp,glowAlpha,cfg.opacity/100f,6*dp);
                return;
            }
            int finalBorder=SideSweep.computeFinalBorder(view, 0xffe8e8ec, dp);
            RectF tile=new RectF(0,0,w,h);
            float rad=12*dp;
            if(tile.width()>tile.height()*1.8f)rad=tile.height()/2f;
            Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);glow.setColor(finalBorder);glow.setAlpha(85);glow.setStyle(Paint.Style.STROKE);glow.setStrokeWidth(6*dp);
            RectF gr=new RectF(tile);gr.inset(-1*dp,-1*dp);
            c.drawRoundRect(gr,rad,rad,glow);
            Paint rim=new Paint(Paint.ANTI_ALIAS_FLAG);rim.setColor(finalBorder);rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(2.4f*dp);
            RectF inset=new RectF(tile);inset.inset(1.2f*dp,1.2f*dp);
            c.drawRoundRect(inset,rad,rad,rim);
        }
        public void setAlpha(int a){if(orig!=null)orig.setAlpha(a);}public void setColorFilter(ColorFilter f){if(orig!=null)orig.setColorFilter(f);}
        @Override public boolean isStateful(){return orig!=null&&orig.isStateful();}
        @Override protected boolean onStateChange(int[] state){boolean changed=orig!=null&&orig.setState(state);if(changed)invalidateSelf();return changed;}
        @Override protected boolean onLevelChange(int level){return orig!=null&&orig.setLevel(level);}
        @Override public boolean getPadding(Rect padding){return orig!=null?orig.getPadding(padding):super.getPadding(padding);}
        @Override public int getIntrinsicWidth(){return orig!=null?orig.getIntrinsicWidth():-1;}
        @Override public int getIntrinsicHeight(){return orig!=null?orig.getIntrinsicHeight():-1;}
        @Override public void setHotspot(float x,float y){if(orig!=null)orig.setHotspot(x,y);}
        @Override public void jumpToCurrentState(){if(orig!=null)orig.jumpToCurrentState();}
        @Override public void invalidateDrawable(Drawable who){invalidateSelf();}
        @Override public void scheduleDrawable(Drawable who,Runnable what,long when){scheduleSelf(what,when);}
        @Override public void unscheduleDrawable(Drawable who,Runnable what){unscheduleSelf(what);}
        public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
