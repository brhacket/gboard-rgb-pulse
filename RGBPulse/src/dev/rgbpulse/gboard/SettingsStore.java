package dev.rgbpulse.gboard;

import android.content.SharedPreferences;
import java.util.Map;

/** Provider reads and editor writes share this lock in the module's default process. */
final class SettingsStore {
    static final Object LOCK=new Object();
    static boolean save(Map<String,?> values,SharedPreferences target){
        synchronized(LOCK){
            Map<String,?> previous=target.getAll();
            try{if(copy(values,target).commit())return true;}catch(RuntimeException ignored){}
            // Android commit(false) can still mutate its in-memory cache. Do not expose
            // that failed revision through the provider on the next keyboard opening.
            try{copy(previous,target).commit();}catch(RuntimeException ignored){}
            return false;
        }
    }
    static SharedPreferences.Editor copy(Map<String,?> values,SharedPreferences target){
        SharedPreferences.Editor editor=target.edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet()){
            Object value=entry.getValue();String key=entry.getKey();
            if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)editor.putInt(key,(Integer)value);
            else if(value instanceof String)editor.putString(key,(String)value);
            else if(value instanceof Long)editor.putLong(key,(Long)value);
            else if(value instanceof Float)editor.putFloat(key,(Float)value);
        }
        return editor;
    }
}
