package dev.rgbpulse.gboard;

final class SettingsContract {
    static final String AUTHORITY="dev.rgbpulse.gboard.settings";
    static final android.net.Uri URI=android.net.Uri.parse("content://"+AUTHORITY);
    static final String REVISION="revision44";
    static final String STATUS="gboard_connection";
    static final String ACTION="dev.rgbpulse.gboard.RELOAD_SETTINGS";
    static final String PERMISSION="dev.rgbpulse.gboard.permission.NOTIFY_SETTINGS";
    static final String GBOARD="com.google.android.inputmethod.latin";
}
