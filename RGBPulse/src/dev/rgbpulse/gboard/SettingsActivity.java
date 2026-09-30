package dev.rgbpulse.gboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public final class SettingsActivity extends Activity {
    private SharedPreferences sp;
    private Config cfg;
    private Preview preview;
    private float dp;
    private TextView status, selectedEffect;

    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        boolean hooked = true;
        try {
            // Vector/LSPosed make this world-readable for the Gboard hook (xposedsharedprefs).
            sp = getSharedPreferences(Config.PREFS, Context.MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            hooked = false;
            sp = getSharedPreferences(Config.PREFS, Context.MODE_PRIVATE);
        }
        cfg = Config.from(sp);
        getWindow().setStatusBarColor(0xFF121218);
        getWindow().setNavigationBarColor(0xFF121218);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * dp);
        col.setPadding(p, p, p, p * 3);

        TextView title = text("RGB Pulse 34", 26, 0xFFFFFFFF);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        status = text(hooked ? "Shared-settings access available. This does not confirm Gboard detection." :
                "Module not active in Vector. Enable it, scope Gboard, then reopen this app.", 13,
                hooked ? 0xFF8BE78B : 0xFFFF8A80);
        col.addView(status);

        preview = new Preview(this);
        col.addView(preview, new LinearLayout.LayoutParams(-1, (int) (190 * dp)));
        col.addView(text("Tap or glide to preview. This is a demo layout, not a capture of Gboard. Close and reopen Gboard to apply settings.", 12, 0xFF9A9AB0));

        EditText test = new EditText(this);
        test.setHint("Test with Gboard here…");
        test.setHintTextColor(0xFF77778A);
        test.setTextColor(0xFFFFFFFF);
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        col.addView(test);

        Button restart=new Button(this);restart.setText("Apply / force-stop Gboard");
        restart.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Restart Gboard?")
            .setMessage("Saves settings and requests root to force-stop Gboard. The keyboard will close; tap a text field to reopen it. If root is unavailable, Android app settings will open. Keep a backup keyboard available.")
            .setPositiveButton("Apply",(d,w) -> restartGboard(restart)).setNegativeButton("Cancel",null).show());col.addView(restart);
        section(col,"Pulse Studio · solid tonal tiles");
        toggle(col,"Solid tonal tiles (Pulse Studio)",cfg.glass,v->{cfg.glass=v;save("glass9",v);});
        col.addView(text("Solid squircle tiles 18dp radius, exterior shadow band only, accent rim .55dp, centered solid legends with luminance-based dark/light text. Row-limited expanding background on tap: origin at tapped key, sweep travels right 90% width, keys left of front in that row blend toward accent. Triple-glow beam option.",12,0xFF9A9AB0));
        choice(col,"Letter font",new String[]{"Gboard original","System medium","Rounded (system fallback)","Monospace","Serif","Imported font","Manrope","Outfit","Space Grotesk · Google Fonts","Syne · display","Orbitron · display"},cfg.font,v->{cfg.font=v;save("font11",v);});
        slider(col,"Circle size",60,95,cfg.tileScale," %",v->{cfg.tileScale=v;save("tileScale18",v);});
        Button accent=new Button(this);accent.setText("Accent color · rims and number hints");accent.setOnClickListener(v->accentDialog());col.addView(accent);
        Button tileColor=new Button(this);tileColor.setText("Tile background color");tileColor.setOnClickListener(v->tileDialog());col.addView(tileColor);
        choice(col,"Sideways expanding row",new String[]{"Off","Single beam","Triple glow"},cfg.sideStyle,v->{cfg.sideStyle=v;save("side20",v);});
        choice(col,"Glide trail",new String[]{"Off","Neon ribbon","Twin stream","Accent filament"},cfg.trailStyle,v->{cfg.trailStyle=v;save("trail19",v);});
        slider(col,"Trail width",1,6,cfg.trailWidth," dp",v->{cfg.trailWidth=v;save("trailWidth19",v);});
        slider(col,"Trail fade",150,1000,cfg.trailLife," ms",v->{cfg.trailLife=v;save("trailLife19",v);});
        col.addView(text("Number hints move inside the circles. Accent controls rims, hints and the filament trail—not Gboard's toolbar or icons. Trails are visual only; turn off Gboard's own gesture trail to avoid two trails. Glide typing must still be enabled in Gboard.",12,0xFF9A9AB0));
        Button finishPreset=new Button(this);finishPreset.setText("Apply retro circular tiles");
        finishPreset.setOnClickListener(v->{sp.edit().putBoolean("glass9",true).putBoolean("letterWave16",false).putInt("layer4",0).putInt("font11",6).putInt("letterSize9",100).putInt("tileScale18",78).commit();recreate();});col.addView(finishPreset);
        toggle(col,"Bold letters",cfg.bold,v->{cfg.bold=v;save("bold9",v);});
        slider(col,"Letter size",75,125,cfg.letterSize," %",v->{cfg.letterSize=v;save("letterSize9",v);});

        Button fontImport=new Button(this);fontImport.setText("Import TTF / OTF (max 1 MB)");
        fontImport.setOnClickListener(v->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(android.content.Intent.CATEGORY_OPENABLE);startActivityForResult(i,91);});col.addView(fontImport);
        Button removeFont=new Button(this);removeFont.setText("Remove imported font");removeFont.setOnClickListener(v->{sp.edit().remove("fontData9").putInt("font11",6).commit();recreate();});col.addView(removeFont);
        section(col, "General");
        toggle(col, "Enabled", cfg.enabled, v -> { cfg.enabled = v; save("enabled", v); });
        choice(col, "Draw layer", Config.LAYERS, cfg.layer, i -> { cfg.layer = i; save("layer4", i); });
        col.addView(text("Both modes target only the typing panel, never the toolbar or editor. Retro circular cutout tiles always keep RGB underneath so waves show through the letters. With tiles off, both draw layers are available. Unsupported layouts stay unmodified.", 12, 0xFF9A9AB0));

        section(col, "Tap animation");
        col.addView(text("Hologram tiles + 10 redesigned reactive effects: luminous halos, glass rims, aurora curtains and merging liquid blobs. Background only by default. Requires Android 13+.", 12, 0xFF9A9AB0));
        selectedEffect=text("",18,0xFFFFFFFF); col.addView(selectedEffect); updateEffectLabel();
        LinearLayout browse=new LinearLayout(this);
        Button prev=new Button(this);prev.setText("‹");prev.setContentDescription("Previous animation");
        prev.setOnClickListener(v -> selectEffect((cfg.effect+Config.EFFECTS.length-1)%Config.EFFECTS.length));
        Button catalog=new Button(this);catalog.setText("Browse animations");catalog.setOnClickListener(v -> showCatalog());
        Button next=new Button(this);next.setText("›");next.setContentDescription("Next animation");
        next.setOnClickListener(v -> selectEffect((cfg.effect+1)%Config.EFFECTS.length));
        browse.addView(prev,new LinearLayout.LayoutParams((int)(52*dp),-2));
        browse.addView(catalog,new LinearLayout.LayoutParams(0,-2,1));
        browse.addView(next,new LinearLayout.LayoutParams((int)(52*dp),-2));col.addView(browse);
        LinearLayout presets=new LinearLayout(this);
        Button water=new Button(this);water.setText("Neon halo");
        water.setOnClickListener(v -> preset(1,3,185,220,1750,110));
        Button oil=new Button(this);oil.setText("Liquid glass");
        oil.setOnClickListener(v -> preset(2,3,185,285,1900,115));
        presets.addView(water,new LinearLayout.LayoutParams(0,-2,1));presets.addView(oil,new LinearLayout.LayoutParams(0,-2,1));col.addView(presets);
        LinearLayout presets2=new LinearLayout(this);
        Button tiles=new Button(this);tiles.setText("Hologram tiles");tiles.setOnClickListener(v -> selectEffect(0));
        Button shuffle=new Button(this);shuffle.setText("Shuffle waves");shuffle.setOnClickListener(v -> preset(Config.LIQUID_SHUFFLE,0,185,280,1850,115));
        presets2.addView(tiles,new LinearLayout.LayoutParams(0,-2,1));presets2.addView(shuffle,new LinearLayout.LayoutParams(0,-2,1));col.addView(presets2);
        slider(col, "Duration", 300, 3500, cfg.duration, " ms", v -> { cfg.duration = v; save("duration3", v); });
        slider(col, "Size", 30, 250, cfg.size, " %", v -> { cfg.size = v; save("size", v); });
        slider(col, "Thickness", 25, 300, cfg.thickness, " %", v -> { cfg.thickness = v; save("thickness8", v); });
        col.addView(text("100% = original ring width. Does not change expansion size. Hologram tiles stays unchanged. Pulses start at key centers.", 12, 0xFF9A9AB0));
        slider(col, "Opacity", 5, 100, cfg.opacity, " %", v -> { cfg.opacity = v; save("opacity3", v); });


        section(col, "Colors");
        choice(col, "Color mode", Config.COLORS, cfg.colorMode, i -> { cfg.colorMode = i; save("colorMode", i); });
        huePicker(col,"Primary color",true);
        huePicker(col,"Secondary color",false);
        slider(col, "Saturation", 0, 100, cfg.sat, " %", v -> { cfg.sat = v; save("sat", v); });

        col.addView(text("Tap-only animation: idle effects and their old settings have been removed. Smaller Size keeps the circular front on the keyboard longer; Duration controls travel time. The keyboard edge always clips the wave.",12,0xFF9A9AB0));

        section(col, "Detection & troubleshooting");
        toggle(col, "Detailed layout logs (no drawing)", cfg.debug, v -> { cfg.debug = v; save("debug3", v); });
        col.addView(text("The diagnostic outline has been removed. This switch enables bounded Vector layout/cap logs, starting with RGBPulse 33. Diagnostics contain class names, positions, dimensions and padding only, never typed text. If there is no effect, send those lines and your Gboard version. Unsupported handwriting, emoji-only or virtual-key layouts intentionally stay unmodified.", 12, 0xFF9A9AB0));
        Button reset = new Button(this);
        reset.setText("Reset to defaults");
        reset.setOnClickListener(v -> { sp.edit().clear().commit(); recreate(); });
        col.addView(reset);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF121218);
        sv.setFitsSystemWindows(true);
        sv.addView(col);
        setContentView(sv);
    }

    void restartGboard(Button button) {
        sp.edit().commit();button.setEnabled(false);button.setText("Waiting for root…");
        new Thread(()->{
            boolean ok=false;Process process=null;
            try {
                process=new ProcessBuilder("su","-c","am force-stop --user current com.google.android.inputmethod.latin").redirectErrorStream(true).start();
                ok=process.waitFor(25,java.util.concurrent.TimeUnit.SECONDS)&&process.exitValue()==0;
            }catch(Exception ignored){}finally{if(process!=null)process.destroy();}
            final boolean success=ok;
            runOnUiThread(()->{if(isFinishing()||isDestroyed())return;button.setEnabled(true);button.setText("Apply / force-stop Gboard");
                if(success)android.widget.Toast.makeText(this,"Gboard stopped. Tap a text field to reopen.",1).show();
                else {android.widget.Toast.makeText(this,"Root unavailable or command failed. Tap Force stop here.",1).show();
                    try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:com.google.android.inputmethod.latin")));}catch(Exception e){android.widget.Toast.makeText(this,"Open Android Settings > Apps > Gboard manually.",1).show();}}
            });
        },"Gboard restart").start();
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);if(request!=91||result!=RESULT_OK||data==null||data.getData()==null)return;
        final android.net.Uri uri=data.getData();
        new Thread(()->{
            java.io.File f=null;
            try(java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1048576)throw new Exception("Font must be 1 MB or smaller");out.write(buf,0,n);}
                byte[] bytes=out.toByteArray();if(bytes.length<4)throw new Exception("Invalid font");
                boolean ttf=bytes[0]==0&&bytes[1]==1&&bytes[2]==0&&bytes[3]==0;
                boolean otf=bytes[0]=='O'&&bytes[1]=='T'&&bytes[2]=='T'&&bytes[3]=='O';
                if(!ttf&&!otf)throw new Exception("Choose a TTF or OTF font");
                f=java.io.File.createTempFile("fontcheck",".ttf",getCacheDir());try(java.io.FileOutputStream file=new java.io.FileOutputStream(f)){file.write(bytes);}Typeface.createFromFile(f);
                if(!sp.edit().putString("fontData9",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)).putInt("font11",5).commit())throw new Exception("Unable to save font");
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){android.widget.Toast.makeText(this,"Font imported. Apply to Gboard.",1).show();recreate();}});
            }catch(Exception e){runOnUiThread(()->android.widget.Toast.makeText(this,"Import failed: "+e.getMessage(),1).show());}
            finally{if(f!=null)f.delete();}
        },"Font import").start();
    }
    void tileDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding((int)(20*dp),0,(int)(20*dp),0);
        EditText hex=new EditText(this);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setText(String.format("#%06X",cfg.tileColor&0xffffff));box.addView(hex);
        LinearLayout colors=new LinearLayout(this);box.addView(colors);
        for(int color:new int[]{0xfff5f5f3,0xff1a1c1e,0xffa6f0c6,0xffd8e4ff,0xffffe8a0}){
            Button b=new Button(this);b.setText("●");b.setTextColor(color);b.setOnClickListener(v->hex.setText(String.format("#%06X",color&0xffffff)));colors.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Tile color").setView(box).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            String value=hex.getText().toString().trim();if(!value.matches("#[0-9a-fA-F]{6}")){hex.setError("Enter #RRGGBB");return;}
            cfg.tileColor=0xff000000|Integer.parseInt(value.substring(1),16);save("tile19",cfg.tileColor);dialog.dismiss();
        }));dialog.show();
    }
    void accentDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding((int)(20*dp),0,(int)(20*dp),0);
        EditText hex=new EditText(this);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setText(String.format("#%06X",cfg.accentColor&0xffffff));box.addView(hex);
        LinearLayout colors=new LinearLayout(this);box.addView(colors);
        for(int color:new int[]{0xff575c68,0xff315ee8,0xff8843b5,0xff16745c,0xffac4934}){
            Button b=new Button(this);b.setText("●");b.setTextColor(color);b.setOnClickListener(v->hex.setText(String.format("#%06X",color&0xffffff)));colors.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Accent color").setView(box).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            String value=hex.getText().toString().trim();if(!value.matches("#[0-9a-fA-F]{6}")){hex.setError("Enter #RRGGBB");return;}
            cfg.accentColor=Color.parseColor(value);save("accent19",cfg.accentColor);dialog.dismiss();
        }));dialog.show();
    }
    void huePicker(LinearLayout col,String title,boolean primary){
        col.addView(text(title,14,0xFFCFCFE0));
        col.addView(new View(this){
            final Paint p=new Paint(3);float hue=primary?cfg.hue1:cfg.hue2;
            protected void onDraw(Canvas c){
                int[] colors=new int[7];for(int i=0;i<7;i++)colors[i]=Color.HSVToColor(new float[]{i*60,1,1});
                float pad=12*dp,y=getHeight()/2f;
                p.setShader(new LinearGradient(pad,0,getWidth()-pad,0,colors,null,Shader.TileMode.CLAMP));c.drawRoundRect(pad,y-12*dp,getWidth()-pad,y+12*dp,12*dp,12*dp,p);p.setShader(null);
                float x=pad+(getWidth()-2*pad)*hue/360;p.setColor(Color.WHITE);c.drawCircle(x,y,16*dp,p);p.setColor(Color.HSVToColor(new float[]{hue,1,1}));c.drawCircle(x,y,12*dp,p);
            }
            public boolean onTouchEvent(MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP){
                    getParent().requestDisallowInterceptTouchEvent(true);hue=Math.max(0,Math.min(360,(e.getX()-12*dp)/Math.max(1,getWidth()-24*dp)*360));
                    if(primary){cfg.hue1=(int)hue;save("hue1",(int)hue);}else{cfg.hue2=(int)hue;save("hue2",(int)hue);}invalidate();
                    if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
                }return true;
            }
            public boolean performClick(){super.performClick();return true;}
        },new LinearLayout.LayoutParams(-1,(int)(56*dp)));
    }

    private void updateEffectLabel() {
        if(selectedEffect!=null)selectedEffect.setText(Config.EFFECTS[cfg.effect]);
    }
    private void selectEffect(int effect) {
        cfg.effect=effect;save("tapFx6",effect);updateEffectLabel();
    }
    private void preset(int fx,int colors,int hue1,int hue2,int duration,int size) {
        sp.edit().putInt("tapFx6",fx).putInt("colorMode",colors).putInt("hue1",hue1).putInt("hue2",hue2)
            .putInt("duration3",duration).putInt("opacity3",90).putInt("sat",100)
            .putInt("size",size).apply();
        recreate();
    }
    private void showCatalog() {
        new AlertDialog.Builder(this).setTitle("Modern animations")
            .setSingleChoiceItems(Config.EFFECTS,cfg.effect,(dialog,index) -> {
                selectEffect(index);dialog.dismiss();
            }).setNegativeButton("Cancel",null).show();
    }

    // ---------- persistence ----------
    private void save(String k, int v) { sp.edit().putInt(k, v).apply(); preview.changed(); }
    private void save(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); preview.changed(); }

    // ---------- tiny UI helpers ----------
    interface IntCb { void on(int v); }
    interface BoolCb { void on(boolean v); }

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setPadding(0, (int) (4 * dp), 0, (int) (4 * dp));
        return t;
    }
    private void section(LinearLayout col, String s) {
        TextView t = text(s.toUpperCase(), 13, 0xFFB388FF);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, (int) (20 * dp), 0, (int) (4 * dp));
        col.addView(t);
    }
    private void toggle(LinearLayout col, String label, boolean val, BoolCb cb) {
        Switch s = new Switch(this);
        s.setText(label); s.setTextColor(0xFFFFFFFF); s.setTextSize(16);
        s.setChecked(val);
        s.setPadding(0, (int) (8 * dp), 0, (int) (8 * dp));
        s.setOnCheckedChangeListener((b, c) -> cb.on(c));
        col.addView(s);
    }
    private void choice(LinearLayout col, String label, String[] items, int val, IntCb cb) {
        col.addView(text(label, 14, 0xFFCFCFE0));
        Spinner sp = new Spinner(this, Spinner.MODE_DIALOG);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override public View getView(int pos, View cv, ViewGroup parent) {
                TextView t = (TextView) super.getView(pos, cv, parent);
                t.setTextColor(0xFFFFFFFF); t.setTextSize(16);
                return t;
            }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(val);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) { cb.on(pos); }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        col.addView(sp);
    }
    private void slider(LinearLayout col, String label, int min, int max, int val, String unit, IntCb cb) {
        TextView t = text(label + ": " + val + unit, 14, 0xFFCFCFE0);
        col.addView(t);
        SeekBar s = new SeekBar(this);
        s.setMax(max - min);
        s.setProgress(val - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int pr, boolean u) {
                t.setText(label + ": " + (pr + min) + unit);
                if (u) cb.on(pr + min);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        col.addView(s);
    }

    @Override protected void onResume() { super.onResume(); if (preview != null) preview.running = true; if (preview != null) preview.invalidate(); }
    @Override protected void onPause() { if (preview != null) { preview.running = false; preview.removeCallbacks(preview.tick); } super.onPause(); }
    @Override protected void onDestroy() { if (preview != null) preview.fx.dispose(); super.onDestroy(); }

    /** Native background draw ordering, with no key mask or pixel extraction. */
    private final class Preview extends View {
        final Fx fx = new Fx(dp);
        
        final EffectSurface surface = new EffectSurface();
        final Paint key = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
        final String[] rows = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
        final RectF r = new RectF(), play = new RectF();
        final Runnable tick = new Runnable() { @Override public void run() { if (running && isShown()) invalidate(); } };
        boolean running = true;
        long lastAuto;
        int demo = 0;

        Preview(Context c) {
            super(c);
            fx.cfg = cfg;
            key.setColor(0xFF33333F);
            label.setColor(0xFFF1EEF8); label.setTextAlign(Paint.Align.CENTER); label.setTextSize(14 * dp);
            GradientDrawable bg = new GradientDrawable(); bg.setColor(0xFF181821); bg.setCornerRadius(14 * dp);
            setBackground(bg); setClipToOutline(true);
            setContentDescription("Tap keyboard preview to test the animation");
        }
        void changed() { fx.cfg = cfg; fx.clear(); lastAuto=0; invalidate(); }
        @Override protected void onSizeChanged(int w,int h,int ow,int oh) { play.set(0,0,w,h); }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int a=e.getActionMasked();
            long now=SystemClock.uptimeMillis();
            if(a==MotionEvent.ACTION_CANCEL||e.getPointerCount()!=1){fx.glide.points.clear();}
            else if(cfg.trailStyle>0){
                if(a==MotionEvent.ACTION_DOWN)fx.glide.points.begin(e.getX(),e.getY(),now);
                else if(a==MotionEvent.ACTION_MOVE){
                    if(play.contains(e.getX(),e.getY())){fx.glide.points.move(e.getX(),e.getY(),now,8*dp);getParent().requestDisallowInterceptTouchEvent(true);}else fx.glide.points.end();
                }else if(a==MotionEvent.ACTION_UP)fx.glide.points.end();
            }
            invalidate();
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
                int i=e.getActionIndex(); lastAuto=SystemClock.uptimeMillis();
                tapKey(e.getX(i),e.getY(i),lastAuto); invalidate();
            }
            if(a==MotionEvent.ACTION_UP) performClick();
            return true;
        }
        void tapKey(float px,float py,long now) {
            float w=getWidth(),h=getHeight(),gap=5*dp,rowH=(h-gap*5)/4f,kw=(w-gap*11)/10f;
            for(int ri=0;ri<rows.length;ri++) {
                float x=(w-(rows[ri].length()*kw+(rows[ri].length()-1)*gap))/2f,y=gap+ri*(rowH+gap);
                for(int k=0;k<rows[ri].length();k++,x+=kw+gap) {
                    if(px>=x && px<x+kw && py>=y && py<y+rowH) {
                        fx.tap(x+kw/2,y+rowH/2,play,now);
                        if(cfg.sideStyle>0)fx.side.tapParent(x+kw/2,(int)y,now,x+kw/2,(int)y);
                        return;
                    }
                }
            }
            float top=gap+3*(rowH+gap);
            if(px>=w*.24f && px<w*.76f && py>=top && py<h-gap){
                fx.tap(w*.5f,(top+h-gap)/2,play,now);
                if(cfg.sideStyle>0)fx.side.tapParent(w*.5f,(int)top,now,w*.5f,(int)top);
            }
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override protected void onDraw(Canvas c) {
            long now=SystemClock.uptimeMillis(); float w=getWidth(), h=getHeight();
            KeyStyle.configure(cfg,getContext());
            if (running && now-lastAuto>Math.max(1700,cfg.duration+500)) {
                lastAuto=now; demo++;
                float gap=5*dp, rh=(h-gap*5)/4f, kw=(w-gap*11)/10f;
                tapKey(gap+(demo%10)*(kw+gap)+kw/2, gap+rh/2, now);
            }
            int sv=c.save(); c.clipRect(play);
            try {
                if(cfg.glass||cfg.layer==0)surface.draw(c,fx,play,now);
                float gap=5*dp, rowH=(h-gap*5)/4f, kw=(w-gap*11)/10f;
                for(int ri=0;ri<rows.length;ri++) {
                    String row=rows[ri]; float x=(w-(row.length()*kw+(row.length()-1)*gap))/2f;
                    float y=gap+ri*(rowH+gap);
                    for(int k=0;k<row.length();k++) {
                        r.set(x,y,x+kw,y+rowH);
                        drawCap(c,r);
                        drawLegend(c,String.valueOf(row.charAt(k)),r,ri==0?String.valueOf((k+1)%10):null);
                        x+=kw+gap;
                    }
                }
                r.set(w*.24f,gap+3*(rowH+gap),w*.76f,h-gap);
                drawCap(c,r); drawLegend(c,"space",r,null);
                if(cfg.layer==1&&!cfg.glass)surface.draw(c,fx,play,now);
                if(cfg.sideStyle>0)fx.side.draw(c,cfg,play,dp,now);
                fx.glide.draw(c,cfg,dp,now);
                if(fx.shaderIssue()!=null){label.setTextSize(10*dp);c.drawText("Shader unavailable — effects paused",w/2,14*dp,label);label.setTextSize(14*dp);}
            } finally { c.restoreToCount(sv); }
            boolean active=fx.active(now);
            removeCallbacks(tick);
            if(running && isShown()) { if(active)postOnAnimation(tick);else postDelayed(tick,250); }
        }
        void drawCap(Canvas c,RectF rect){
            if(cfg.glass){ /* Tile and glyph hole are drawn together in drawLegend. */ }
            else c.drawRoundRect(rect,6*dp,6*dp,key);
            KeyStyle.configure(cfg,getContext());
            label.setTypeface(cfg.glass?Typography.face:Typeface.DEFAULT);label.setTextSize(14*dp*(cfg.glass?cfg.letterSize/100f:1));
            label.clearShadowLayer();label.setShader(null);
        }
        void drawLegend(Canvas c,String text,RectF rect,String hint){
            boolean isSpace=text.equals("space");
            if(isSpace){text="space";}
            float baseline=rect.centerY()-(label.ascent()+label.descent())/2;
            if(cfg.glass){
                float typical=(getWidth()-55*dp)/10f;
                float size=TileGeometry.diameter(rect.width(),rect.height(),typical,dp,cfg.tileScale);
                boolean single=text.codePointCount(0,text.length())==1;
                float tw=single?size:rect.width()*.86f;
                RectF tile=new RectF(rect.centerX()-tw/2,rect.centerY()-size/2,rect.centerX()+tw/2,rect.centerY()+size/2);
                // row-limited sweep preview
                int tileCol=cfg.tileColor!=0?cfg.tileColor:0xfff5f5f3;
                int accent=cfg.accentColor!=0?cfg.accentColor:0xffa6f0c6;
                int finalTileCol=tileCol;
                // simulate sweep across middle row
                float sweepX=play.left+play.width()*0.6f;
                float keyCx=rect.centerX();
                float rowY=play.height()*0.5f;
                boolean inActiveRow=Math.abs(rect.centerY()-rowY)<40*dp;
                if(inActiveRow && keyCx<=sweepX){
                    float fade=Math.max(0,1-(sweepX-keyCx)/(360*dp));
                    finalTileCol=StudioTiles.blend(tileCol,accent,0.22f+0.38f*fade);
                }
                // v32 preview: stock dark keys, border only during wave
                Paint stock=new Paint(Paint.ANTI_ALIAS_FLAG);stock.setColor(0xFF3A3A42);c.drawRoundRect(tile,12*dp,12*dp,stock);
                // simulate wave in middle row
                float sweepProg=0.5f; // for preview
                float originX=play.centerX();
                float radius=play.width()*0.4f;
                float dist=Math.abs(rect.centerX()-originX);
                boolean inRow=Math.abs(rect.centerY()-play.centerY())<40*dp;
                boolean inWave=inRow && dist<=radius;
                int finalCol=inWave?0xffffffff:0xffe8e8ec;
                if(inWave){
                    Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);glow.setColor(finalCol);glow.setAlpha(90);glow.setStyle(Paint.Style.STROKE);glow.setStrokeWidth(6*dp);
                    c.drawRoundRect(tile,12*dp,12*dp,glow);
                    Paint rim2=new Paint(Paint.ANTI_ALIAS_FLAG);rim2.setColor(finalCol);rim2.setStyle(Paint.Style.STROKE);rim2.setStrokeWidth(2.4f*dp);
                    RectF ins=new RectF(tile);ins.inset(1.2f*dp,1.2f*dp);c.drawRoundRect(ins,12*dp,12*dp,rim2);
                }
                if(isSpace){return;}
                Paint leg=new Paint(label);leg.setColor(StudioTiles.legendColorFor(finalTileCol));leg.setTextAlign(Paint.Align.CENTER);leg.setShader(null);leg.clearShadowLayer();
                leg.setTypeface(Typography.face);
                float twm=leg.measureText(text);float maxW=tile.width()*(single?.56f:.84f);if(twm>maxW)leg.setTextSize(leg.getTextSize()*maxW/twm);
                float base=tile.centerY()-(leg.ascent()+leg.descent())/2;
                c.drawText(text,tile.centerX(),base,leg);
                if(hint!=null){
                    Paint hp=new Paint(Paint.ANTI_ALIAS_FLAG);hp.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));hp.setColor(accent);hp.setTextSize(tile.height()*.20f);hp.setTextAlign(Paint.Align.CENTER);
                    c.drawText(hint,tile.centerX()+tile.width()*.22f,tile.top+tile.height()*.25f-(hp.ascent()+hp.descent())/2,hp);
                }
            }else c.drawText(text,rect.centerX(),baseline,label);
        }
        @Override protected void onDetachedFromWindow() { removeCallbacks(tick); surface.release();super.onDetachedFromWindow(); }
    }
}
