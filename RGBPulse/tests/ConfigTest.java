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
        check(c.enabled&&c.effect==7&&c.duration==800&&c.debug&&c.fontData.equals(""));
        p.put("refined37",true);p.put("tapEffects36",true);p.put("font11",8);
        p.put("duration3",3500);p.put("opacity3",100);p.put("trail19",3);
        c=Config.from(p);
        check(c.refined&&c.glass&&c.sideStyle==1&&c.tapEffects&&c.trailStyle==0);
        check(c.font==0&&c.letterSize==100&&c.duration==1100&&c.opacity==85);
        p.put("tapEffects36",false);
        check(!Config.from(p).tapEffects); // refined mode honors the background opt-out
        p.put("tapEffects36",true);
        check(Config.from(p).tapEffects); // and the explicit opt-in
        // Explicit switches must beat stale legacy "enabled" / "side20" values.
        for(boolean oldMaster:new boolean[]{false,true})for(boolean ripple:new boolean[]{false,true})for(boolean background:new boolean[]{false,true}){
            p.put("enabled",oldMaster);p.put("ripple40",ripple);p.put("tapEffects36",background);
            p.put("glass9",true);p.put("side20",1);
            c=Config.from(p);
            check(c.enabled==(ripple||background));
            check(c.ripple==ripple&&c.glass==ripple&&c.sideStyle==(ripple?1:0));
            check(c.tapEffects==background);
            Config reopened=Config.from(p);check(reopened.ripple==ripple&&reopened.tapEffects==background);
        }
        p.remove("ripple40");p.put("enabled",false);p.put("tapEffects36",true);
        c=Config.from(p);check(c.enabled&&!c.ripple&&c.tapEffects); // upgrade background-only
        p.put("opacity3",5);p.put("rippleStrength41",80);p.put("backgroundStrength41",95);
        c=Config.from(p);check(c.rippleOpacity==80&&c.backgroundOpacity==95);
        p.put("rippleStrength41",25);check(Config.from(p).backgroundOpacity==95);
        p.put("backgroundStrength41",35);check(Config.from(p).rippleOpacity==25);
        p.put("tiles41",false);check(!Config.from(p).tiles);
        p.put("tiles41",true);check(!Config.from(p).tiles&&Config.from(p).hideTiles);
        p.put("hideTiles45",false);check(Config.from(p).tiles);
        p.put("hideTiles45",true);p.put("ripple40",false);p.put("tapEffects36",false);
        check(!Config.from(p).enabled); // hiding tiles never silently enables effects
        for(int style=0;style<12;style++){p.put("pulse47",style);check(Config.from(p).effect==style);}
        p.put("pulse47",16);check(Config.from(p).effect==11); // background indices are not key animations
        p.remove("pulse47");p.put("tapFx6",19);check(Config.from(p).effect==7);
        check(Config.from(p).pulseDuration==160);p.put("pulseDuration47",999);check(Config.from(p).pulseDuration==360);
        p.put("pulseDuration47",1);check(Config.from(p).pulseDuration==120);
        p.put("rippleStyle42",5);check(Config.from(p).rippleStyle==4);
        p.remove("rippleStrength41");p.remove("backgroundStrength41");
        c=Config.from(p);check(c.rippleOpacity==70&&c.backgroundOpacity==90); // no hidden legacy dim
        p.put("rippleActive42",0xff123456);p.put("letterInactive42",0xffaabbcc);p.put("borderWidth42",99);p.put("rippleStyle42",3);
        c=Config.from(p);check(c.rippleActive==0xff123456&&c.letterInactive==0xffaabbcc&&c.borderTenths==30&&c.rippleStyle==3);
        check(Config.EFFECTS.length==16&&Config.EFFECT_HINTS.length==16);
        p.put("quietBackground48",true);p.put("quietStrength48",90);check(!Config.from(p).enabled); // removed quiet background stays inert
        p.remove("quietBackground48");p.remove("quietStrength48");
        p.put("fluid50",true);p.put("fluidStrength50",70);check(!Config.from(p).enabled); // removed feature keys stay inert
        p.remove("fluid50");p.remove("fluidStrength50");
        p.put("pulse47",9);check(Config.from(p).effect==9);p.put("pulse47",12);check(Config.from(p).effect==11);
        p.put("pulse47",3);p.put("background51",2);c=Config.from(p);check(c.enabled&&c.background==2&&c.effect==3);
        p.put("background51",9);check(Config.from(p).background==4);
        p.put("bgduration51",9999);check(Config.from(p).bgDuration==3500);p.put("bgduration51",10);check(Config.from(p).bgDuration==300);p.put("bgduration51",1800);check(Config.from(p).bgDuration==1800);
        p.put("background51",0);p.put("tapEffects36",false);check(!Config.from(p).enabled);
        p.put("opening46",2);c=Config.from(p);check(c.enabled&&c.opening==2&&!c.ripple&&!c.tapEffects);
        p.put("opening46",0);p.put("closing46",3);check(Config.from(p).enabled);
        p.put("closing46",0);check(!Config.from(p).enabled);
        p.put("opening46",999);p.put("closing46",-2);check(Config.from(p).opening==3&&Config.from(p).closing==0);
        check(Config.RIPPLES.length==9);
        check(((Integer)p.get("font11"))==8); // loading never mutates preferences
        System.out.println("PASS: opt-in defaults, malformed preference isolation, refined constraints and read-only loading");
    }
}
