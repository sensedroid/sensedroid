package dk.itu.noxdroid.setup;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import dk.itu.noxdroid.R;

public class PreferencesActivity extends PreferenceActivity {
	
	private String TAG; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
			.getClass().getSimpleName());
		
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		Log.d(TAG, "Called Preferences Activity");
	}
	
	
}
