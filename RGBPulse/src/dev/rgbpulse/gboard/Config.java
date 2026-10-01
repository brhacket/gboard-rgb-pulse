package dev.rgbpulse.gboard;

import android.content.SharedPreferences;

public final class Config {
    public static final String PREFS = "settings";
    public static final String[] EFFECTS = {"Hologram tiles", "Neon halo", "Liquid glass", "Aurora ring",
        "Liquid metaballs", "Gradient bloom", "Prism ring", "Sonar pulse", "Silk wave",
        "Plasma burst", "Dual orbit", "Shuffle modern waves"};
    public static final int GPU_COUNT = 11, LIQUID_SHUFFLE = 11;
    public static final String[] COLORS = {"Rainbow cycle", "Random per tap", "Single color", "Two-color gradient", "Hue by position"};
    public static final String[] LAYERS = {"Real keyboard background (default)", "Over keys — keyboard only"};
    public boolean enabled = false;
    public boolean tapEffects = false;
    public int effect = 0, colorMode = 0;
    public int hue1 = 290, hue2 = 190, sat = 100;
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
            c.enabled = p.getBoolean("enabled", c.enabled);
            c.tapEffects = p.getBoolean("tapEffects36", false);
            // v6 catalog is intentionally new. First upgrade selects the user's favorite.
            c.effect = clamp(p.getInt("tapFx6", c.effect), 0, EFFECTS.length - 1);
            c.colorMode = clamp(p.getInt("colorMode", c.colorMode), 0, COLORS.length - 1);
            c.hue1 = clamp(p.getInt("hue1", c.hue1), 0, 360);
            c.hue2 = clamp(p.getInt("hue2", c.hue2), 0, 360);
            c.sat = clamp(p.getInt("sat", c.sat), 0, 100);
            c.opacity = clamp(p.getInt("opacity3", c.opacity), 5, 100);
            c.duration = clamp(p.getInt("duration3", c.duration), 300, 3500);
            c.size = clamp(p.getInt("size", c.size), 30, 250);
            c.thickness = clamp(p.getInt("thickness8", c.thickness), 25, 300);
            c.accentColor=p.getInt("accent19",0xff575c68);c.tileColor=p.getInt("tile19",0xfff5f5f3);c.sideStyle=clamp(p.getInt("side20",0),0,2);c.trailStyle=clamp(p.getInt("trail19",0),0,3);c.trailWidth=clamp(p.getInt("trailWidth19",2),1,6);c.trailLife=clamp(p.getInt("trailLife19",420),150,1000);
            c.layer = clamp(p.getInt("layer4", c.layer), 0, LAYERS.length - 1);
            c.letterWave=false;c.letterBrightness=clamp(p.getInt("letterBrightness16",55),20,100);c.metal=clamp(p.getInt("metal16",70),0,100);
            c.glass=p.getBoolean("glass9",false); c.bold=p.getBoolean("bold9",false);
            c.tileScale=clamp(p.getInt("tileScale18",78),60,95);
            c.font=clamp(p.getInt("font11",p.getInt("font9",0)==5?5:0),0,10);c.letterSize=clamp(p.getInt("letterSize9",100),75,125);
            c.roundness=clamp(p.getInt("roundness15",90),0,100);
            c.frost=clamp(p.getInt("frost13",35),0,100);
            c.texture=clamp(p.getInt("texture12",8),0,100);c.lens=clamp(p.getInt("lens12",125),0,200);c.letterTexture=clamp(p.getInt("letterTexture11",40),0,100);
            c.glow=clamp(p.getInt("glow9",35),0,100);c.fontData=p.getString("fontData9","");
            c.debug = p.getBoolean("debug3", false);
        } catch (RuntimeException ignored) { }
        return c;
    }
    static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
