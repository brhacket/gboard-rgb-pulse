package android.content;
/** Minimal compile-only interface for Android-free Config tests. Never packaged in the APK. */
public interface SharedPreferences {
    default java.util.Map<String,?> getAll(){throw new UnsupportedOperationException();}
    default Editor edit(){throw new UnsupportedOperationException();}
    interface Editor {
        Editor clear();
        Editor putBoolean(String key,boolean value);
        Editor putInt(String key,int value);
        Editor putString(String key,String value);
        Editor putLong(String key,long value);
        Editor putFloat(String key,float value);
        boolean commit();
    }
    boolean getBoolean(String key,boolean fallback);
    int getInt(String key,int fallback);
    String getString(String key,String fallback);
}
