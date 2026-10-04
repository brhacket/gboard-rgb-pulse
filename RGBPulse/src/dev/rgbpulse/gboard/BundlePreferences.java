package dev.rgbpulse.gboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.*;

/** Immutable adapter: Config uses the same parser in the editor and Gboard process. */
final class BundlePreferences implements SharedPreferences {
    private final Map<String,Object> values=new HashMap<>();
    BundlePreferences(Bundle data){for(String key:data.keySet())values.put(key,data.get(key));}
    @Override public Map<String,?> getAll(){return Collections.unmodifiableMap(values);}
    @Override public boolean contains(String key){return values.containsKey(key);}
    @Override public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}
    @SuppressWarnings("unchecked") @Override public Set<String> getStringSet(String k,Set<String> d){return values.containsKey(k)?(Set<String>)values.get(k):d;}
    @Override public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}
    @Override public long getLong(String k,long d){return values.containsKey(k)?(Long)values.get(k):d;}
    @Override public float getFloat(String k,float d){return values.containsKey(k)?(Float)values.get(k):d;}
    @Override public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}
    @Override public Editor edit(){throw new UnsupportedOperationException("Read-only snapshot");}
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
}
