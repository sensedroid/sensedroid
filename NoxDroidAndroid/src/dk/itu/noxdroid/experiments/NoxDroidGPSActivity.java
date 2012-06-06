package dk.itu.noxdroid.experiments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.service.NoxDroidService;

public class NoxDroidGPSActivity extends Activity {
	private String TAG; 

	private NoxDroidService service;

	private LocationManager locationManager;
	private LocationListener locationListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		if (!locationManager.isProviderEnabled(LOCATION_SERVICE)) {
			Intent gpsOptionsIntent = new Intent(
					android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(gpsOptionsIntent);
		} else {
			locationListener = new NoxDroidGPSListener();
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		}

	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			service = ((NoxDroidService.ServiceBinder) binder).getService();
			Log.i("", "Connected to NoxDroidService");
		}
	};

	void doBindService() {
		bindService(new Intent(this, NoxDroidService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	public class NoxDroidGPSListener implements LocationListener {

		@Override
		public void onProviderDisabled(String provider) {
			Toast.makeText(getApplicationContext(), "Gps Disabled",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			Toast.makeText(getApplicationContext(), "Gps Enabled",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onLocationChanged(Location location) {
			location.getLatitude();
			location.getLongitude();

			String Text = "My current location is: " + "Latitud = "
					+ location.getLatitude() + "Longitud = "
					+ location.getLongitude();
			Toast.makeText(getApplicationContext(), Text, Toast.LENGTH_SHORT)
					.show();
		}
	}
}