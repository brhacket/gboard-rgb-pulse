package dev.rgbpulse.gboard;

import android.graphics.*;
import android.view.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import de.robv.android.xposed.*;

/** Suppresses large plate fills in verified key subtrees. Multi-contour, open and concave paths are preserved. */
final class CapHooks {
    static final String ENTRY="rgbpulse.cap.context";
    static final ThreadLocal<Entry> active=new ThreadLocal<Entry>();
    static final ThreadLocal<Anchor> anchor=new ThreadLocal<Anchor>();
    static int traces;
    static final class Anchor {final Canvas canvas;final Matrix matrix=new Matrix();Anchor(Canvas c){canvas=c;c.getMatrix(matrix);}}
    static void trace(String reason,Entry e,String operation,RectF bounds){
        if(!KeyStyle.cfg.debug||traces>=80)return;traces++;
        XposedBridge.log("RGBPulse 33 cap "+reason+" op="+operation+" view="+e.view.getClass().getName()+" keyClass="+e.key.getClass().getName()+" keyPos="+e.key.getLeft()+","+e.key.getTop()+" key="+e.key.getWidth()+"x"+e.key.getHeight()+" padding="+e.key.getPaddingLeft()+","+e.key.getPaddingTop()+","+e.key.getPaddingRight()+","+e.key.getPaddingBottom()+" bounds="+bounds);
    }
    static final ThreadLocal<Boolean> drawing=new ThreadLocal<Boolean>();
    static final HashSet<Method> installed=new HashSet<Method>();
    static final HashSet<Class<?>> scannedDrawClasses=new HashSet<Class<?>>();
    static int removedFills,removedBackgrounds;
    boolean retryPending;
    final java.util.ArrayList<Profile> profiles=new java.util.ArrayList<Profile>();
    static final class Profile {
        final float w,h;final int color;final String op;final Class<?> keyClass;
        final java.util.HashSet<View> witnesses=new java.util.HashSet<View>();
        Profile(RectF r,Paint paint,String op,View key){w=r.width();h=r.height();color=paint.getColor();this.op=op;keyClass=key.getClass();witnesses.add(key);}
        boolean same(RectF r,Paint p,String name){return p.getShader()==null&&color==p.getColor()&&op.equals(name)
            &&Math.abs(r.width()-w)<=w*.1f&&Math.abs(r.height()-h)<=h*.1f;}
    }
    void learn(RectF r,Paint paint,String op,View key){
        if(paint.getShader()!=null)return;
        for(Profile p:profiles)if(p.keyClass==key.getClass()&&p.same(r,paint,op)){
            if(p.witnesses.size()<2&&p.witnesses.add(key)&&p.witnesses.size()==2)retryPending=true;
            return;
        }
        if(profiles.size()<24)profiles.add(new Profile(r,paint,op,key));
    }
    boolean knownEdge(RectF r,Paint paint,String op,View key){
        for(Profile p:profiles)if(p.witnesses.size()>=2&&p.keyClass==key.getClass()&&p.same(r,paint,op)
            &&CapPolicy.edgeCap(r.left,r.top,r.right,r.bottom,key.getWidth(),key.getHeight(),p.w,p.h))return true;
        return false;
    }
    final java.util.IdentityHashMap<View,RectF> observed=new java.util.IdentityHashMap<View,RectF>();
    boolean geometryPending;
    void observe(View key,RectF r){
        RectF old=observed.get(key);
        if(old==null||Math.abs(old.left-r.left)>.5f||Math.abs(old.top-r.top)>.5f||Math.abs(old.right-r.right)>.5f||Math.abs(old.bottom-r.bottom)>.5f){
            // Prefer the largest confidently accepted plate. Avoid state-layer size oscillation.
            if(old!=null && r.width()*r.height()<old.width()*old.height()*.98f)return;
            if(old!=null&&r.width()*r.height()<=old.width()*old.height()*1.02f){
                // Keep the first equal-size position stable during state-layer transitions.
                return;
            }
            observed.put(key,new RectF(r));geometryPending=true;
        }
    }
    RectF visualBounds(View key){
        RectF r=observed.get(key);if(r!=null)return new RectF(r);
        float pl=key.getPaddingLeft(),pt=key.getPaddingTop(),pr=key.getPaddingRight(),pb=key.getPaddingBottom();
        if(CapPolicy.paddedCap(pl,pt,key.getWidth()-pr,key.getHeight()-pb,key.getWidth(),key.getHeight(),pl,pt,pr,pb))
            return new RectF(pl,pt,key.getWidth()-pr,key.getHeight()-pb);
        return new RectF(0,0,key.getWidth(),key.getHeight());
    }
    static boolean padded(View key,RectF r){return CapPolicy.paddedCap(r.left,r.top,r.right,r.bottom,
        key.getWidth(),key.getHeight(),key.getPaddingLeft(),key.getPaddingTop(),key.getPaddingRight(),key.getPaddingBottom());}
    static void hookDeclaredDraws(Class<?> cl){
        // onDraw is important when HWUI skips View.draw() for a child display-list refresh.
        for(Class<?> c=cl;c!=null&&View.class.isAssignableFrom(c);c=c.getSuperclass()){
            try{hook(c.getDeclaredMethod("onDraw",Canvas.class));}catch(Throwable ignored){}
        }
    }
    static final String CHILD="rgbpulse.cap.child";
    final java.util.IdentityHashMap<View,android.graphics.drawable.Drawable> originals=new java.util.IdentityHashMap<View,android.graphics.drawable.Drawable>();
    final java.util.HashSet<View> visited=new java.util.HashSet<View>();
    static final class Clear extends android.graphics.drawable.ColorDrawable {Clear(){super(Color.TRANSPARENT);}}
    void apply(java.util.List<View> keys){
        observed.keySet().retainAll(keys);
        for(java.util.Iterator<Profile> i=profiles.iterator();i.hasNext();){Profile p=i.next();p.witnesses.retainAll(keys);if(p.witnesses.isEmpty())i.remove();}
        visited.clear();for(View key:keys){track(key,key,0,0);walk(key,key,0,0);}
        for(java.util.Iterator<java.util.Map.Entry<View,android.graphics.drawable.Drawable>> it=originals.entrySet().iterator();it.hasNext();){
            java.util.Map.Entry<View,android.graphics.drawable.Drawable> e=it.next();
            if(!visited.contains(e.getKey())){restore(e.getKey(),e.getValue());it.remove();}
        }
    }
    static boolean edgeBackground(View key,View v,float x,float y){
        android.graphics.drawable.Drawable bg=v.getBackground();
        if(!(bg instanceof android.graphics.drawable.ColorDrawable)&&!(bg instanceof android.graphics.drawable.GradientDrawable)
            &&!(bg instanceof android.graphics.drawable.StateListDrawable)&&!(bg instanceof android.graphics.drawable.RippleDrawable))return false;
        return CapPolicy.offsetPlate(x,y,x+v.getWidth(),y+v.getHeight(),key.getWidth(),key.getHeight());
    }
    void walk(View key,View v,float x,float y){
        if(v!=key && (edgeBackground(key,v,x,y)||CapPolicy.cap(x,y,x+v.getWidth(),y+v.getHeight(),key.getWidth(),key.getHeight())||padded(key,new RectF(x,y,x+v.getWidth(),y+v.getHeight())))){
            visited.add(v);
            if(!originals.containsKey(v)){
                originals.put(v,v.getBackground());XposedHelpers.setAdditionalInstanceField(v,CHILD,this);
            }
            if(!(v.getBackground() instanceof Clear)){
                originals.put(v,v.getBackground());KeyStyle.setBackground(v,new Clear());v.invalidate();removedBackgrounds++;
            }
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){
            View child=g.getChildAt(i);if(child.getMatrix().isIdentity())walk(key,child,x+child.getLeft()-g.getScrollX(),y+child.getTop()-g.getScrollY());
        }}
    }
    static void restore(View v,android.graphics.drawable.Drawable old){
        XposedHelpers.removeAdditionalInstanceField(v,CHILD);if(v.getBackground() instanceof Clear)KeyStyle.setBackground(v,old);KeyStyle.invalidateTree(v);
    }
    void restore(){for(java.util.Map.Entry<View,android.graphics.drawable.Drawable> e:originals.entrySet())restore(e.getKey(),e.getValue());originals.clear();profiles.clear();observed.clear();retryPending=false;geometryPending=false;}

    static final class Entry {
        final View key,view; final float x,y; final int w,h;
        Entry(View key,View view,float x,float y){this.key=key;this.view=view;this.x=x;this.y=y;w=view.getWidth();h=view.getHeight();}
    }
    static void track(View key,View v,float x,float y){
        Object old=XposedHelpers.getAdditionalInstanceField(v,ENTRY);
        if(!(old instanceof Entry)||((Entry)old).key!=key||((Entry)old).x!=x||((Entry)old).y!=y||((Entry)old).w!=v.getWidth()||((Entry)old).h!=v.getHeight()){
            if(v==key){Object style=XposedHelpers.getAdditionalInstanceField(key,KeyStyle.TAG);if(style instanceof KeyStyle)((KeyStyle)style).caps.observed.remove(key);}
            XposedHelpers.setAdditionalInstanceField(v,ENTRY,new Entry(key,v,x,y));v.invalidate();
        }
        try {hook(v.getClass().getMethod("draw",Canvas.class));}catch(Throwable ignored){}
        if(scannedDrawClasses.add(v.getClass()))hookDeclaredDraws(v.getClass());
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View child=g.getChildAt(i);if(!child.getMatrix().isIdentity())continue;
                track(key,child,x+child.getLeft()-g.getScrollX(),y+child.getTop()-g.getScrollY());
            }
        }
    }
    static void clear(View v){
        XposedHelpers.removeAdditionalInstanceField(v,ENTRY);
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)clear(g.getChildAt(i));}
    }
    static void hook(Method method){
        if(!installed.add(method))return;
        XposedBridge.hookMethod(method,new XC_MethodHook(){
            protected void beforeHookedMethod(MethodHookParam p){
                if(!KeyStyle.cfg.enabled||!KeyStyle.cfg.glass)return;
                Object e=XposedHelpers.getAdditionalInstanceField(p.thisObject,ENTRY);if(!(e instanceof Entry))return;
                p.setObjectExtra("oldCap",active.get());p.setObjectExtra("oldAnchor",anchor.get());p.setObjectExtra("capScope",true);active.set((Entry)e);
                if(p.args.length>0&&p.args[0] instanceof Canvas)anchor.set(new Anchor((Canvas)p.args[0]));else anchor.remove();
            }
            protected void afterHookedMethod(MethodHookParam p){
                if(p.getObjectExtra("capScope")!=null){Entry old=(Entry)p.getObjectExtra("oldCap");if(old==null)active.remove();else active.set(old);
                    Anchor a=(Anchor)p.getObjectExtra("oldAnchor");if(a==null)anchor.remove();else anchor.set(a);
                }
            }
        });
    }
    static void install(){
        XposedHelpers.findAndHookMethod(View.class,"setBackgroundDrawable",android.graphics.drawable.Drawable.class,new XC_MethodHook(){
            protected void beforeHookedMethod(MethodHookParam p){
                if(Boolean.TRUE.equals(KeyStyle.ownWrite.get())||!KeyStyle.cfg.enabled||!KeyStyle.cfg.glass)return;
                Object owner=XposedHelpers.getAdditionalInstanceField(p.thisObject,CHILD);
                if(owner instanceof CapHooks && !(p.args[0] instanceof Clear)){
                    ((CapHooks)owner).originals.put((View)p.thisObject,(android.graphics.drawable.Drawable)p.args[0]);p.args[0]=new Clear();
                }
            }
        });
        try {hook(View.class.getDeclaredMethod("updateDisplayListIfDirty"));}catch(Throwable ignored){}
        HashSet<Method> seen=new HashSet<Method>();
        for(String cl:new String[]{"android.graphics.Canvas","android.graphics.BaseCanvas","android.graphics.BaseRecordingCanvas","android.graphics.RecordingCanvas"})try{
            for(Method method:Class.forName(cl).getDeclaredMethods()){
                String name=method.getName();
                if(name.equals("drawBitmap")||name.equals("drawRenderNode")){
                    if(seen.add(method))XposedBridge.hookMethod(method,new XC_MethodHook(){
                        protected void beforeHookedMethod(MethodHookParam p){Entry e=active.get();if(e!=null&&!Boolean.TRUE.equals(drawing.get()))trace("observed-not-modified",e,p.method.getName(),new RectF());}
                    });
                    continue;
                }
                if(!(name.equals("drawRect")||name.equals("drawRoundRect")||name.equals("drawOval")||name.equals("drawCircle")||name.equals("drawPath")))continue;
                Class<?>[] types=method.getParameterTypes();if(types.length==0||types[types.length-1]!=Paint.class||!seen.add(method))continue;
                XposedBridge.hookMethod(method,new XC_MethodHook(){
                    protected void beforeHookedMethod(MethodHookParam p){
                        Entry e=active.get();
                        if(e==null||!KeyStyle.cfg.enabled||!KeyStyle.cfg.glass||Boolean.TRUE.equals(drawing.get()))return;
                        // Entry may outlive a hidden/recycled child, so verify ownership at draw time.
                        Object style=XposedHelpers.getAdditionalInstanceField(e.key,KeyStyle.TAG);
                        if(!(style instanceof KeyStyle))return;
                        CapHooks owner=((KeyStyle)style).caps;
                        try {
                            Paint paint=(Paint)p.args[p.args.length-1];
                            if(paint.getStyle()!=Paint.Style.FILL||paint.getAlpha()<204)return;
                            RectF r=new RectF();
                            if(p.args[0] instanceof Path){
                                Path path=(Path)p.args[0];
                                if(path.isInverseFillType())return;
                                path.computeBounds(r,true);
                                if(r.width()<e.key.getWidth()*.5f||r.height()<e.key.getHeight()*.5f)return;
                                PathMeasure pm=new PathMeasure(path,false);
                                if(!pm.isClosed()||pm.getLength()<=0)return;
                                float length=pm.getLength();float[] xy=new float[2],points=new float[48];
                                for(int j=0;j<24;j++){if(!pm.getPosTan(length*j/24,xy,null))return;points[j*2]=xy[0];points[j*2+1]=xy[1];}
                                if(pm.nextContour()||!CapPolicy.plate(points,r.width(),r.height())){
                                    trace("path-preserved",e,p.method.getName(),r);return;
                                }
                            }else if(p.args[0] instanceof RectF)r.set((RectF)p.args[0]);
                            else if(p.args[0] instanceof Rect)r.set((Rect)p.args[0]);
                            else if(p.method.getName().equals("drawCircle")){
                                float x=(Float)p.args[0],y=(Float)p.args[1],radius=(Float)p.args[2];r.set(x-radius,y-radius,x+radius,y+radius);
                            }else {if(p.args.length<5)return;r.set((Float)p.args[0],(Float)p.args[1],(Float)p.args[2],(Float)p.args[3]);}
                            // Only compare local simple fills. Reject transformed canvases rather than guessing.
                            Matrix m=new Matrix();((Canvas)p.thisObject).getMatrix(m);
                            Anchor a=anchor.get();
                            if(a!=null && a.canvas==p.thisObject){Matrix inv=new Matrix();if(!a.matrix.invert(inv))return;Matrix relative=new Matrix();relative.setConcat(inv,m);m=relative;}
                            float[] values=new float[9];m.getValues(values);
                            if(!CapPolicy.axisAligned(values)){trace("matrix-rejected",e,p.method.getName(),r);return;}
                            m.mapRect(r);r.offset(e.x,e.y);
                            boolean standard=CapPolicy.cap(r.left,r.top,r.right,r.bottom,e.key.getWidth(),e.key.getHeight());
                            boolean inset=padded(e.key,r)||((p.method.getName().equals("drawRoundRect")||p.method.getName().equals("drawRect"))&&paint.getShader()==null&&CapPolicy.offsetPlate(r.left,r.top,r.right,r.bottom,e.key.getWidth(),e.key.getHeight()));
                            String op=p.method.getName();
                            boolean edge=!standard&&!inset&&owner.knownEdge(r,paint,op,e.key);
                            if(!standard&&!inset&&!edge){
                                if(r.width()>e.key.getWidth()*.4f&&r.height()>e.key.getHeight()*.5f)trace("large-fill-preserved",e,op,r);
                                return;
                            }
                            if(standard)owner.learn(r,paint,op,e.key);
                            // Use observed cap geometry for the visible lens, not its enlarged hit box.
                            if(r.left>=0&&r.top>=0&&r.right<=e.key.getWidth()&&r.bottom<=e.key.getHeight())owner.observe(e.key,r);
                            trace(edge?"suppressed-matching-edge":(inset&&!standard?"suppressed-padded-cap":"suppressed-fill"),e,p.method.getName(),r);p.setResult(null);removedFills++;
                        }catch(Throwable ignored){}
                    }
                });
            }
        }catch(Throwable ignored){}
    }
}
