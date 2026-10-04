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

/** Draft-based studio: edit freely, then one button saves everything and restarts Gboard. */
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
    private View secondaryHueRow;
    private Button saveRestart;
    private Preview preview;
    private LinearLayout controls;
    private ScrollView settingsScroll;
    private int selectedPage;
    private LinearLayout[] pages;
    private Button[] pageButtons;
    private boolean previewCollapsed;
    private LifecycleLight previewLight;

    private static final int BG=0xff0c1419, CARD=0xff182329, INK=0xfff1f2ef, MUTED=0xffa2aaa9;

    interface HueSink{void hue(int degrees);}
    interface SvSink{void sv(float s,float v);}

    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        dp=getResources().getDisplayMetrics().density;
        if(saved!=null){selectedPage=Config.clamp(saved.getInt("page46",0),0,2);previewCollapsed=saved.getBoolean("previewCollapsed46",false);}
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
        if(previewLight!=null)previewLight.cancel();
        if(preview!=null){preview.stop();preview.fx.dispose();}
        cfg=Config.from(draft);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(BG);
        shell.setFitsSystemWindows(true);shell.setFocusableInTouchMode(true);
        LinearLayout pinned=new LinearLayout(this);pinned.setOrientation(1);pinned.setPadding(px(18),px(8),px(18),0);shell.addView(pinned);
        TextView brand=text("PULSE STUDIO",11,0xffb9efd4);brand.setLetterSpacing(.18f);pinned.addView(brand);
        TextView title=text("Make every tap yours.",23,INK);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);pinned.addView(title);
        LinearLayout demo=card(pinned);demo.setPadding(px(12),px(4),px(12),px(4));
        LinearLayout caption=new LinearLayout(this);caption.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("YOUR PREVIEW · tap to try",11,MUTED);caption.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button collapse=button(previewCollapsed?"Expand":"Minimize",false);caption.addView(collapse);demo.addView(caption);
        preview=new Preview();preview.setVisibility(previewCollapsed?View.GONE:View.VISIBLE);demo.addView(preview,new LinearLayout.LayoutParams(-1,px(140)));
        previewLight=new LifecycleLight(demo);
        collapse.setOnClickListener(v->{previewCollapsed=!previewCollapsed;preview.setVisibility(previewCollapsed?View.GONE:View.VISIBLE);collapse.setText(previewCollapsed?"Expand":"Minimize");});
        shaderStatus=text("",12,MUTED);shaderStatus.setVisibility(View.GONE);demo.addView(shaderStatus);
        shell.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if(preview==null)return;
            boolean compact=shell.getHeight()<px(500);
            int headerVisibility=compact?View.GONE:View.VISIBLE;
            if(title.getVisibility()!=headerVisibility){title.setVisibility(headerVisibility);brand.setVisibility(headerVisibility);}
            boolean tooShort=shell.getHeight()<px(320);
            int previewVisibility=(previewCollapsed||tooShort)?View.GONE:View.VISIBLE;
            if(preview.getVisibility()!=previewVisibility)preview.setVisibility(previewVisibility);
            int height=Math.max(px(64),Math.min(px(156),(int)(shell.getHeight()*.20f)));
            if(preview.getLayoutParams().height!=height){preview.getLayoutParams().height=height;preview.requestLayout();}
        });
        LinearLayout navigation=new LinearLayout(this);navigation.setPadding(0,px(8),0,px(8));pinned.addView(navigation);
        pageButtons=new Button[3];String[] titles={"Ripple","Touch","Keyboard"};
        for(int i=0;i<3;i++){final int index=i;Button tab=button(titles[i],false);pageButtons[i]=tab;
            tab.setOnClickListener(v->selectPage(index));navigation.addView(tab,new LinearLayout.LayoutParams(0,px(48),1));}
        ScrollView scroll=new ScrollView(this);settingsScroll=scroll;scroll.setFillViewport(true);
        controls=new LinearLayout(this);controls.setOrientation(1);controls.setPadding(px(18),0,px(18),px(24));
        scroll.addView(controls);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        pages=new LinearLayout[3];for(int i=0;i<3;i++){pages[i]=new LinearLayout(this);pages[i].setOrientation(1);controls.addView(pages[i]);}
        LinearLayout ripplePage=pages[0],pulsePage=pages[1],keyboardPage=pages[2];
        ripplePage.addView(text("A little extra, when you want it",20,INK));
        ripplePage.addView(text("Traveling ripple is optional. For an everyday setup, leave this off and use Touch feedback instead.",13,MUTED));
        pulsePage.addView(text("Small details. Better typing.",20,INK));
        pulsePage.addView(text("Key animations and background animations are separate controls — run either, or both at once. No traveling wave needed.",13,MUTED));
        keyboardPage.addView(text("Make yourself at home",20,INK));
        keyboardPage.addView(text("Opening light, closing light, and a place to test the real thing.",13,MUTED));
        LinearLayout keyboardTest=card(keyboardPage);
        keyboardTest.addView(text("Test your keyboard",17,INK));
        keyboardTest.addView(text("Tap below to open your actual keyboard. Gboard uses your last saved settings, not the draft above. Typed text is not saved by this app.",12,MUTED));
        EditText test=new EditText(this);test.setHint("Type here with Gboard…");test.setTextColor(INK);test.setHintTextColor(MUTED);
        test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        test.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffb9efd4));
        if(test.getTextCursorDrawable()!=null)test.getTextCursorDrawable().setTint(0xffb9efd4);
        test.setMinLines(2);test.setMaxLines(4);test.setSaveEnabled(false);
        test.setContentDescription("Test your real keyboard with applied settings");keyboardTest.addView(test);

        LinearLayout settings=card(ripplePage);
        Switch enabled=new Switch(this);enabled.setText("Ripple on tap");enabled.setTextColor(INK);
        enabled.setTextSize(17);enabled.setMinHeight(px(52));enabled.setChecked(cfg.ripple);
        enabled.setOnCheckedChangeListener((b,value)->{draft.edit().putBoolean("ripple40",value).apply();changed();});settings.addView(enabled);
        settings.addView(text("Explore full-keyboard circles, diamonds, crosses and diagonals. One tap, one wave. Your letters never move.",13,MUTED));
        slider(settings,"Ripple strength",15,100,cfg.rippleOpacity,false);
        Switch tiles=new Switch(this);tiles.setText("Hide key fills");tiles.setTextColor(INK);tiles.setMinHeight(px(48));tiles.setChecked(cfg.hideTiles);
        tiles.setOnCheckedChangeListener((b,value)->{draft.edit().putBoolean("hideTiles45",value).apply();changed();});settings.addView(tiles);
        settings.addView(text("Keep the keyboard airy without solid key tiles. Ripple borders stay visible. Turn all lighting off to restore your Gboard theme.",12,MUTED));
        slider(settings,"Duration",150,1100,Config.clamp(cfg.duration,150,1100),true);
        Button rippleStyle=button("Ripple · "+Config.RIPPLES[cfg.rippleStyle],false);
        rippleStyle.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Ripple effect")
            .setSingleChoiceItems(Config.RIPPLES,cfg.rippleStyle,(dialog,index)->{
                draft.edit().putInt("ripplePattern47",index).apply();rippleStyle.setText("Ripple · "+Config.RIPPLES[index]);changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());settings.addView(rippleStyle);
        Button appearance=button("Ripple colors & border  +",false);ripplePage.addView(appearance);
        LinearLayout appearancePanel=card(ripplePage);appearancePanel.setVisibility(View.GONE);
        appearance.setOnClickListener(v->{boolean open=appearancePanel.getVisibility()!=View.VISIBLE;
            appearancePanel.setVisibility(open?View.VISIBLE:View.GONE);appearance.setText(open?"Ripple colors & border  −":"Ripple colors & border  +");});
        appearancePanel.addView(text("Active colors appear as the wave passes. Idle colors show between taps. Choose readable letter colors against your keyboard background.",12,MUTED));
        borderWidth(appearancePanel);
        colorControl(appearancePanel,"Border · active","rippleActive42",cfg.rippleActive);
        colorControl(appearancePanel,"Border · inactive","rippleInactive42",cfg.rippleInactive);
        colorControl(appearancePanel,"Letters · active","letterActive42",cfg.letterActive);
        colorControl(appearancePanel,"Letters · inactive","letterInactive42",cfg.letterInactive);


        Button backgrounds=button("Background animations  +",false);pulsePage.addView(backgrounds);
        LinearLayout backgroundPanel=card(pulsePage);backgrounds.setVisibility(View.GONE);
        backgrounds.setOnClickListener(v->{boolean open=backgroundPanel.getVisibility()!=View.VISIBLE;
            backgroundPanel.setVisibility(open?View.VISIBLE:View.GONE);backgrounds.setText(open?"Background animations  −":"Background animations  +");});
        Switch backgroundOn=new Switch(this);backgroundOn.setText("Key animations");backgroundOn.setTextColor(INK);
        backgroundOn.setMinHeight(px(48));backgroundOn.setChecked(cfg.tapEffects);
        backgroundOn.setOnCheckedChangeListener((b,value)->{cfg.tapEffects=value;draft.edit().putBoolean("tapEffects36",value).apply();changed();});backgroundPanel.addView(backgroundOn);
        backgroundPanel.addView(text("Layered-light responses on the tapped key. Start with Soft press.",12,MUTED));
        final String[] keyEffects=java.util.Arrays.copyOfRange(Config.EFFECTS,0,8);
        TextView effectHint=text(Config.EFFECT_HINTS[cfg.effect],13,MUTED);
        Button effect=button("Key animation · "+Config.EFFECTS[cfg.effect],false);
        effect.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Key animation")
            .setSingleChoiceItems(keyEffects,cfg.effect,(dialog,index)->{
                cfg.effect=index;draft.edit().putInt("pulse47",index).apply();effect.setText("Key animation · "+Config.EFFECTS[index]);effectHint.setText(Config.EFFECT_HINTS[index]);changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());backgroundPanel.addView(effect);backgroundPanel.addView(effectHint);
        Button bg=button("Background · "+(cfg.background==0?"Off":Config.EFFECTS[7+cfg.background]),false);
        TextView bgHint=text(cfg.background==0?"A wide light show behind the keys. It runs independently of key animations — use either, or both at once.":Config.EFFECT_HINTS[7+cfg.background],13,MUTED);
        bg.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Background animation")
            .setSingleChoiceItems(new String[]{"Off","Comet sweep","Nebula bloom"},cfg.background,(dialog,index)->{
                cfg.background=index;draft.edit().putInt("background51",index).apply();
                bg.setText("Background · "+(index==0?"Off":Config.EFFECTS[7+index]));
                bgHint.setText(index==0?"A wide light show behind the keys. It runs independently of key animations — use either, or both at once.":Config.EFFECT_HINTS[7+index]);
                changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());backgroundPanel.addView(bg);backgroundPanel.addView(bgHint);
        backgroundStrength(backgroundPanel);
        slider(backgroundPanel,"Feedback length",120,360,cfg.pulseDuration,true);
        Button colors=button("Color mode · "+Config.COLORS[cfg.colorMode],false);
        colors.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Background color mode")
            .setSingleChoiceItems(Config.COLORS,cfg.colorMode,(dialog,index)->{
                cfg.colorMode=index;draft.edit().putInt("colorMode",index).apply();colors.setText("Color mode · "+Config.COLORS[index]);
                if(secondaryHueRow!=null)secondaryHueRow.setVisibility(index==3?View.VISIBLE:View.GONE);
                changed();dialog.dismiss();
            }).setNegativeButton("Cancel",null).show());backgroundPanel.addView(colors);
        backgroundPalette=new View(this);backgroundPalette.setContentDescription("Background palette preview");backgroundPanel.addView(backgroundPalette,new LinearLayout.LayoutParams(-1,px(24)));updatePalette();
        hueControl(backgroundPanel,"Primary hue","hue1",true);
        secondaryHueRow=hueControl(backgroundPanel,"Secondary hue","hue2",false);
        secondaryHueRow.setVisibility(cfg.colorMode==3?View.VISIBLE:View.GONE);
        Button everyday=button("Try everyday setup",false);backgroundPanel.addView(everyday);
        everyday.setOnClickListener(v->{draft.edit().putBoolean("ripple40",false).putBoolean("tapEffects36",true).putInt("pulse47",7).putInt("pulseDuration47",160).putInt("background51",0).putInt("opening46",0).putInt("closing46",0).apply();render();Toast.makeText(this,"Preview only — tap Save & restart Gboard when you’re ready.",Toast.LENGTH_SHORT).show();});

        LinearLayout transitions=card(keyboardPage);
        transitions.addView(text("Hello & goodbye",18,INK));
        transitions.addView(text("A dramatic reveal or exit, with the keys firmly in place. Closing depends on Android’s dismissal timing and may be cut short.",13,MUTED));
        transitionControl(transitions,"Opening light","opening46",false);
        transitionControl(transitions,"Closing light","closing46",true);
        Button help=button("Setup & troubleshooting  +",false);keyboardPage.addView(help);
        LinearLayout details=card(keyboardPage);details.setVisibility(View.GONE);
        help.setOnClickListener(v->{boolean open=details.getVisibility()!=View.VISIBLE;details.setVisibility(open?View.VISIBLE:View.GONE);help.setText(open?"Setup & troubleshooting  −":"Setup & troubleshooting  +");});
        details.addView(text("Pulse Studio · version 1.1.0\nSettings are stored privately. Saving force-stops Gboard once, so the keyboard always reopens with exactly what you saved. Enable this module in LSPosed / Vector and scope Gboard. After upgrading from an older build, reboot once to unload the old hooks.",13,MUTED));
        details.addView(text("Android 13+ and LSPosed / Vector are required. Edit anything, then tap Save & restart Gboard: one tap saves everything and restarts the keyboard. The force-stop needs root; without it, opening the keyboard still syncs the new settings. Font replacement and gesture trails remain off.",13,MUTED));
        details.addView(text("Background animations are optional and work together with the refined ripple. Gboard is unchanged until you save.",13,MUTED));
        Switch logs=new Switch(this);logs.setText("Detailed layout logs (no drawing)");logs.setTextColor(INK);logs.setMinHeight(px(48));logs.setChecked(cfg.debug);
        logs.setOnCheckedChangeListener((b,value)->{cfg.debug=value;draft.edit().putBoolean("debug3",value).apply();changed();});details.addView(logs);
        details.addView(text("If letter colors do not change: enable logs, Save & restart Gboard, then tap a few keys. In LSPosed logs, share only lines beginning RGBPulse legend, plus your Gboard version. These lines contain renderer names and counters, not typed text.",12,MUTED));
        Button emergency=button("Turn everything off & save",false);
        emergency.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Disable all module effects?")
            .setMessage("Turns all lighting off and removes module tiles, overlays and letter tint. This does not disable Gboard’s own native key-press animation. Everything is saved and Gboard is force-stopped, so the change is live the next time the keyboard opens.")
            .setPositiveButton("Disable & save",(d,w)->{
                draft.edit().putBoolean("refined37",true).putBoolean("ripple40",false).putBoolean("tapEffects36",false)
                    .putInt("background51",0).putInt("opening46",0).putInt("closing46",0).putBoolean("enabled",false).putBoolean("glass9",false).putBoolean("tiles41",false).putInt("side20",0).putInt("trail19",0).apply();
                render();persist();
            }).setNegativeButton("Cancel",null).show());details.addView(emergency);
        Button keyboardSettings=button("Open app settings",false);keyboardSettings.setOnClickListener(v->openGboardSettings());details.addView(keyboardSettings);
        Button reset=button("Reset draft",false);reset.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Reset draft?").setMessage("Ripple will be off, with balanced strength and duration. Nothing changes in Gboard until you save.")
            .setPositiveButton("Reset",(d,w)->{draft.edit().clear().apply();prepareDraft();render();}).setNegativeButton("Cancel",null).show());details.addView(reset);
        keyboardPage.addView(text("Your draft stays here until you save it. Nothing plays automatically in the preview.",12,MUTED));

        LinearLayout footer=new LinearLayout(this);footer.setOrientation(1);footer.setPadding(px(22),px(8),px(22),px(12));footer.setBackgroundColor(BG);
        state=text("",12,MUTED);state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);footer.addView(state);
        LinearLayout actions=new LinearLayout(this);
        Button discard=button("Discard",false);discard.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Discard draft changes?")
            .setMessage("Reload your last saved settings. Gboard will not be changed.")
            .setPositiveButton("Discard",(d,w)->{copySettings(applied.getAll(),draft).commit();prepareDraft();render();}).setNegativeButton("Cancel",null).show());
        details.addView(discard);
        // The one and only apply path: saves every setting, then force-stops Gboard so
        // the keyboard reopens with the new configuration. No separate apply step.
        saveRestart=button("Save & restart Gboard",true);
        saveRestart.setContentDescription("Save all settings and force-stop Gboard");
        saveRestart.setOnClickListener(v->persist());
        actions.addView(saveRestart,new LinearLayout.LayoutParams(-1,px(56)));
        footer.addView(actions);
shell.addView(footer);setContentView(shell);tintControls(shell);selectPage(selectedPage);updateState();
    }
    private void selectPage(int index){
        selectedPage=index;if(settingsScroll!=null)settingsScroll.scrollTo(0,0);
        for(int i=0;i<3;i++){
            pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);
            pageButtons[i].setSelected(i==index);
            pageButtons[i].setTextColor(i==index?BG:MUTED);
            pageButtons[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(i==index?0xffb9efd4:CARD));
        }
    }
    private void transitionControl(LinearLayout parent,String title,String key,boolean closing){
        Button choice=button(title+" · "+Config.TRANSITIONS[closing?cfg.closing:cfg.opening],false);
        choice.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(title)
            .setSingleChoiceItems(Config.TRANSITIONS,closing?cfg.closing:cfg.opening,(d,index)->{
                draft.edit().putInt(key,index).apply();changed();choice.setText(title+" · "+Config.TRANSITIONS[index]);d.dismiss();
            }).setNegativeButton("Cancel",null).show());parent.addView(choice);
        Button tryIt=button("Preview "+(closing?"closing":"opening"),false);
        tryIt.setOnClickListener(v->{
            int style=closing?cfg.closing:cfg.opening;
            if(style==0){Toast.makeText(this,"Choose a light style first",Toast.LENGTH_SHORT).show();return;}
            if(previewCollapsed){Toast.makeText(this,"Expand the preview first",Toast.LENGTH_SHORT).show();return;}
            android.graphics.Rect area=new android.graphics.Rect();preview.getHitRect(area);
            previewLight.play(style,closing,cfg.rippleActive,new RectF(area));
        });parent.addView(tryIt);
    }
    private void tintControls(View view){
        android.content.res.ColorStateList tint=new android.content.res.ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xffb9efd4,0xff65747c});
        if(view instanceof Switch){((Switch)view).setThumbTintList(tint);((Switch)view).setTrackTintList(tint);}
        if(view instanceof SeekBar){((SeekBar)view).setProgressTintList(android.content.res.ColorStateList.valueOf(0xffb9efd4));((SeekBar)view).setThumbTintList(android.content.res.ColorStateList.valueOf(0xffb9efd4));}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)tintControls(group.getChildAt(i));}
    }
    private void slider(LinearLayout parent,String name,int min,int max,int value,boolean duration){
        TextView label=text(name+"  ·  "+value+(duration?" ms":"%"),14,INK);parent.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(value-min);bar.setContentDescription(name);bar.setMinimumHeight(px(48));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffd7e7dd));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(INK));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean user){if(!user)return;int n=min+progress;
                label.setText(name+"  ·  "+n+(duration?" ms":"%"));
                if(duration&&name.equals("Feedback length")){cfg.pulseDuration=n;draft.edit().putInt("pulseDuration47",n).apply();}
                else if(duration){cfg.duration=n;draft.edit().putInt("duration3",n).apply();}
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
        // A real picker: saturation/value square plus hue bar, synced with hex and sample.
        final float[] hsv=new float[3];Color.colorToHSV(candidate[0],hsv);
        final Runnable[] apply=new Runnable[1];
        ColorSquare square=new ColorSquare(this,hsv[0],hsv[1],hsv[2],(s,v)->{hsv[1]=s;hsv[2]=v;apply[0].run();});
        content.addView(square,new LinearLayout.LayoutParams(-1,px(170)));
        HueBar hbar=new HueBar(this,hsv[0],deg->{hsv[0]=deg;square.setHue(deg);apply[0].run();});
        content.addView(hbar,new LinearLayout.LayoutParams(-1,px(44)));
        apply[0]=()->{candidate[0]=Color.HSVToColor(hsv)|0xff000000;sample.setTextColor(candidate[0]);swatch(sample,candidate[0]);input.setText(hex(candidate[0]));};
        apply[0].run();
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){
                if(s.toString().matches("#[0-9a-fA-F]{6}")){candidate[0]=Color.parseColor(s.toString())|0xff000000;Color.colorToHSV(candidate[0],hsv);
                    square.setHue(hsv[0]);square.setSV(hsv[1],hsv[2]);sample.setTextColor(candidate[0]);swatch(sample,candidate[0]);}}
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
    private static float clamp01(float v){return v<0?0:(v>1?1:v);}

    /** A draggable hue spectrum bar with a round thumb — a proper hue picker. */
    private final class HueBar extends View {
        private final Paint p=new Paint(3);
        private final HueSink sink;
        private float degrees;
        HueBar(Context c,float start,HueSink sink){super(c);this.sink=sink;degrees=start;setMinimumHeight(px(44));setContentDescription("Hue");}
        void set(float value){degrees=value;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            float x0=getPaddingLeft(),x1=getWidth()-getPaddingRight(),w=Math.max(1,x1-x0),cy=getHeight()*.5f;
            int[] rainbow=new int[7];for(int i=0;i<7;i++)rainbow[i]=Color.HSVToColor(new float[]{i*60,1,1});
            p.setShader(new LinearGradient(x0,0,x1,0,rainbow,null,Shader.TileMode.CLAMP));
            canvas.drawRoundRect(x0,cy-px(8),x1,cy+px(8),px(8),px(8),p);
            p.setShader(null);
            float tx=x0+w*clamp01(degrees/360f);
            p.setColor(0xff0c1419);canvas.drawCircle(tx,cy,px(12),p);
            p.setColor(Color.HSVToColor(new float[]{degrees,1,1}));canvas.drawCircle(tx,cy,px(9),p);
            p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(px(1.5f));canvas.drawCircle(tx,cy,px(9),p);p.setStyle(Paint.Style.FILL);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){
                float x0=getPaddingLeft(),w=Math.max(1,getWidth()-getPaddingLeft()-getPaddingRight());
                degrees=clamp01((e.getX()-x0)/w)*360f;
                sink.hue(Math.round(degrees));invalidate();
            }
            return true;
        }
    }

    /** Saturation/value square for the current hue. */
    private final class ColorSquare extends View {
        private final Paint p=new Paint(3);
        private final SvSink sink;
        private float hue,sat,val;
        ColorSquare(Context c,float h,float s,float v,SvSink sink){super(c);this.sink=sink;hue=h;sat=s;val=v;setMinimumHeight(px(160));setContentDescription("Saturation and brightness");}
        void setHue(float h){hue=h;invalidate();}
        void setSV(float s,float v){sat=s;val=v;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            float w=getWidth(),h=getHeight();
            int base=Color.HSVToColor(new float[]{hue,1,1});
            p.setShader(new LinearGradient(0,0,w,0,new int[]{0xffffffff,base},null,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p);
            p.setShader(new LinearGradient(0,0,0,h,new int[]{0x00000000,0xff000000},null,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p);
            p.setShader(null);
            float tx=clamp01(sat)*w,ty=(1-clamp01(val))*h;
            p.setColor(0xff0c1419);canvas.drawCircle(tx,ty,px(11),p);
            p.setColor(Color.HSVToColor(new float[]{hue,sat,val})|0xff000000);canvas.drawCircle(tx,ty,px(8),p);
            p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(px(1.5f));canvas.drawCircle(tx,ty,px(8),p);p.setStyle(Paint.Style.FILL);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){
                float s=clamp01(e.getX()/Math.max(1,getWidth())),v=1-clamp01(e.getY()/Math.max(1,getHeight()));
                sat=s;val=v;sink.sv(s,v);invalidate();
            }
            return true;
        }
    }

    private LinearLayout hueControl(LinearLayout panel,String title,String key,boolean primary){
        LinearLayout row=new LinearLayout(this);row.setOrientation(1);
        TextView value=text(title+" · "+(primary?cfg.hue1:cfg.hue2)+"°",13,INK);row.addView(value);
        swatch(value,Color.HSVToColor(new float[]{primary?cfg.hue1:cfg.hue2,cfg.sat/100f,1}));
        HueBar bar=new HueBar(this,primary?cfg.hue1:cfg.hue2,degrees->{
            if(primary)cfg.hue1=degrees;else cfg.hue2=degrees;
            value.setText(title+" · "+degrees+"°");swatch(value,Color.HSVToColor(new float[]{degrees,cfg.sat/100f,1}));
            draft.edit().putInt(key,degrees).apply();changed();
        });
        row.addView(bar,new LinearLayout.LayoutParams(-1,px(44)));panel.addView(row);return row;
    }
    private void changed(){
        cfg=Config.from(draft);preview.fx.cfg=cfg;updatePalette();
        preview.fx.clear();preview.clearFades();preview.invalidate();updateState();
    }
    private void updateState(){
        boolean dirty=!draft.getAll().equals(applied.getAll());
        String revision=Config.string(applied,SettingsContract.REVISION,"");
        String received=Config.string(getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE),SettingsContract.REVISION,"");
        String savedState=confirmed(revision,received)?
            (Config.from(applied).enabled?"You’re all set · Gboard is up to date":"Gboard confirmed: all module effects off"):
            "Saved · waiting for Gboard to open";
        state.setText(saving?"Saving & restarting Gboard…":dirty?"Unsaved changes · tap Save & restart Gboard":savedState);
        saveRestart.setEnabled(!saving);saveRestart.setAlpha(!saving?1f:.45f);
    }
    private static SharedPreferences.Editor copySettings(Map<String,?> values,SharedPreferences target){return SettingsStore.copy(values,target);}
    private void persist(){
        if(saving)return;
        final Map<String,Object> snapshot=new java.util.HashMap<>(draft.getAll());
        final String revision=java.util.UUID.randomUUID().toString();snapshot.put(SettingsContract.REVISION,revision);
        saving=true;updateState();
        state.setText("Saving, then waiting for root approval…");
        new Thread(()->{
            boolean saved=false,stopped=false;String failure=null;
            try{
                saved=SettingsStore.save(snapshot,applied);
                if(saved){
                    draft.edit().putString(SettingsContract.REVISION,revision).apply();
                    notifyKeyboard();
                    stopped=GboardRestart.stop();
                }
            }catch(RuntimeException e){failure=e.getClass().getSimpleName();android.util.Log.w("RGBPulse","Save & restart did not complete",e);}
            final boolean ok=saved,didStop=stopped;final String problem=failure;
            runOnUiThread(()->{
                saving=false;
                if(isFinishing()||isDestroyed())return;
                updateState();
                if(!ok){state.setText("Couldn’t save. Tap Save & restart Gboard to try again.");return;}
                confirmationChecks=0;confirmationHandler.removeCallbacks(confirmationTick);confirmationHandler.postDelayed(confirmationTick,300);
                if(didStop)state.setText("Saved · Gboard was stopped. Tap a text field to reopen it with the new settings.");
                else if(problem!=null)state.setText("Saved, but connection needs a retry. Open Gboard to sync.");
                else state.setText("Saved · the force-stop was not approved. Open Gboard to sync.");
                if(didStop)Toast.makeText(this,"Saved. Gboard restarts the next time you open it.",Toast.LENGTH_SHORT).show();
                else new AlertDialog.Builder(this).setTitle("Gboard was not stopped")
                    .setMessage("Settings were saved, but the force-stop needs root and it was unavailable, denied, or timed out. Open the keyboard to pick up the new settings anyway, or force-stop Gboard manually.")
                    .setPositiveButton("Open app settings",(d,w)->openGboardSettings()).setNegativeButton("Not now",null).show();
                if(!draft.getAll().equals(snapshot))state.append(" Newer edits still need saving.");
            });
        },"Save & restart").start();
    }
    private void notifyKeyboard(){
        try{SettingsTransport.push(this);}
        catch(RuntimeException e){android.util.Log.w("RGBPulse","Settings delivery failed",e);}
    }
    private boolean confirmed(String revision,String received){
        return RevisionGate.accepts(revision,received,SettingsContract.RUNTIME_VERSION,getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE)
            .getInt("runtimeVersion",0));
    }
    private void checkConfirmation(){
        if(isFinishing()||isDestroyed()||saving)return;
        updateState();
        String revision=Config.string(applied,SettingsContract.REVISION,"");
        String received=Config.string(getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE),SettingsContract.REVISION,"");
        if(confirmed(revision,received))return;
        if(confirmationChecks==3||confirmationChecks==10)notifyKeyboard();
        if(++confirmationChecks<30)confirmationHandler.postDelayed(confirmationTick,400);
        else {
            boolean reply=getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE).getBoolean("moduleResponded48",false);
            state.append(reply?". Module replied, but did not confirm application.":". No live module reply. Open Gboard; check module scope and reboot to load 1.1.0.");
        }
    }
    private void openGboardSettings(){
        try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:com.google.android.inputmethod.latin")));}
        catch(RuntimeException e){Toast.makeText(this,"Open Android Settings > Apps > Gboard > Force stop.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("page46",selectedPage);state.putBoolean("previewCollapsed46",previewCollapsed);super.onSaveInstanceState(state);
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
    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(px(16),px(14),px(16),px(14));GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{CARD,0xff141e25});surface.setCornerRadius(px(20));surface.setStroke(px(1),0xff29363e);c.setBackground(surface);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=px(18);parent.addView(c,lp);return c;}
    private Button button(String name,boolean primary){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?BG:INK);b.setMinHeight(px(48));b.setPadding(px(10),0,px(10),0);b.setBackground(background(primary?0xffb9efd4:0xff23323a,14));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?0xffd7e7dd:0xff292e31));return b;}
    @Override protected void onPause(){if(previewLight!=null)previewLight.cancel();confirmationHandler.removeCallbacks(confirmationTick);if(preview!=null)preview.stop();super.onPause();}
    @Override protected void onResume(){super.onResume();if(preview!=null){preview.running=true;preview.invalidate();}confirmationChecks=0;confirmationHandler.post(confirmationTick);}
    @Override protected void onDestroy(){if(previewLight!=null)previewLight.cancel();confirmationHandler.removeCallbacks(confirmationTick);if(preview!=null)preview.fx.dispose();super.onDestroy();}

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
                    keyRect(row,k);if(rect.contains(e.getX(pointer),e.getY(pointer))){if(previewLight!=null)previewLight.cancel();long now=SystemClock.uptimeMillis();fx.tap(rect.centerX(),rect.centerY(),play,rect,now);if(cfg.ripple)fx.side.tap(rect.centerX(),(int)rect.top,now);invalidate();return true;}}
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
                if(cfg.ripple)ripple.draw(canvas,rect,dp,amount,cfg.rippleOpacity/100f,6*dp,cfg.rippleActive,cfg.rippleInactive,cfg.borderTenths/10f);
                label.setColor(cfg.ripple?LegendTint.color(INK,amount,cfg.letterInactive,cfg.letterActive):INK);
                label.setTextSize(Math.min((row==3?11:13)*dp,rect.height()*.72f));
                canvas.drawText(rows[row][k],rect.centerX(),rect.centerY()-(label.ascent()+label.descent())/2,label);
            }
            canvas.restoreToCount(save);removeCallbacks(tick);
            if(shaderStatus!=null&&fx.shaderIssue()!=null){shaderStatus.setText("Background shader unavailable on this device; the key ripple still works.");shaderStatus.setVisibility(View.VISIBLE);}
            if(running&&isShown()&&active)postOnAnimation(tick);
        }
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    }
}
