package dev.rgbpulse.gboard;

import android.content.*;
import android.os.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Serialized settings handoff. Notifications contain no configuration payload. */
final class SettingsClient {
    interface Listener {void apply(Config config);}
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final AtomicLong generation=new AtomicLong();
    private static final AtomicBoolean scheduled=new AtomicBoolean();
    private static Context context;
    private static Listener listener;
    private static boolean registered;
    static void start(Context ctx,Listener callback){
        context=ctx.getApplicationContext()==null?ctx:ctx.getApplicationContext();listener=callback;
        if(!registered){
            try{
                context.registerReceiver(new BroadcastReceiver(){
                    @Override public void onReceive(Context c,Intent i){if(SettingsContract.ACTION.equals(i.getAction()))request();}
                },new IntentFilter(SettingsContract.ACTION),SettingsContract.PERMISSION,null,Context.RECEIVER_EXPORTED);
                registered=true;
            }catch(RuntimeException e){android.util.Log.w("RGBPulse","Live settings notification unavailable; reload on keyboard open",e);}
        }
        request();
    }
    static void request(){generation.incrementAndGet();schedule();}
    private static void schedule(){
        if(context==null||!scheduled.compareAndSet(false,true))return;
        final long request=generation.get();final Context ctx=context;
        IO.execute(()->{
            Bundle loaded=null;
            try{loaded=ctx.getContentResolver().call(SettingsContract.URI,"read",null,null);}
            catch(RuntimeException e){android.util.Log.w("RGBPulse","Settings read failed; effects will be disabled",e);}
            final Bundle data=loaded;
            MAIN.post(()->{
                try{
                    if(request!=generation.get())return; // Never apply a superseded response.
                    Config next=new Config();String revision="";
                    if(data!=null){
                        revision=data.getString(SettingsContract.REVISION,"");
                        if(!revision.isEmpty())next=Config.from(new BundlePreferences(data));
                    }
                    listener.apply(next); // Restore old visuals first, then apply this snapshot.
                    if(data!=null&&!revision.isEmpty()){
                        final String appliedRevision=revision;
                        IO.execute(()->{
                            Bundle receipt=new Bundle();receipt.putString(SettingsContract.REVISION,appliedRevision);
                            try{ctx.getContentResolver().call(SettingsContract.URI,"ack",null,receipt);}
                            catch(RuntimeException e){android.util.Log.w("RGBPulse","Settings applied but acknowledgement failed",e);}
                        });
                    }
                }catch(RuntimeException e){
                    android.util.Log.w("RGBPulse","Settings application failed; disabling effects",e);
                    listener.apply(new Config());
                }finally{
                    scheduled.set(false);if(request!=generation.get())schedule();
                }
            });
        });
    }
}
