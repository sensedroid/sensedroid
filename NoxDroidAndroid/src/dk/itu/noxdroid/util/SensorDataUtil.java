package dk.itu.noxdroid.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;


public class SensorDataUtil {
//	Sensor output
//	2.2 ± 0.5 µA/ppm
	
	/**
	 * 
	 * @param a the output in muA received from the sensor 
	 * @return the sensor data point converted to mu grames per sq. meter
	 */
	public static float muAtoMuGrames(float a) {
		return (float ) 1880.0f * a / 2.2f;
	}
	
	public static boolean isServiceRunning(Context context, Class<?> service) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo rs : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (service.equals(rs.service.getClass())) {
	            return true;
	        }
	    }
	    return false;
	}
}
