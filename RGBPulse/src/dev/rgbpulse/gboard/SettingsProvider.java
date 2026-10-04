package dev.rgbpulse.gboard;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import java.util.Map;

/** Read-only settings IPC for the actual Gboard UID, not world-readable XML. */
public final class SettingsProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private boolean isGboard(){
        String[] packages=getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if(packages!=null)for(String name:packages)if(SettingsContract.GBOARD.equals(name))return true;
        return false;
    }
    @Override public Bundle call(String method,String arg,Bundle extras){
        boolean gboard=isGboard();
        if(!gboard&&Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException("Settings caller not allowed");
        synchronized(SettingsStore.LOCK){
        SharedPreferences prefs=getContext().getSharedPreferences(Config.PREFS,Context.MODE_PRIVATE);
        if("read".equals(method)){
            Bundle data=new Bundle();
            for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
                Object value=entry.getValue();String key=entry.getKey();
                // No imported font blobs or other large/private application data in IPC.
                if("fontData9".equals(key))continue;
                if(value instanceof Boolean)data.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer)data.putInt(key,(Integer)value);
                else if(value instanceof String&&((String)value).length()<=4096)data.putString(key,(String)value);
            }
            return data;
        }
        if("ack".equals(method)){
            if(!gboard)throw new SecurityException("Only Gboard may acknowledge settings");
            String received=extras==null?null:extras.getString(SettingsContract.REVISION);
            String current=Config.string(prefs,SettingsContract.REVISION,"");
            boolean accepted=RevisionGate.accepts(current,received,SettingsContract.RUNTIME_VERSION,extras==null?0:extras.getInt("runtimeVersion",0));
            if(accepted)getContext().getSharedPreferences(SettingsContract.STATUS,Context.MODE_PRIVATE).edit()
                .putString(SettingsContract.REVISION,received).putInt("runtimeVersion",SettingsContract.RUNTIME_VERSION).putLong("receivedAt",System.currentTimeMillis()).apply();
            Bundle reply=new Bundle();reply.putBoolean("accepted",accepted);return reply;
        }
        throw new IllegalArgumentException("Unsupported settings operation");
        }
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
