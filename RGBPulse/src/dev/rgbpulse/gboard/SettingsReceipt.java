package dev.rgbpulse.gboard;

import android.content.*;

/** Non-exported. Only the immutable, one-shot capability sent to Gboard can reach it. */
public final class SettingsReceipt extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        synchronized(SettingsStore.LOCK){
            SharedPreferences applied=context.getSharedPreferences(Config.PREFS,Context.MODE_PRIVATE);
            String revision=intent.getStringExtra(SettingsContract.REVISION);
            int build=intent.getIntExtra("runtimeVersion",0);
            if(!RevisionGate.accepts(Config.string(applied,SettingsContract.REVISION,""),revision,SettingsContract.RUNTIME_VERSION,build))return;
            context.getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE).edit()
                .putString(SettingsContract.REVISION,revision).putInt("runtimeVersion",build)
                .putLong("receivedAt",System.currentTimeMillis()).apply();
        }
    }
}
