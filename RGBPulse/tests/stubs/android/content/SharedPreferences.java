package android.content;
/** Minimal compile-only interface for Android-free Config tests. Never packaged in the APK. */
public interface SharedPreferences {
    boolean getBoolean(String key,boolean fallback);
    int getInt(String key,int fallback);
    String getString(String key,String fallback);
}
