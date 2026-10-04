package dev.rgbpulse.gboard;

import java.io.File;
import java.util.concurrent.TimeUnit;

/** Called on a worker only after Save & restart confirmation and successful persistence. */
final class GboardRestart {
    static boolean stop(){
        Process process=null;
        try{
            process=new ProcessBuilder("su","-c","am force-stop --user current com.google.android.inputmethod.latin")
                .redirectErrorStream(true).redirectOutput(new File("/dev/null")).start();
            return process.waitFor(25,TimeUnit.SECONDS)&&process.exitValue()==0;
        }catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
        catch(Exception e){return false;}
        finally{if(process!=null){if(process.isAlive())process.destroyForcibly();else process.destroy();}}
    }
}
