package dev.rgbpulse.gboard;

import android.app.PendingIntent;
import android.content.*;
import android.os.*;

/** Signed push snapshots with one-shot receipts. No Gboard -> provider dependency. */
final class SettingsClient {
    interface Listener {void apply(Config config);}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static Context context;
    private static Listener listener;
    private static boolean registered;
    private static long lastStamp;
    private static String currentRevision="";
    private static final Runnable failClosed=()->{
        currentRevision="";if(listener!=null)listener.apply(new Config());
        android.util.Log.w("RGBPulse","No signed settings reply: effects disabled. Open the module and Apply.");
    };
    private static final Runnable pull=()->{
        if(context==null)return;
        try{
            context.sendBroadcast(new Intent(SettingsContract.PULL).setComponent(
                new ComponentName("dev.rgbpulse.gboard","dev.rgbpulse.gboard.SettingsRelay")));
            MAIN.removeCallbacks(failClosed);MAIN.postDelayed(failClosed,3000);
        }catch(RuntimeException e){failClosed.run();}
    };
    static void start(Context ctx,Listener callback){
        context=ctx.getApplicationContext()==null?ctx:ctx.getApplicationContext();listener=callback;
        if(!registered){
            try{
                context.registerReceiver(new BroadcastReceiver(){
                    @Override public void onReceive(Context c,Intent intent){
                        if(!SettingsContract.ACTION.equals(intent.getAction()))return;
                        if(intent.getIntExtra("runtimeVersion",0)!=SettingsContract.RUNTIME_VERSION)return;
                        Bundle data=intent.getBundleExtra("snapshot");
                        PendingIntent receipt=intent.getParcelableExtra("receipt",PendingIntent.class);
                        long stamp=intent.getLongExtra("stamp",0);
                        if(data==null||receipt==null||stamp<lastStamp)return;
                        String revision=data.getString(SettingsContract.REVISION,"");if(revision.isEmpty())return;
                        try{
                            Config next=Config.from(new BundlePreferences(data));
                            if(!revision.equals(currentRevision))listener.apply(next);
                            lastStamp=stamp;currentRevision=revision;
                            MAIN.removeCallbacks(failClosed);MAIN.removeCallbacks(pull);
                            if(isOrderedBroadcast())setResultCode(SettingsContract.RUNTIME_VERSION);
                            receipt.send(); // Only after main-thread application/cleanup succeeds.
                        }catch(PendingIntent.CanceledException e){android.util.Log.w("RGBPulse","Receipt already consumed",e);}
                        catch(RuntimeException e){failClosed.run();android.util.Log.w("RGBPulse","Snapshot failed",e);}
                    }
                },new IntentFilter(SettingsContract.ACTION),SettingsContract.PERMISSION,null,Context.RECEIVER_EXPORTED);
                registered=true;
            }catch(RuntimeException e){android.util.Log.w("RGBPulse","Settings receiver registration failed",e);}
        }
        request();
    }
    static void request(){MAIN.removeCallbacks(pull);MAIN.postDelayed(pull,80);}
}
