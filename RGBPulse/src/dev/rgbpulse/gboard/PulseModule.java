package dev.rgbpulse.gboard;

import android.graphics.*;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Strict panel lighting with optional reversible key cosmetics; never reads editor text. */
public final class PulseModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    @Override public void initZygote(StartupParam p){Typography.modulePath=p.modulePath;}
    static final String TARGET = "com.google.android.inputmethod.latin";
    static final String SELF = "dev.rgbpulse.gboard";
    private static final String SERVICE = "rgbpulse.v4.service", ROOT = "rgbpulse.v4.root", PANEL = "rgbpulse.v4.panel";
    private static final ArrayList<java.lang.ref.WeakReference<Controller>> controllers=new ArrayList<>();
    private static Config config = new Config();
    private static boolean loggedFailure;

    static void failure(Throwable t) {
        if (!loggedFailure) { loggedFailure = true; XposedBridge.log("RGBPulse 44: " + android.util.Log.getStackTraceString(t)); }
    }
    private static void acceptSettings(Config next){
        config=next;
        for(java.util.Iterator<java.lang.ref.WeakReference<Controller>> it=controllers.iterator();it.hasNext();){
            Controller controller=it.next().get();
            if(controller==null||controller.disposed){it.remove();continue;}
            controller.applyCurrentSettings();
        }
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!TARGET.equals(p.packageName)) return;
        KeyStyle.installTextHooks();
        XposedBridge.log("RGBPulse 44.0: loaded in " + p.processName);
        XposedHelpers.findAndHookMethod(InputMethodService.class, "setInputView", View.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                try {
                    Controller old = (Controller) XposedHelpers.removeAdditionalInstanceField(p.thisObject, SERVICE);
                    if (old != null) old.dispose();
                    if (p.args[0] instanceof ViewGroup) {
                        Controller c = new Controller((ViewGroup) p.args[0]);
                        XposedHelpers.setAdditionalInstanceField(p.thisObject, SERVICE, c);
                    }
                } catch (Throwable t) { failure(t); }
            }
        });
        XposedHelpers.findAndHookMethod(InputMethodService.class, "onStartInputView", EditorInfo.class, boolean.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                try {
                    Controller c = get(p.thisObject);
                    if (c != null) c.show();
                } catch (Throwable t) { failure(t); }
            }
        });
        XposedHelpers.findAndHookMethod(InputMethodService.class, "onWindowShown", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                try { Controller c = get(p.thisObject); if (c != null) c.show(); } catch (Throwable t) { failure(t); }
            }
        });
        XposedHelpers.findAndHookMethod(InputMethodService.class, "onWindowHidden", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                try { Controller c = get(p.thisObject); if (c != null) c.pause(); } catch (Throwable t) { failure(t); }
            }
        });
        XposedHelpers.findAndHookMethod(InputMethodService.class, "onDestroy", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    Controller c = (Controller) XposedHelpers.removeAdditionalInstanceField(p.thisObject, SERVICE);
                    if (c != null) c.dispose();
                } catch (Throwable t) { failure(t); }
            }
        });
        // Hook the FRAMEWORK implementation only, after subclass pre-super painting, before keys.
        // An implementation which bypasses super is intentionally unsupported (draw nothing).
        XposedHelpers.findAndHookMethod(ViewGroup.class, "dispatchDraw", Canvas.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { draw(p, false); }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                draw(p, true);
                Controller c=(Controller)XposedHelpers.getAdditionalInstanceField(p.thisObject,PANEL);
                if(c!=null){c.afterChildren();c.drawTrail((Canvas)p.args[0]);}
            }
            private void draw(MethodHookParam p, boolean after) {
                Controller c = (Controller) XposedHelpers.getAdditionalInstanceField(p.thisObject, PANEL);
                if (c == null || after != (!config.glass && config.layer == 1)) return;
                try { c.render((Canvas) p.args[0]); } catch (Throwable t) { c.disabled = true; failure(t); }
            }
        });
        XposedHelpers.findAndHookMethod(ViewGroup.class, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                Controller c = (Controller) XposedHelpers.getAdditionalInstanceField(p.thisObject, ROOT);
                if (c == null) return;
                try {
                    MotionEvent e = (MotionEvent) p.args[0]; int a = e.getActionMasked();
                    c.glideEvent(e);
                    if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
                        int i = e.getActionIndex(); c.tap(e.getX(i), e.getY(i));
                    }
                } catch (Throwable t) { failure(t); }
                // Never setResult(), change the event, or read the editor's text.
            }
        });
    }
    private static Controller get(Object service) { return (Controller) XposedHelpers.getAdditionalInstanceField(service, SERVICE); }

    static boolean keyClass(View v) {
        for (Class<?> cl = v.getClass(); cl != null && cl != View.class; cl = cl.getSuperclass()) {
            String name = cl.getSimpleName();
            if (name.equals("SoftKeyView")) return true;
        }
        return false;
    }
    static boolean keyboardClass(View v) {
        for (Class<?> cl = v.getClass(); cl != null && cl != View.class; cl = cl.getSuperclass()) {
            String n = cl.getSimpleName();
            if (n.equals("SoftKeyboardView")) return true;
        }
        return false;
    }
    static final class Selection {
        ViewGroup body;
        final ArrayList<View> keys = new ArrayList<View>();
        final Rect box = new Rect();
        int score;
    }
    /** Finds named Gboard keys in a real keyboard subtree; never guesses from screen width. */
    static Selection resolve(ViewGroup root) {
        ArrayList<View> keys = new ArrayList<View>(); collectKeys(root, keys, 0);
        LinkedHashMap<View,ArrayList<View>> groups=PanelPolicy.group(keys,root,new PanelPolicy.Access<View>() {
            public View parent(View v) { return v.getParent() instanceof View ? (View)v.getParent() : null; }
            public boolean softKeyboardPanel(View v) { return v instanceof ViewGroup && keyboardClass(v); }
        });
        Selection best = null;
        float dp = root.getResources().getDisplayMetrics().density;
        for (View node : groups.keySet()) {
            ViewGroup group=(ViewGroup)node;
            ArrayList<View> list = groups.get(group);
            if (list.size() < 8 || group.getWidth() < 100 * dp || group.getHeight() < 75 * dp) continue;
            // A specific SoftKeyboardView, never KeyboardHolder, rows or the input root.
            boolean named = keyboardClass(group);
            if (!named) continue;
            Rect box = new Rect(); ArrayList<Integer> rows = new ArrayList<Integer>(); boolean transformed = false;
            for (View k : list) {
                Rect r = new Rect(0, 0, k.getWidth(), k.getHeight()); group.offsetDescendantRectToMyCoords(k, r);
                box.union(r);
                int cy = r.centerY(); boolean near = false;
                for (int y : rows) if (Math.abs(y - cy) < Math.max(6 * dp, r.height() * .4f)) near = true;
                if (!near) rows.add(cy);
                for (View v = k; v != group; ) {
                    if (!v.getMatrix().isIdentity()) transformed = true;
                    if (!(v.getParent() instanceof View)) break;
                    v = (View) v.getParent();
                }
            }
            // Unknown/custom transformed layouts are unsupported instead of using stale coordinates.
            if (transformed || rows.size() < 3 || box.isEmpty()) continue;
            Rect limit = new Rect(group.getScrollX(), group.getScrollY(), group.getScrollX()+group.getWidth(), group.getScrollY()+group.getHeight());
            if (!box.intersect(limit)) continue;
            if (!BodyGeometry.acceptable(group==root,named,transformed,list.size(),rows.size(),
                    group.getWidth(),group.getHeight(),box.width(),box.height(),dp,
                    root.getResources().getDisplayMetrics().heightPixels)) continue;
            // Only standalone SoftKeyboardView candidates survive. Toolbar one-row panels do not.
            int score = BodyGeometry.score(list.size(),group.getWidth(),group.getHeight());
            if (best == null || score > best.score) {
                best = new Selection(); best.body = group; best.box.set(limit); best.keys.addAll(list); best.score = score;
            }
        }
        return best;
    }
    private static void collectKeys(View v, ArrayList<View> out, int depth) {
        if (depth > 35 || !v.isShown() || v.getAlpha() < .05f || v.getWidth() <= 0 || v.getHeight() <= 0) return;
        if (keyClass(v)) { out.add(v); return; }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i=0; i<g.getChildCount(); i++) collectKeys(g.getChildAt(i), out, depth+1);
        }
    }

    static final class Controller implements ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener, Runnable {
        final ViewGroup root;
        final Fx fx;
        final KeyStyle keyStyle=new KeyStyle();

        final RectF play = new RectF();
        final Rect visibleRect = new Rect();
        final EffectSurface surface = new EffectSurface();
        ViewGroup body;
        final ArrayList<View> keys = new ArrayList<View>();
        boolean disposed, disabled, visible = true, ticking, rendering;
        boolean recordedDraw, shaderIssueLogged, layoutDirty=true;
        long lastScan, lastDraw; int missedFrames; String lastLog = "";
        final Runnable settleScan = new Runnable(){public void run(){if(!visible||disposed)return;safeScan(true);kick();}};
        final Runnable scanLater = new Runnable() { public void run() { safeScan(true); kick(); } };

        Controller(ViewGroup root) {
            this.root = root; fx = new Fx(root.getResources().getDisplayMetrics().density); fx.cfg = config;
            XposedHelpers.setAdditionalInstanceField(root, ROOT, this);
            controllers.add(new java.lang.ref.WeakReference<>(this));
            SettingsClient.start(root.getContext(),PulseModule::acceptSettings);
            root.addOnAttachStateChangeListener(this); observe(); show();
        }
        void observe() {
            ViewTreeObserver o = root.getViewTreeObserver();
            if (o.isAlive()) { o.removeOnPreDrawListener(this); o.addOnPreDrawListener(this); o.removeOnGlobalLayoutListener(this); o.addOnGlobalLayoutListener(this); }
        }
        void show() {
            if(disposed)return;
            visible=true;SettingsClient.request();applyCurrentSettings();
        }
        void applyCurrentSettings(){
            if(disposed)return;
            // Off must clean up even if the panel is hidden or no longer bound.
            root.removeCallbacks(this);root.removeCallbacks(scanLater);root.removeCallbacks(settleScan);ticking=false;
            fx.clear();keyStyle.restore();bind(null);
            KeyStyle.configure(config,root.getContext());fx.cfg=config;
            disabled=false;missedFrames=0;layoutDirty=true;
            if(!config.enabled||!visible){root.invalidate();return;}
            if(config.debug){lastLog="";CapHooks.traces=0;}
            safeScan(true);kick();
        }
        void safeScan(boolean force) {
            try { scan(force); } catch (Throwable t) { bind(null); failure(t); }
        }
        void scan(boolean force) {
            long now = SystemClock.uptimeMillis();
            if (!force && now - lastScan < 120) return;
            lastScan = now;layoutDirty=false;
            if (!visible || disposed || !root.isShown() || !config.enabled) { bind(null); return; }
            Selection s = resolve(root);
            if (s == null) {
                bind(null);
                log("no verified keys body; drawing disabled (no root fallback)");
                return;
            }
            if (body != s.body) bind(s.body);
            if (!play.equals(new RectF(s.box))) {fx.clear();keyStyle.clearRippleFades();}
            play.set(s.box); keys.clear(); keys.addAll(s.keys); keyStyle.apply(keys,config);
            try{keyStyle.cutouts.studio.updateBody(s.body,keys);}catch(Throwable ignored){}
            if(config.glass){
                if(keyStyle.caps.retryPending){keyStyle.caps.retryPending=false;keyStyle.refresh();}
                if(keyStyle.caps.geometryPending){keyStyle.caps.geometryPending=false;body.invalidate();}
            }
            log("body="+body.getClass().getName()+"; size="+body.getWidth()+"x"+body.getHeight()+"; bg="+(body.getBackground()==null?"none":body.getBackground().getClass().getName())+"; keys="+keys.size()+"; clip="+s.box.toShortString()+"; layer="+config.layer);
        }
        void bind(ViewGroup next) {
            if (body == next && next != null) return;
            keyStyle.restore();
            if (body != null) { XposedHelpers.removeAdditionalInstanceField(body, PANEL); body.invalidate(); }
            body = next; recordedDraw=false; play.setEmpty(); keys.clear(); fx.clear(); missedFrames = 0;
            if (body != null) { XposedHelpers.setAdditionalInstanceField(body, PANEL, this); body.invalidate(); }
        }
        void log(String text) {
            if (!text.equals(lastLog)) {
                lastLog = text; XposedBridge.log("RGBPulse 44: "+text);
                if (config.debug) dump(root, 0, new int[]{0});
            }
        }
        void dump(View v, int depth, int[] count) {
            if (count[0] >= 90 || depth > 16 || v.getVisibility()!=View.VISIBLE) return;
            count[0]++;
            // No text, content descriptions, tags, editor info, or screenshots.
            XposedBridge.log("RGBPulse tree d="+depth+" "+v.getClass().getName()+" ["+v.getLeft()+","+v.getTop()+" "+v.getWidth()+"x"+v.getHeight()+"] bg="+(v.getBackground()==null?"none":v.getBackground().getClass().getName()));
            if (v instanceof ViewGroup) { ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) dump(g.getChildAt(i),depth+1,count); }
        }
        // Full key discovery allocates and reapplies hooks; do not repeat it every frame.
        // Touch and explicit layout-settle paths still request an immediate scan.
        @Override public void onGlobalLayout(){layoutDirty=true;}
        @Override public boolean onPreDraw() {
            // Symbol/letter swaps may replace children without replacing the panel.
            // Bypass the idle throttle after layout, before recording the first frame.
            safeScan(layoutDirty||body==null||!body.isShown());
            return true;
        }

        void glideEvent(MotionEvent e){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_CANCEL||e.getPointerCount()!=1){fx.glide.points.clear();if(body!=null)body.invalidate();return;}
            if(body==null||!visible||disabled||!config.enabled||config.trailStyle==0)return;
            Matrix m=new Matrix();root.transformMatrixToGlobal(m);body.transformMatrixToLocal(m);
            float[] xy={e.getX(),e.getY()};m.mapPoints(xy);xy[0]+=body.getScrollX();xy[1]+=body.getScrollY();
            if(!play.contains(xy[0],xy[1])){fx.glide.points.end();kick();return;}
            long now=SystemClock.uptimeMillis();
            if(action==MotionEvent.ACTION_DOWN){
                boolean hit=false;for(View key:keys){Rect r=new Rect(0,0,key.getWidth(),key.getHeight());body.offsetDescendantRectToMyCoords(key,r);if(r.contains((int)xy[0],(int)xy[1])){hit=true;break;}}
                if(hit)fx.glide.points.begin(xy[0],xy[1],now);
            }else if(action==MotionEvent.ACTION_MOVE)fx.glide.points.move(xy[0],xy[1],now,8*root.getResources().getDisplayMetrics().density);
            else if(action==MotionEvent.ACTION_UP)fx.glide.points.end();
            kick();
        }
        void drawTrail(Canvas canvas){
            if(body==null||!visible||disabled||!config.enabled||disposed||!body.isShown())return;
            int sv=canvas.save();try{if(!body.getLocalVisibleRect(visibleRect))return;canvas.clipRect(visibleRect);canvas.clipRect(play);fx.glide.draw(canvas,config,root.getResources().getDisplayMetrics().density,SystemClock.uptimeMillis());}finally{canvas.restoreToCount(sv);}
        }
        void tap(float x, float y) {
            if (disposed || disabled || !config.enabled) return;
            safeScan(true);
            if (body == null || play.isEmpty()) return;
            // Map root-local touch through Gboard transforms (floating / one-handed / slide).
            Matrix map = new Matrix(); root.transformMatrixToGlobal(map); body.transformMatrixToLocal(map);
            float[] xy = {x, y}; map.mapPoints(xy);
            float px = xy[0] + body.getScrollX(), py = xy[1] + body.getScrollY();
            if (!play.contains(px, py)) return;
            boolean hit = false;
            Rect r = new Rect();
            for (View k : keys) {
                r.set(0,0,k.getWidth(),k.getHeight()); body.offsetDescendantRectToMyCoords(k,r);
                if (r.contains((int)px,(int)py)) { px=r.exactCenterX(); py=r.exactCenterY(); hit=true; break; }
            }
            if (!hit || !play.contains(px,py)) return;
            long now=SystemClock.uptimeMillis();
            fx.cfg=config; fx.tap(px, py, play, now);
            if(config.glass && config.sideStyle>0){
                int rowTopBody=0, rowTopParent=0; float parentCx=0;
                for(View k:keys){Rect rr=new Rect(0,0,k.getWidth(),k.getHeight()); body.offsetDescendantRectToMyCoords(k,rr); if(rr.contains((int)px,(int)py)){rowTopBody=rr.top; rowTopParent=k.getTop(); parentCx=k.getLeft()+k.getWidth()/2f; break;}}
                fx.side.tapParent(parentCx,rowTopParent,now,px,rowTopBody);
            }
            kick();
            root.removeCallbacks(settleScan);root.postDelayed(settleScan,140);
        }
        void kick() {
            if (ticking || disposed || disabled || !visible || body==null || !body.isShown() || !config.enabled) return;
            // Always allow a final cleanup frame. Testing/expiring waves here used to
            // skip the last redraw and leave a cached key highlight behind.
            ticking=true; root.postOnAnimation(this);
        }
        @Override public void run() {
            ticking=false;
            if (disposed || disabled || !visible || !config.enabled || body==null || !body.isShown()) return;
            long now=SystemClock.uptimeMillis();
            if (lastDraw==0 || now-lastDraw>150) missedFrames++; else missedFrames=0;
            if (missedFrames>60 && !config.refined) { disabled=true; log("selected view bypassed framework dispatchDraw or stopped drawing; effect stopped"); return; }
            boolean active=fx.active(now);
            boolean sideActive=fx.side.active(now,config.duration);
            if(sideActive)fx.side.advance(config,play,now);
            boolean fading=keyStyle.advanceRipples(now);
            // postOnAnimation already follows the display refresh rate. A fixed 16ms
            // gate skips 90/120Hz frames and can freeze cached key backgrounds.
            body.invalidate();
            for(View k:keys) k.invalidate();
            keyStyle.refreshRipples();
            if(active||sideActive||fading) kick();
        }
        void render(Canvas canvas) {
            if (rendering || disposed || disabled || !visible || !config.enabled || body==null || play.isEmpty() || !body.isShown()) return;
            lastDraw=SystemClock.uptimeMillis();
            if(!recordedDraw) {
                recordedDraw=true;
                XposedBridge.log("RGBPulse 44: draw reached "+body.getClass().getName()+"; phase="+(config.glass||config.layer==0?"after panel paint / before children":"after children"));
            }
            if (!body.getLocalVisibleRect(visibleRect)) return;
            if (!config.glass && !fx.active(lastDraw)) return;
            int save=canvas.save(); rendering=true;
            try {
                // Strict typing-panel bounds for the unchanged RGB field and sideways sweep.
                canvas.clipRect(visibleRect); canvas.clipRect(play);
                surface.draw(canvas,fx,play,lastDraw);
                if(config.sideStyle>0)fx.side.draw(canvas,config,play,root.getResources().getDisplayMetrics().density,lastDraw);

                if (!shaderIssueLogged && fx.shaderIssue()!=null) {
                    shaderIssueLogged=true; XposedBridge.log("RGBPulse 44: shader disabled: "+fx.shaderIssue());
                }
            } finally {
                canvas.restoreToCount(save);rendering=false;
                if(keyStyle.caps.retryPending||keyStyle.caps.geometryPending){root.removeCallbacks(scanLater);root.post(scanLater);}
            }
        }
        void afterChildren(){
            if(!disposed&&visible&&(keyStyle.caps.retryPending||keyStyle.caps.geometryPending)){
                root.removeCallbacks(scanLater);root.post(scanLater);
            }
        }
        void pause() {
            visible=false; fx.clear();keyStyle.clearRippleFades(); root.removeCallbacks(this); root.removeCallbacks(scanLater); root.removeCallbacks(settleScan); ticking=false;
            if (body!=null) {body.invalidate();for(View k:keys) k.invalidate();keyStyle.refreshRipples();}
        }
        @Override public void onViewAttachedToWindow(View v) { observe(); show(); }
        @Override public void onViewDetachedFromWindow(View v) { pause(); bind(null); surface.release(); }
        void dispose() {
            if (disposed) return;
            pause(); bind(null); disposed=true;
            ViewTreeObserver o=root.getViewTreeObserver(); if(o.isAlive()){o.removeOnPreDrawListener(this);o.removeOnGlobalLayoutListener(this);}
            root.removeOnAttachStateChangeListener(this); XposedHelpers.removeAdditionalInstanceField(root,ROOT);
            surface.release(); fx.dispose();
        }
    }
}
