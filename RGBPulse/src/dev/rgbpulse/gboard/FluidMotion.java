package dev.rgbpulse.gboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Device tilt for the magnetic fluid. Projects gravity onto the screen plane so
 * the liquid slides toward whichever edge the phone tips down. Registered only
 * while the fluid is enabled; unregistered on hide/dispose, never at boot.
 */
final class FluidMotion implements SensorEventListener {
    private static volatile float tiltX, tiltY;
    private final SensorManager sensors;
    private Sensor sensor;
    private boolean accelerometer, registered;

    static float tiltX(){return tiltX;}
    static float tiltY(){return tiltY;}

    FluidMotion(Context context){
        SensorManager found=null;
        try{
            Context app=context.getApplicationContext();
            found=(SensorManager)(app!=null?app:context).getSystemService(Context.SENSOR_SERVICE);
        }catch(RuntimeException ignored){}
        sensors=found;
    }
    void start(){
        if(sensors==null||registered)return;
        try{
            sensor=sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);accelerometer=false;
            if(sensor==null){sensor=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);accelerometer=true;}
            if(sensor==null)return;
            registered=sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME);
        }catch(RuntimeException e){registered=false;}
    }
    void stop(){
        if(sensors!=null&&registered){
            try{sensors.unregisterListener(this);}catch(RuntimeException ignored){}
        }
        registered=false;
    }
    @Override public void onSensorChanged(SensorEvent event){
        float[] v=event.values;
        if(v==null||v.length<2)return;
        // Accelerometer reports reaction force (sign-flipped gravity) at rest.
        float nx=(accelerometer?v[0]:-v[0])/SensorManager.GRAVITY_EARTH;
        float ny=(accelerometer?v[1]:-v[1])/SensorManager.GRAVITY_EARTH;
        float mag=(float)Math.hypot(nx,ny);
        if(mag>1f){nx/=mag;ny/=mag;}
        if(Math.abs(nx)<.04f)nx=0; // A phone never rests perfectly level; ignore the noise.
        if(Math.abs(ny)<.04f)ny=0;
        tiltX+=(nx-tiltX)*.2f;
        tiltY+=(ny-tiltY)*.2f;
    }
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
}
