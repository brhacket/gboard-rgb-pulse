package dev.rgbpulse.gboard;

import android.content.*;
import android.os.SystemClock;

/** Bootstrap only: ignores all payloads and always sends own applied settings to Gboard.
 * Exported so the injected process can request settings after restarting. No data is
 * returned to the caller, and no caller-selected destination, revision or values exist.
 */
public final class SettingsRelay extends BroadcastReceiver {
    private static long last=-1000;
    @Override public void onReceive(Context context,Intent intent){
        if(!SettingsContract.PULL.equals(intent.getAction()))return;
        long now=SystemClock.elapsedRealtime();if(now-last<500)return;last=now;
        SettingsTransport.push(context);
    }
}
