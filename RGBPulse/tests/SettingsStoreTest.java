package dev.rgbpulse.gboard;
import android.content.SharedPreferences;
import java.util.*;

public final class SettingsStoreTest {
    static final class Prefs implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>();
        boolean fail;int commits;
        public Map<String,?> getAll(){return new HashMap<>(values);}
        public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}
        public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}
        public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}
        public Editor edit(){return new Editor(){
            final Map<String,Object> next=new HashMap<>(values);
            public Editor clear(){next.clear();return this;}
            public Editor putString(String k,String v){next.put(k,v);return this;}
            public Editor putBoolean(String k,boolean v){next.put(k,v);return this;}
            public Editor putInt(String k,int v){next.put(k,v);return this;}
            public Editor putLong(String k,long v){next.put(k,v);return this;}
            public Editor putFloat(String k,float v){next.put(k,v);return this;}
            public boolean commit(){commits++;values.clear();values.putAll(next);return !fail;}
        };}
    }
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        Prefs p=new Prefs();p.values.put("revision44","old");p.values.put("ripple40",true);
        Map<String,Object> next=new HashMap<>();next.put("revision44","new");next.put("ripple40",false);
        p.fail=true;check(!SettingsStore.save(next,p));
        check(p.commits==2&&p.getString("revision44","").equals("old")&&p.getBoolean("ripple40",false));
        p.fail=false;check(SettingsStore.save(next,p));
        check(p.commits==3&&p.getString("revision44","").equals("new")&&!p.getBoolean("ripple40",true));
        check(next.get("revision44").equals("new"));
        System.out.println("PASS: successful settings snapshot replaces old values; failed commit restores in-memory revision and effects");
    }
}
