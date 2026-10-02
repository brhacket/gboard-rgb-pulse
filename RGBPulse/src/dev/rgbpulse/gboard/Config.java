package dev.rgbpulse.gboard;

import android.content.SharedPreferences;

public final class Config {
    public static final String PREFS = "settings";
    public static final String[] EFFECTS = {"Hologram tiles", "Neon halo", "Liquid glass", "Aurora ring",
        "Liquid metaballs", "Gradient bloom", "Prism ring", "Sonar pulse", "Silk wave",
        "Plasma burst", "Dual orbit", "Shuffle modern waves", "Material bloom", "Diffused ring", "Tonal orbit"};
    public static final int GPU_COUNT = 15, LIQUID_SHUFFLE = 11;
    public static final String[] COLORS = {"Rainbow cycle", "Random per tap", "Single color", "Two-color gradient", "Hue by position"};
    public static final String[] LAYERS = {"Real keyboard background (default)", "Over keys — keyboard only"};
    public boolean enabled = false;
    public boolean tapEffects = false;
    public boolean ripple = false;
    public static final String[] RIPPLES={"Row flow", "Soft echo", "Wide glow", "Touch pulse"};
    public int rippleStyle=0,borderTenths=10;
    public int rippleActive=0xffd0bcff,rippleInactive=0xff49454f;
    public int letterActive=0xfff5efff,letterInactive=0xffb7b2be;
    public boolean tiles = true;
    public int rippleOpacity=70, backgroundOpacity=90;
    public boolean refined = false;
    public int effect = 12, colorMode = 2;
    public int hue1 = 270, hue2 = 195, sat = 30;
    public int opacity = 90, duration = 1700, size = 120, thickness = 100;
    public int layer = 0;
    public int accentColor=0xffa6f0c6,tileColor=0xfff5f5f3,trailStyle=0,trailWidth=2,trailLife=420,sideStyle=0;
    public boolean debug = false;
    public boolean glass=false,bold=false,letterWave=false;
    public int letterBrightness=55,metal=70;
    public int font=0,letterSize=100,glow=35,tileScale=78;
    public int texture=8,lens=125,letterTexture=40,frost=35,roundness=90;
    public String fontData="";

    public static Config from(SharedPreferences p) {
        Config c = new Config();
        if (p == null) return c;
        try {
            c.refined = flag(p,"refined37",false);
            c.tiles=flag(p,"tiles41",true);
            c.rippleStyle=clamp(number(p,"rippleStyle42",0),0,RIPPLES.length-1);
            c.borderTenths=clamp(number(p,"borderWidth42",10),5,30);
            c.rippleActive=number(p,"rippleActive42",c.rippleActive)|0xff000000;
            c.rippleInactive=number(p,"rippleInactive42",c.rippleInactive)|0xff000000;
            c.letterActive=number(p,"letterActive42",c.letterActive)|0xff000000;
            c.letterInactive=number(p,"letterInactive42",c.letterInactive)|0xff000000;
            c.rippleOpacity=clamp(number(p,"rippleStrength41",70),15,100);
            c.backgroundOpacity=clamp(number(p,"backgroundStrength41",90),5,100);
            c.enabled = flag(p,"enabled", c.enabled);
            c.tapEffects = flag(p,"tapEffects36", false);
            // v6 catalog is intentionally new. First upgrade selects the user's favorite.
            c.effect = clamp(number(p,"tapFx6", c.effect), 0, EFFECTS.length - 1);
            c.colorMode = clamp(number(p,"colorMode", c.colorMode), 0, COLORS.length - 1);
            c.hue1 = clamp(number(p,"hue1", c.hue1), 0, 360);
            c.hue2 = clamp(number(p,"hue2", c.hue2), 0, 360);
            c.sat = clamp(number(p,"sat", c.sat), 0, 100);
            c.opacity = clamp(number(p,"opacity3", c.opacity), 5, 100);
            c.duration = clamp(number(p,"duration3", c.duration), 300, 3500);
            c.size = clamp(number(p,"size", c.size), 30, 250);
            c.thickness = clamp(number(p,"thickness8", c.thickness), 25, 300);
            c.accentColor=number(p,"accent19",0xff575c68);c.tileColor=number(p,"tile19",0xfff5f5f3);c.sideStyle=clamp(number(p,"side20",0),0,2);c.trailStyle=clamp(number(p,"trail19",0),0,3);c.trailWidth=clamp(number(p,"trailWidth19",2),1,6);c.trailLife=clamp(number(p,"trailLife19",420),150,1000);
            c.layer = clamp(number(p,"layer4", c.layer), 0, LAYERS.length - 1);
            c.letterWave=false;c.letterBrightness=clamp(number(p,"letterBrightness16",55),20,100);c.metal=clamp(number(p,"metal16",70),0,100);
            c.glass=flag(p,"glass9",false); c.bold=flag(p,"bold9",false);
            c.tileScale=clamp(number(p,"tileScale18",78),60,95);
            c.font=clamp(number(p,"font11",number(p,"font9",0)==5?5:0),0,10);c.letterSize=clamp(number(p,"letterSize9",100),75,125);
            c.roundness=clamp(number(p,"roundness15",90),0,100);
            c.frost=clamp(number(p,"frost13",35),0,100);
            c.texture=clamp(number(p,"texture12",8),0,100);c.lens=clamp(number(p,"lens12",125),0,200);c.letterTexture=clamp(number(p,"letterTexture11",40),0,100);
            c.glow=clamp(number(p,"glow9",35),0,100);c.fontData=string(p,"fontData9","");
            c.debug = flag(p,"debug3", false);
        } catch (RuntimeException ignored) { }
        c.ripple=c.enabled&&c.glass&&c.sideStyle>0;
        if(c.refined){
            // Migrate the old mislabelled master switch only when ripple40 is absent.
            c.ripple=flag(p,"ripple40",c.enabled);
            c.enabled=c.ripple||c.tapEffects;
            c.glass=c.ripple;c.sideStyle=c.ripple?1:0;c.trailStyle=0;c.layer=0;
            c.font=0;c.bold=false;c.letterSize=100;
            c.duration=clamp(c.duration,400,1100);c.opacity=clamp(c.opacity,15,85);
        }
        return c;
    }
    // A malformed preference must not abort loading every setting after it.
    static boolean flag(SharedPreferences p,String key,boolean fallback){
        try{return p.getBoolean(key,fallback);}catch(ClassCastException e){return fallback;}
    }
    static int number(SharedPreferences p,String key,int fallback){
        try{return p.getInt(key,fallback);}catch(ClassCastException e){return fallback;}
    }
    static String string(SharedPreferences p,String key,String fallback){
        try{String value=p.getString(key,fallback);return value==null?fallback:value;}catch(ClassCastException e){return fallback;}
    }
    static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
