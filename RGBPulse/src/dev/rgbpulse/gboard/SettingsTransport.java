package dev.rgbpulse.gboard;

import android.app.PendingIntent;
import android.content.*;
import android.os.*;
import java.util.Map;

/** A signed, package-targeted snapshot; no provider visibility or shared XML needed. */
final class SettingsTransport {
    static void push(Context context){
        synchronized(SettingsStore.LOCK){
            android.content.SharedPreferences prefs=context.getSharedPreferences(Config.PREFS,Context.MODE_PRIVATE);
            String revision=Config.string(prefs,SettingsContract.REVISION,"");
            if(revision.isEmpty())return;
            Bundle data=new Bundle();
            for(Map.Entry<String,?> e:prefs.getAll().entrySet()){
                Object value=e.getValue();String key=e.getKey();
                if("fontData9".equals(key))continue;
                if(value instanceof Boolean)data.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer)data.putInt(key,(Integer)value);
                else if(value instanceof String&&((String)value).length()<=4096)data.putString(key,(String)value);
            }
            Intent receipt=new Intent(context,SettingsReceipt.class)
                .setData(android.net.Uri.parse("rgbpulse://receipt/"+revision))
                .putExtra(SettingsContract.REVISION,revision).putExtra("runtimeVersion",SettingsContract.RUNTIME_VERSION);
            PendingIntent capability=PendingIntent.getBroadcast(context,0,receipt,
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_UPDATE_CURRENT);
            Intent delivery=new Intent(SettingsContract.ACTION).setPackage(SettingsContract.GBOARD)
                .putExtra("snapshot",data).putExtra("receipt",capability)
                .putExtra("stamp",SystemClock.elapsedRealtimeNanos()).putExtra("runtimeVersion",SettingsContract.RUNTIME_VERSION);
            context.sendOrderedBroadcast(delivery,null,new BroadcastReceiver(){
                @Override public void onReceive(Context c,Intent i){
                    c.getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE).edit()
                        .putBoolean("moduleResponded48",getResultCode()==SettingsContract.RUNTIME_VERSION).apply();
                }
            },null,0,null,null);
        }
    }
}
