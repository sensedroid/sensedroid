/*
 * 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.itu.noxdroid.location;

import java.util.ArrayList;

import org.gavaghan.geodesy.GlobalCoordinates;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import dk.itu.noxdroid.NoxDroidApp;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.database.NoxDroidDbAdapter;
import dk.itu.noxdroid.service.NoxDroidService;
import dk.itu.noxdroid.util.GPSUtil;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application. The
 * {@link LocalServiceActivities.Controller} and
 * {@link LocalServiceActivities.Binding} classes show how to interact with the
 * service.
 * 
 * <p>
 * Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service. This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */

public class GPSLocationService extends Service {

	private String TAG;

	private LocationManager lm;
	private LocationListener locListenD;
	private Double latitude;
	private Double longitude;
	private NoxDroidDbAdapter mDbHelper;
	private int updateinterval;

	private ArrayList<Messenger> clients = new ArrayList<Messenger>();
	private ArrayList<Integer> msgQueue;
	public final Messenger _handler = new Messenger(new IncomingHandler());

	private boolean record = false;
	private GlobalCoordinates lastKnownPosition;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {

		public GPSLocationService getService() {
			Log.d(TAG, "LocalBinder called");
			return GPSLocationService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		msgQueue = new ArrayList<Integer>();
		//
		// Get handle for LocationManager
		//
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		//
		// Get the global database adapter
		// - this approach needs no open commands and such its handled with the
		// adapter
		//
		mDbHelper = ((NoxDroidApp) getApplication()).getDbAdapter();
		updateinterval = Integer.valueOf((String) PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						getString(R.string.GPS_UPDATE_INTERVAL), "2000"));

		Log.d(TAG, "GPS updateinterval: " + updateinterval);

		if (providerEnabled()) {
			notifyClients(NoxDroidService.STATUS_GPS_OK);
			Log.d(TAG, "GPS enabled");
		} else {
			notifyClients(NoxDroidService.ERROR_NO_GPS);
			Log.d(TAG, "GPS not enabled");
		}

		// ask the Location Manager to send us location updates
		locListenD = new DispLocListener();
		// bind to location manager - TODO: fine tune the variables
		// 30000L / minTime = the minimum time interval for notifications, in
		// milliseconds.
		// 10.0f / minDistance - the minimum distance interval for notifications
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				locListenD);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
				locListenD);
		lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0,
				locListenD);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		try {
			Log.d(TAG, "onDestroy called");
			// Location: close down / unsubscribe the location updates
			lm.removeUpdates(locListenD);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

		// the NoxDroid database don't need to be closed - are handled globally
		// // close database
		// mDbHelper.close();

	}

	@Override
	public IBinder onBind(Intent intent) {
		return _handler.getBinder();
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	/*
	 * Location listener
	 * 
	 * - could also have been implemented directly on the LocationService class
	 * but its convenient to split it out.
	 */
	private class DispLocListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {

			latitude = location.getLatitude();
			longitude = location.getLongitude();

			Log.i(TAG, "GPS LOC. lat: " + latitude + " lon: " + longitude
					+ " provider: " + location.getProvider());
			/**
			 * Add to database
			 */
			if (record) {
				GlobalCoordinates newPosition = new GlobalCoordinates(latitude, longitude);
				if (lastKnownPosition == null || GPSUtil.getGPSDelta(lastKnownPosition, newPosition) >= NoxDroidApp.getGPSDelta()) {
					mDbHelper.createLocationPoint(latitude, longitude,
							location.getProvider());
					lastKnownPosition = newPosition;
				}
			}
		}

		@Override
		public void onProviderDisabled(String arg0) {
			Log.d(TAG, "GPS_EVENT_STOPPED");
			if (providerEnabled()) {
				notifyClients(NoxDroidService.STATUS_GPS_OK);
			} else {
				notifyClients(NoxDroidService.ERROR_NO_GPS);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "GPS_EVENT_STARTED");
			if (providerEnabled()) {
				notifyClients(NoxDroidService.STATUS_GPS_OK);
			} else {
				notifyClients(NoxDroidService.ERROR_NO_GPS);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}
	}

	private boolean providerEnabled() {
		return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null
				|| lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null
				|| lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) != null;
	}

	private void startRecording() {
		Log.i(TAG, "Start rec");
		record = true;
		// ask the Location Manager to send us location updates
		// locListenD = new DispLocListener();
		// bind to location manager - TODO: fine tune the variables
		// 30000L / minTime = the minimum time interval for notifications, in
		// milliseconds.
		// 10.0f / minDistance - the minimum distance interval for notifications
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				locListenD);
	}

	private void stopRecording() {
		Log.i(TAG, "Stop rec");
		record = false;
		lm.removeUpdates(locListenD);
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Handling incoming message");
			switch (msg.what) {
			case NoxDroidService.MSG_REGISTER_CLIENT:
				Log.i(TAG, "Added client: " + msg.replyTo
						+ this.getClass().getSimpleName());
				clients.add(msg.replyTo);
				Location loc = lm
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (loc != null) {
					notifyClients(NoxDroidService.STATUS_GPS_OK);
				} else {
					notifyClients(NoxDroidService.ERROR_NO_GPS);
				}

				Log.d(TAG,
						"lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) is "
								+ loc);

				break;
			case NoxDroidService.ACTION_START_TRACK:
				startRecording();
				break;
			case NoxDroidService.ACTION_STOP_TRACK:
				stopRecording();
				break;
			case NoxDroidService.CHANGE_UPDATEINTERVAL_GPS:
				updateinterval = msg.getData().getInt(
						getString(R.string.GPS_UPDATE_INTERVAL));
				break;
			default:
				break;
			}
		}
	}

	private void notifyClients(int msg) {
		if (clients.size() > 0) {
			Log.i(TAG, "Notifying clients # " + clients.size());
			for (int i = 0; i < clients.size(); i++) {
				try {
					Log.i(TAG, "Sent message to : " + clients.get(i));
					clients.get(i).send(Message.obtain(null, msg));

					for (Integer m : msgQueue) {
						clients.get(i).send(Message.obtain(null, m));
					}

					msgQueue.clear();
				} catch (RemoteException e) {
					// If we get here, the client is dead, and we should remove
					// it
					// from the list
					Log.e(TAG, "Removing client: " + clients.get(i));
					clients.remove(i);
				}
			}
		} else {
			msgQueue.add(msg);
		}
	}
}