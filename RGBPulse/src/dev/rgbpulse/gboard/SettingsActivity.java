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
import java.util.Map;

/** One effect, three controls, and an explicit boundary between draft and live settings. */
public final class SettingsActivity extends Activity {
    private SharedPreferences applied, draft;
    private Config cfg;
    private float dp;
    private boolean saving;
    private int confirmationChecks;
    private final android.os.Handler confirmationHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable confirmationTick=()->checkConfirmation();
    private TextView state, shaderStatus;
    private View backgroundPalette;
    private Button apply;
    private Preview preview;
    private LinearLayout controls;
    private static final int BG=0xff101214, CARD=0xff1b1e21, INK=0xfff1f2ef, MUTED=0xffa2aaa9;

    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        dp=getResources().getDisplayMetrics().density;
        applied=getSharedPreferences(Config.PREFS,Context.MODE_PRIVATE);
        draft=getSharedPreferences("settings_draft",Context.MODE_PRIVATE);
        if(saved==null){copySettings(applied.getAll(),draft).commit();prepareDraft();}
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        render();
    }
    // Migration is staged privately. Merely opening this screen never changes Gboard.
    private void prepareDraft(){
        // Seed only absent values; opening settings must never lower saved strength.
        if(!draft.contains("duration3"))draft.edit().putInt("duration3",720).apply();
        draft.edit().putBoolean("refined37",true)
            .putBoolean("ripple40",Config.flag(draft,"ripple40",Config.flag(draft,"enabled",false)))
            .putInt("trail19",0)
            .putInt("layer4",0).putInt("font11",0).putBoolean("bold9",false)
            .putInt("letterSize9",100).apply();
    }
    private void render(){
        if(preview!=null){preview.stop();preview.fx.dispose();}
        cfg=Config.from(draft);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(BG);
        shell.setFitsSystemWindows(true);shell.setFocusableInTouchMode(true);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        controls=new LinearLayout(this);controls.setOrientation(1);controls.setPadding(px(22),px(20),px(22),px(24));
        scroll.addView(controls);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView eyebrow=text("RGB PULSE  /  45",11,MUTED);eyebrow.setLetterSpacing(.16f);controls.addView(eyebrow);
        TextView title=text("Quiet by design.",30,INK);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);controls.addView(title);
        controls.addView(text("Smooth ripples. Optional background light.",14,MUTED));

        LinearLayout demo=card(controls);
        LinearLayout caption=new LinearLayout(this);caption.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("REFINED RIPPLE",11,INK);label.setLetterSpacing(.12f);
        caption.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        caption.addView(text("TAP TO TEST",10,MUTED));demo.addView(caption);
        preview=new Preview();demo.addView(preview,new LinearLayout.LayoutParams(-1,px(194)));
        demo.addView(text("Preview shows your draft. It responds only to touch and is not a capture of Gboard.",12,MUTED));
        shaderStatus=text("",12,MUTED);shaderStatus.setVisibility(View.GONE);demo.addView(shaderStatus);

        LinearLayout keyboardTest=card(controls);
        keyboardTest.addView(text("Test your keyboard",17,INK));
        keyboardTest.addView(text("Tap below to open your actual keyboard. Gboard uses your last applied settings, not the draft above. Typed text is not saved by this app.",12,MUTED));
        EditText test=new EditText(this);test.setHint("Type here with Gboard…");test.setTextColor(INK);test.setHintTextColor(MUTED);
        test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        test.setMinLines(2);test.setMaxLines(4);test.setSaveEnabled(false);
        test.setContentDescription("Test your real keyboard with applied settings");keyboardTest.addView(test);

        LinearLayout settings=card(controls);
        Switch enabled=new Switch(this);enabled.setText("Enable ripple");enabled.setTextColor(INK);
        enabled.setTextSize(17);enabled.setMinHeight(px(52));enabled.setChecked(cfg.ripple);
        enabled.setOnCheckedChangeListener((b,value)->{draft.edit().putBoolean("ripple40",value).apply();changed();});settings.addView(enabled);
        settings.addView(text("Constant-speed color travels across the tapped row. Letter size, position and font stay intact.",13,MUTED));
        slider(settings,"Ripple strength",15,100,cfg.rippleOpacity,false);
        Switch tiles=new Switch(this);tiles.setText("Hide all key tiles");tiles.setTextColor(INK);tiles.setMinHeight(px(48));tiles.setChecked(cfg.hideTiles);
        tiles.setOnCheckedChangeListener((b,value)->{draft.edit().putBoolean("hideTiles45",value).apply();changed();});settings.addView(tiles);
        settings.addView(text("Removes native key backgrounds, module faces and key outlines while either effect is on. Ripple animates letters without boxes. With both effects off, your original Gboard theme is restored.",12,MUTED));
        slider(settings,"Duration",400,1100,Config.clamp(cfg.duration,400,1100),true);
        Button rippleStyle=button("Ripple · "+Config.RIPPLES[cfg.rippleStyle],false);
        rippleStyle.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Ripple effect")
            .setSingleChoiceItems(Config.RIPPLES,cfg.rippleStyle,(dialog,index)->{
                draft.edit().putInt("rippleStyle42",index).apply();rippleStyle.setText("Ripple · "+Config.RIPPLES[index]);changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());settings.addView(rippleStyle);
        Button appearance=button("Ripple colors & border  +",false);controls.addView(appearance);
        LinearLayout appearancePanel=card(controls);appearancePanel.setVisibility(View.GONE);
        appearance.setOnClickListener(v->{boolean open=appearancePanel.getVisibility()!=View.VISIBLE;
            appearancePanel.setVisibility(open?View.VISIBLE:View.GONE);appearance.setText(open?"Ripple colors & border  −":"Ripple colors & border  +");});
        appearancePanel.addView(text("Active = during the wave. Inactive = between taps. Letters and borders share a gradual fade, with independent colors. Choose colors with readable contrast against your key tiles.",12,MUTED));
        borderWidth(appearancePanel);
        colorControl(appearancePanel,"Border · active","rippleActive42",cfg.rippleActive);
        colorControl(appearancePanel,"Border · inactive","rippleInactive42",cfg.rippleInactive);
        colorControl(appearancePanel,"Letters · active","letterActive42",cfg.letterActive);
        colorControl(appearancePanel,"Letters · inactive","letterInactive42",cfg.letterInactive);


        Button backgrounds=button("Background animations  +",false);controls.addView(backgrounds);
        LinearLayout backgroundPanel=card(controls);backgroundPanel.setVisibility(View.GONE);
        backgrounds.setOnClickListener(v->{boolean open=backgroundPanel.getVisibility()!=View.VISIBLE;
            backgroundPanel.setVisibility(open?View.VISIBLE:View.GONE);backgrounds.setText(open?"Background animations  −":"Background animations  +");});
        Switch backgroundOn=new Switch(this);backgroundOn.setText("Enable background animations");backgroundOn.setTextColor(INK);
        backgroundOn.setMinHeight(px(48));backgroundOn.setChecked(cfg.tapEffects);
        backgroundOn.setOnCheckedChangeListener((b,value)->{cfg.tapEffects=value;draft.edit().putBoolean("tapEffects36",value).apply();changed();});backgroundPanel.addView(backgroundOn);
        backgroundPanel.addView(text("Optional tap effects behind the letters. Uses its own brightness and the shared duration. Works independently of Enable ripple. Off means no background effects; choosing a style never enables it.",12,MUTED));
        backgroundStrength(backgroundPanel);
        Button effect=button("Animation · "+Config.EFFECTS[cfg.effect],false);
        effect.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Background animation · distinct motion")
            .setSingleChoiceItems(Config.EFFECTS,cfg.effect,(dialog,index)->{
                cfg.effect=index;draft.edit().putInt("tapFx6",index).apply();effect.setText("Animation · "+Config.EFFECTS[index]);changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());backgroundPanel.addView(effect);
        Button colors=button("Color mode · "+Config.COLORS[cfg.colorMode],false);
        colors.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Background color mode")
            .setSingleChoiceItems(Config.COLORS,cfg.colorMode,(dialog,index)->{
                cfg.colorMode=index;draft.edit().putInt("colorMode",index).apply();colors.setText("Color mode · "+Config.COLORS[index]);changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());backgroundPanel.addView(colors);
        backgroundPalette=new View(this);backgroundPalette.setContentDescription("Background palette preview");backgroundPanel.addView(backgroundPalette,new LinearLayout.LayoutParams(-1,px(24)));updatePalette();
        hueControl(backgroundPanel,"Primary hue","hue1",true);
        hueControl(backgroundPanel,"Secondary hue","hue2",false);

        Button help=button("Setup & troubleshooting  +",false);controls.addView(help);
        LinearLayout details=card(controls);details.setVisibility(View.GONE);
        help.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);help.setText(open?"Setup & troubleshooting  −":"Setup & troubleshooting  +");});
        details.addView(text("Settings are stored privately. Apply sends a revision to Gboard; only a matching reply is shown as confirmed. Enable this module in LSPosed / Vector and scope Gboard. After upgrading from an older build, reboot once to unload the old hooks.",13,MUTED));
        details.addView(text("Android 13+ and LSPosed / Vector are required. Press Apply after editing. If Gboard does not refresh, close and reopen it manually. Save only leaves Gboard running. Save & restart requests root only after confirmation. Font replacement and gesture trails remain off.",13,MUTED));
        details.addView(text("Background animations remain optional and work together with the refined ripple. Font replacement and gesture trails remain off. Gboard is unchanged until Apply.",13,MUTED));
        Switch logs=new Switch(this);logs.setText("Detailed layout logs (no drawing)");logs.setTextColor(INK);logs.setMinHeight(px(48));logs.setChecked(cfg.debug);
        logs.setOnCheckedChangeListener((b,value)->{cfg.debug=value;draft.edit().putBoolean("debug3",value).apply();changed();});details.addView(logs);
        details.addView(text("If letter colors do not change: enable logs, Apply & restart, then tap a few keys. In LSPosed logs, share only lines beginning RGBPulse legend, plus your Gboard version. These lines contain renderer names and counters, not typed text.",12,MUTED));
        Button emergency=button("Turn everything off & apply",false);
        emergency.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Disable all module effects?")
            .setMessage("Saves both effects off and removes module tiles, overlays and letter tint when Gboard receives it. This does not disable Gboard’s own native key-press animation. No root restart is requested.")
            .setPositiveButton("Disable & apply",(d,w)->{
                draft.edit().putBoolean("refined37",true).putBoolean("ripple40",false).putBoolean("tapEffects36",false)
                    .putBoolean("enabled",false).putBoolean("glass9",false).putBoolean("tiles41",false).putInt("side20",0).putInt("trail19",0).apply();
                render();persist(false);
            }).setNegativeButton("Cancel",null).show());details.addView(emergency);
        Button reset=button("Reset draft",false);reset.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Reset draft?").setMessage("Ripple will be off, with balanced strength and duration. Nothing changes in Gboard until Apply.")
            .setPositiveButton("Reset",(d,w)->{draft.edit().clear().apply();prepareDraft();render();}).setNegativeButton("Cancel",null).show());details.addView(reset);
        controls.addView(text("No font replacement. No automatic demo. Changes require Apply.",12,MUTED));

        LinearLayout footer=new LinearLayout(this);footer.setOrientation(1);footer.setPadding(px(22),px(8),px(22),px(12));footer.setBackgroundColor(BG);
        state=text("",12,MUTED);state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);footer.addView(state);
        LinearLayout actions=new LinearLayout(this);
        Button discard=button("Discard",false);discard.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Discard draft changes?")
            .setMessage("Reload your last applied settings. Gboard will not be changed.")
            .setPositiveButton("Discard",(d,w)->{copySettings(applied.getAll(),draft).commit();prepareDraft();render();}).setNegativeButton("Cancel",null).show());
        actions.addView(discard,new LinearLayout.LayoutParams(0,px(52),1));
        apply=button("Apply / restart",true);apply.setOnClickListener(v->applySettings());
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,px(52),2);ap.leftMargin=px(10);actions.addView(apply,ap);
        footer.addView(actions);shell.addView(footer);setContentView(shell);updateState();
    }
    private void slider(LinearLayout parent,String name,int min,int max,int value,boolean duration){
        TextView label=text(name+"  ·  "+value+(duration?" ms":"%"),14,INK);parent.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(value-min);bar.setContentDescription(name);bar.setMinimumHeight(px(48));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffd7e7dd));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(INK));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean user){if(!user)return;int n=min+progress;
                label.setText(name+"  ·  "+n+(duration?" ms":"%"));
                if(duration){cfg.duration=n;draft.edit().putInt("duration3",n).apply();}
                else{cfg.rippleOpacity=n;draft.edit().putInt("rippleStrength41",n).apply();}changed();}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });parent.addView(bar);
    }
    private void borderWidth(LinearLayout panel){
        TextView value=text("Border width · "+cfg.borderTenths/10f+" dp",14,INK);panel.addView(value);
        SeekBar bar=new SeekBar(this);bar.setMax(25);bar.setProgress(cfg.borderTenths-5);bar.setMinimumHeight(px(48));bar.setContentDescription("Border width");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean user){if(!user)return;
                value.setText("Border width · "+(n+5)/10f+" dp");draft.edit().putInt("borderWidth42",n+5).apply();changed();}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
        });panel.addView(bar);
    }
    private String hex(int color){return String.format(java.util.Locale.ROOT,"#%06X",color&0xffffff);}
    private void swatch(TextView view,int color){
        GradientDrawable chip=background(color,8);chip.setSize(px(28),px(28));chip.setStroke(px(1),0xff818781);
        view.setCompoundDrawablesWithIntrinsicBounds(chip,null,null,null);view.setCompoundDrawablePadding(px(12));
    }
    private void colorControl(LinearLayout panel,String title,String key,int fallback){
        Button select=button(title+"  "+hex(Config.number(draft,key,fallback)),false);swatch(select,Config.number(draft,key,fallback));
        select.setOnClickListener(v->colorDialog(title,key,fallback,select));panel.addView(select);
    }
    private void colorDialog(String title,String key,int fallback,Button select){
        LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(px(22),px(12),px(22),px(12));
        TextView sample=text("Aa  ·  Color preview",22,INK);sample.setBackground(background(CARD,12));sample.setPadding(px(16),px(14),px(16),px(14));content.addView(sample);
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("#RRGGBB");input.setText(hex(Config.number(draft,key,fallback)));content.addView(input);
        LinearLayout palette=new LinearLayout(this);content.addView(palette);
        for(int color:new int[]{0xffd0bcff,0xffa8c7fa,0xffa8dab5,0xffffb4ab,0xfff5efff,0xff49454f}){
            Button chip=button("●",false);chip.setTextColor(color);chip.setContentDescription("Use "+hex(color));
            chip.setOnClickListener(v->input.setText(hex(color)));palette.addView(chip,new LinearLayout.LayoutParams(0,px(48),1));
        }
        final int[] candidate={Config.number(draft,key,fallback)};sample.setTextColor(candidate[0]);swatch(sample,candidate[0]);
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){
                if(s.toString().matches("#[0-9a-fA-F]{6}")){candidate[0]=Color.parseColor(s.toString());sample.setTextColor(candidate[0]);swatch(sample,candidate[0]);}}
            public void afterTextChanged(android.text.Editable e){}
        });
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(content).setPositiveButton("Use in draft",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            if(!input.getText().toString().matches("#[0-9a-fA-F]{6}")){input.setError("Enter #RRGGBB");return;}
            draft.edit().putInt(key,candidate[0]).apply();select.setText(title+"  "+hex(candidate[0]));swatch(select,candidate[0]);changed();dialog.dismiss();
        }));dialog.show();
    }
    private void updatePalette(){
        if(backgroundPalette==null)return;
        int first=Color.HSVToColor(new float[]{cfg.hue1,cfg.sat/100f,1}),second=Color.HSVToColor(new float[]{cfg.hue2,cfg.sat/100f,1});
        int[] colors;
        if(cfg.colorMode==2)colors=new int[]{first,first};
        else if(cfg.colorMode==3)colors=new int[]{first,second};
        else{colors=new int[7];for(int i=0;i<7;i++)colors[i]=Color.HSVToColor(new float[]{i*60,cfg.sat/100f,1});}
        GradientDrawable palette=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);palette.setCornerRadius(px(8));backgroundPalette.setBackground(palette);
    }
    private void backgroundStrength(LinearLayout panel){
        TextView label=text("Background brightness · "+cfg.backgroundOpacity+"%",14,INK);panel.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMax(95);bar.setProgress(cfg.backgroundOpacity-5);bar.setMinimumHeight(px(48));bar.setContentDescription("Background brightness");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean user){if(!user)return;
                label.setText("Background brightness · "+(n+5)+"%");draft.edit().putInt("backgroundStrength41",n+5).apply();changed();}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
        });panel.addView(bar);
    }
    private void hueControl(LinearLayout panel,String title,String key,boolean primary){
        TextView value=text(title+" · "+(primary?cfg.hue1:cfg.hue2)+"°",13,INK);panel.addView(value);
        swatch(value,Color.HSVToColor(new float[]{primary?cfg.hue1:cfg.hue2,cfg.sat/100f,1}));
        SeekBar hue=new SeekBar(this);hue.setMax(360);hue.setProgress(primary?cfg.hue1:cfg.hue2);hue.setMinimumHeight(px(48));hue.setContentDescription(title);
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){if(!user)return;
                if(primary)cfg.hue1=progress;else cfg.hue2=progress;
                value.setText(title+" · "+progress+"°");swatch(value,Color.HSVToColor(new float[]{progress,cfg.sat/100f,1}));draft.edit().putInt(key,progress).apply();changed();}
            public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}
        });panel.addView(hue);
    }
    private void changed(){
        cfg=Config.from(draft);preview.fx.cfg=cfg;updatePalette();
        preview.fx.clear();preview.clearFades();preview.invalidate();updateState();
    }
    private void updateState(){
        boolean dirty=!draft.getAll().equals(applied.getAll());
        String revision=Config.string(applied,SettingsContract.REVISION,"");
        String received=Config.string(getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE),SettingsContract.REVISION,"");
        String savedState=RevisionGate.matches(revision,received)?
            (Config.from(applied).enabled?"Gboard confirmed these settings":"Gboard confirmed: all module effects off"):
            "Saved locally · NOT confirmed by Gboard";
        state.setText(saving?"Saving…":dirty?"Draft only · press Apply to change Gboard":savedState);
        apply.setEnabled(!saving);apply.setAlpha(!saving?1f:.45f);
    }
    private static SharedPreferences.Editor copySettings(Map<String,?> values,SharedPreferences target){return SettingsStore.copy(values,target);}
    private void applySettings(){
        if(saving)return;
        new AlertDialog.Builder(this).setTitle("Apply settings / restart Gboard?")
            .setMessage("Save only keeps Gboard running. Save & restart requests root to force-stop Gboard after saving. The keyboard will close; tap a text field to reopen it. Keep a backup keyboard enabled. No restart happens without this confirmation.")
            .setPositiveButton("Save & restart",(d,w)->persist(true))
            .setNeutralButton("Save only",(d,w)->persist(false))
            .setNegativeButton("Cancel",null).show();
    }
    private void persist(boolean restart){
        if(saving)return;
        final Map<String,Object> snapshot=new java.util.HashMap<>(draft.getAll());
        final String revision=java.util.UUID.randomUUID().toString();snapshot.put(SettingsContract.REVISION,revision);
        saving=true;updateState();
        state.setText(restart?"Saving, then waiting for root approval…":"Saving…");
        new Thread(()->{
            boolean saved;
            try{saved=SettingsStore.save(snapshot,applied);}catch(RuntimeException e){saved=false;}
            final boolean ok=saved;
            if(ok){
                draft.edit().putString(SettingsContract.REVISION,revision).apply();
                try{sendBroadcast(new android.content.Intent(SettingsContract.ACTION).setPackage(SettingsContract.GBOARD));}
                catch(RuntimeException e){android.util.Log.w("RGBPulse","Gboard notification failed; reopen keyboard to retry",e);}
            }
            boolean stopped=ok&&restart&&GboardRestart.stop();
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;saving=false;updateState();
                if(!ok){state.setText("Save failed. Gboard was not stopped. Tap Apply to retry.");return;}
                confirmationChecks=0;confirmationHandler.removeCallbacks(confirmationTick);confirmationHandler.postDelayed(confirmationTick,400);
                if(restart){
                    state.setText(stopped?"Saved · Gboard stopped. Tap the test field to reopen it.":"Saved · automatic restart failed or root was denied.");
                    if(!stopped)new AlertDialog.Builder(this).setTitle("Restart Gboard manually")
                        .setMessage("Settings were saved locally; receipt is checked separately. Root was unavailable, denied or timed out. You can open Gboard’s app settings and tap Force stop yourself.")
                        .setPositiveButton("Open app settings",(d,w)->openGboardSettings()).setNegativeButton("Not now",null).show();
                }
                if(!draft.getAll().equals(snapshot))state.append(" Newer edits still need Apply.");
            });
        },"Apply ripple").start();
    }
    private void checkConfirmation(){
        if(isFinishing()||isDestroyed()||saving)return;
        updateState();
        String revision=Config.string(applied,SettingsContract.REVISION,"");
        String received=Config.string(getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE),SettingsContract.REVISION,"");
        if(RevisionGate.matches(revision,received))return;
        if(++confirmationChecks<30)confirmationHandler.postDelayed(confirmationTick,400);
        else state.append(". Reopen Gboard. If still unconfirmed, check module scope and reboot.");
    }
    private void openGboardSettings(){
        try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:com.google.android.inputmethod.latin")));}
        catch(RuntimeException e){Toast.makeText(this,"Open Android Settings > Apps > Gboard > Force stop.",Toast.LENGTH_LONG).show();}
    }
    @Override public void onBackPressed(){
        if(saving){Toast.makeText(this,"Please wait for saving to finish.",Toast.LENGTH_SHORT).show();return;}
        if(!draft.getAll().equals(applied.getAll()))new AlertDialog.Builder(this).setTitle("Leave without applying?")
            .setMessage("Gboard keeps its applied settings. Draft changes are discarded on your next launch.")
            .setPositiveButton("Leave",(d,w)->finish()).setNegativeButton("Keep editing",null).show();
        else super.onBackPressed();
    }
    private int px(float n){return Math.round(n*dp);}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,px(5),0,px(5));return t;}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(px(radius));return d;}
    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(px(16),px(14),px(16),px(14));c.setBackground(background(CARD,20));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=px(18);parent.addView(c,lp);return c;}
    private Button button(String name,boolean primary){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?BG:INK);b.setMinHeight(px(48));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?0xffd7e7dd:0xff292e31));return b;}
    @Override protected void onPause(){confirmationHandler.removeCallbacks(confirmationTick);if(preview!=null)preview.stop();super.onPause();}
    @Override protected void onResume(){super.onResume();if(preview!=null){preview.running=true;preview.invalidate();}confirmationChecks=0;confirmationHandler.post(confirmationTick);}
    @Override protected void onDestroy(){confirmationHandler.removeCallbacks(confirmationTick);if(preview!=null)preview.fx.dispose();super.onDestroy();}

    private final class Preview extends View {
        final Fx fx=new Fx(dp);
        final RipplePaint ripple=new RipplePaint();
        final Paint key=new Paint(3),label=new Paint(3);
        final RectF play=new RectF(),rect=new RectF();
        final String[][] rows={{"q","w","e","r","t","y","u","i","o","p"},{"a","s","d","f","g","h","j","k","l"},{"⇧","z","x","c","v","b","n","m","⌫"},{"?123","space","↵"}};
        final BorderFade[][] fades=new BorderFade[rows.length][];
        final Runnable tick=()->invalidate();boolean running=true;
        Preview(){super(SettingsActivity.this);fx.cfg=cfg;
            for(int row=0;row<rows.length;row++){fades[row]=new BorderFade[rows[row].length];for(int k=0;k<rows[row].length;k++)fades[row][k]=new BorderFade();}setContentDescription("Tap a key to preview your chosen ripple and background effects.");label.setTextAlign(Paint.Align.CENTER);label.setColor(INK);label.setTypeface(Typeface.DEFAULT);}
        void clearFades(){for(BorderFade[] row:fades)for(BorderFade fade:row)fade.clear();}
        void stop(){running=false;removeCallbacks(tick);fx.clear();clearFades();}
        protected void onSizeChanged(int w,int h,int ow,int oh){play.set(0,0,w,h);fx.clear();clearFades();}
        void keyRect(int row,int k){float gap=4*dp,rh=(getHeight()-5*gap)/4f,kw=(getWidth()-11*gap)/10f;
            float y=gap+row*(rh+gap);
            if(row==3){float left=gap,right=getWidth()-gap,w=right-left;
                if(k==0)rect.set(left,y,left+w*.19f,y+rh);
                else if(k==1)rect.set(left+w*.19f+gap,y,left+w*.81f-gap,y+rh);
                else rect.set(left+w*.81f,y,right,y+rh);
            }else{float start=(getWidth()-(rows[row].length*kw+(rows[row].length-1)*gap))/2;
                rect.set(start+k*(kw+gap),y,start+k*(kw+gap)+kw,y+rh);}
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){
                int pointer=e.getActionIndex();
                if(cfg.enabled)for(int row=0;row<rows.length;row++)for(int k=0;k<rows[row].length;k++){
                    keyRect(row,k);if(rect.contains(e.getX(pointer),e.getY(pointer))){long now=SystemClock.uptimeMillis();fx.tap(rect.centerX(),rect.centerY(),play,now);if(cfg.ripple)fx.side.tap(rect.centerX(),(int)rect.top,now);invalidate();return true;}}
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override protected void onDraw(Canvas canvas){
            long now=SystemClock.uptimeMillis();boolean sideActive=cfg.ripple&&fx.side.active(now,cfg.duration);
            if(sideActive)fx.side.advance(cfg,play,now);
            boolean active=sideActive||fx.active(now);
            int save=canvas.save();canvas.clipRect(play);
            fx.drawFields(canvas,play,now);
            for(int row=0;row<rows.length;row++)for(int k=0;k<rows[row].length;k++){
                keyRect(row,k);key.setColor(0xff303538);if(cfg.tiles)canvas.drawRoundRect(rect,6*dp,6*dp,key);
                float amount=sideActive?SideSweep.glowAt(rect.centerX(),(int)rect.top,dp,true):0;
                BorderFade fade=fades[row][k];
                if(!cfg.ripple)fade.clear();
                amount=fade.advance(amount,now);active|=fade.active();
                if(cfg.ripple&&!cfg.hideTiles)ripple.draw(canvas,rect,dp,amount,cfg.rippleOpacity/100f,6*dp,cfg.rippleActive,cfg.rippleInactive,cfg.borderTenths/10f);
                label.setColor(cfg.ripple?LegendTint.color(INK,amount,cfg.letterInactive,cfg.letterActive):INK);
                label.setTextSize((row==3?11:13)*dp);
                canvas.drawText(rows[row][k],rect.centerX(),rect.centerY()-(label.ascent()+label.descent())/2,label);
            }
            canvas.restoreToCount(save);removeCallbacks(tick);
            if(shaderStatus!=null&&fx.shaderIssue()!=null){shaderStatus.setText("Background shader unavailable on this device; the key ripple still works.");shaderStatus.setVisibility(View.VISIBLE);}
            if(running&&isShown()&&active)postOnAnimation(tick);
        }
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    }
}
