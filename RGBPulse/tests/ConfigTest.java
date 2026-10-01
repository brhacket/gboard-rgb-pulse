package dev.rgbpulse.gboard;
import android.content.SharedPreferences;
import java.util.HashMap;

public final class ConfigTest {
    private static class Preferences extends HashMap<String,Object> implements SharedPreferences {
        public boolean getBoolean(String key,boolean fallback){return containsKey(key)?(Boolean)get(key):fallback;}
        public int getInt(String key,int fallback){return containsKey(key)?(Integer)get(key):fallback;}
        public String getString(String key,String fallback){return containsKey(key)?(String)get(key):fallback;}
    }
    private static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        Preferences p=new Preferences();Config empty=Config.from(p);
        check(!empty.enabled&&!empty.tapEffects&&empty.trailStyle==0&&empty.sideStyle==0);
        p.put("enabled",true);p.put("tapFx6","corrupt");p.put("duration3",800);
        p.put("fontData9",null);p.put("debug3",true);
        Config c=Config.from(p);
        check(c.enabled&&c.effect==0&&c.duration==800&&c.debug&&c.fontData.equals(""));
        p.put("refined37",true);p.put("tapEffects36",true);p.put("font11",8);
        p.put("duration3",3500);p.put("opacity3",100);p.put("trail19",3);
        c=Config.from(p);
        check(c.refined&&c.glass&&c.sideStyle==1&&c.tapEffects&&c.trailStyle==0);
        check(c.font==0&&c.letterSize==100&&c.duration==1100&&c.opacity==85);
        p.put("tapEffects36",false);
        check(!Config.from(p).tapEffects); // refined mode honors the background opt-out
        p.put("tapEffects36",true);
        check(Config.from(p).tapEffects); // and the explicit opt-in
        check(((Integer)p.get("font11"))==8); // loading never mutates preferences
        System.out.println("PASS: opt-in defaults, malformed preference isolation, refined constraints and read-only loading");
    }
}
