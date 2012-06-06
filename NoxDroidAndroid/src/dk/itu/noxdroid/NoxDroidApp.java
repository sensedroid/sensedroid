package dk.itu.noxdroid;

import java.util.Hashtable;
import java.util.UUID;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import dk.itu.noxdroid.database.NoxDroidDbAdapter;

public class NoxDroidApp extends Application implements OnSharedPreferenceChangeListener {
	//private DbAdapter dbAdapter;
	private NoxDroidDbAdapter dbAdapter;
	private String TAG;
	private UUID currentTrack = null;
	private SharedPreferences APP_PREFS;
	private static double GPSDelta = 0.0;
	private Hashtable<Class<?>, Boolean> sensorStates = new Hashtable<Class<?>, Boolean>();
	private static double NOXDelta = 0.0;
	
	@Override
	public void onCreate() {
		super.onCreate();
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		Log.i(TAG, "Created NoxdroidApp");
		
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		APP_PREFS = PreferenceManager.getDefaultSharedPreferences(this);
		GPSDelta = Double.parseDouble(APP_PREFS.getString("GPS_DELTA", "10.0"));
		NOXDelta = Double.parseDouble(APP_PREFS.getString("NOX_DELTA_MIN", "0.5"));
	}
	
	public NoxDroidDbAdapter getDbAdapter() {
		if (dbAdapter == null) {
			NoxDroidDbAdapter.initInstance(this);
			dbAdapter = NoxDroidDbAdapter.getInstance();
			Log.i(TAG, "Init DbAdapter");
		}
		
		return dbAdapter;
	}
	
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Log.d(TAG, "onTerminate called");
		if (dbAdapter != null)  {
			dbAdapter.close();
		}
	}
	
	public void setCurrentTrack(UUID uuid) {
		this.currentTrack = uuid;
	}
	public UUID getCurrentTrack() {
		return this.currentTrack;
	}
	
	public void updateState(Class<?> c, boolean state) {
		sensorStates.put(c, state);
	}
	
	public boolean getState(Class<?> c) 
	{
		return sensorStates.containsKey(c) && sensorStates.get(c); 
	}
	
	public SharedPreferences getAppPrefs() {
		return APP_PREFS;
	}
	
	public static double getGPSDelta() {
		return GPSDelta;
	} 
	public static double getNOXDelta() {
		return NOXDelta;
	} 

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		APP_PREFS = PreferenceManager.getDefaultSharedPreferences(this);
	}
}
