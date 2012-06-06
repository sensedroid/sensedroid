package dk.itu.noxdroid.location;

import java.util.ArrayList;

import org.gavaghan.geodesy.GlobalCoordinates;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.WPSStreetAddressLookup;
import com.skyhookwireless.wps.XPS;

import dk.itu.noxdroid.NoxDroidApp;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.database.NoxDroidDbAdapter;
import dk.itu.noxdroid.service.NoxDroidService;
import dk.itu.noxdroid.util.GPSUtil;

/*
 * NoxDroidLocationService
 * 
 * 
 */
public class SkyHookLocationService extends Service {

	private XPS _xps;
	private NoxDroidDbAdapter mDbHelper;
	private SkyhookLocationCallBack _callback = new SkyhookLocationCallBack();
	private GlobalCoordinates lastKnownPosition = null;

	private String TAG;
	private ArrayList<Messenger> clients = new ArrayList<Messenger>();

	private int updateinterval = 3000;

	final WPSAuthentication auth = new WPSAuthentication("noxdroid",
			"dk.itu.noxdroid");

	// our Handler understands three messages:
	// a location, an error, or a finished request
	private boolean doCheck = true;

	public final Messenger _handler = new Messenger(new IncomingHandler());

	private int retries = 0;
	private ArrayList<Integer> msgQueue;
	

	@Override
	public IBinder onBind(Intent arg0) {
		return _handler.getBinder();
	}

	public class ServiceBinder extends Binder {
		public SkyHookLocationService getService() {
			return SkyHookLocationService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		_xps = new XPS(this);
		
		msgQueue = new ArrayList<Integer>();

		mDbHelper = ((NoxDroidApp) getApplication()).getDbAdapter();
		doCheck = true;

//		_xps.getLocation(auth,
//				WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, _callback);
		
		_xps.getPeriodicLocation(auth,
				WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,updateinterval, 0, _callback);

		// before this is set it has a value of: 10000 view
		// http://screencast.com/t/nSXoYDm54aw4
		updateinterval = Integer.valueOf((String) ((NoxDroidApp) getApplication()).getAppPrefs().getString("SKYHOOK_UPDATE_INTERVAL", "2000"));
		Log.d(TAG, "Skyhook updateinterval: " + updateinterval);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		_xps.abort();
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Handling incoming message");
			switch (msg.what) {
			case NoxDroidService.MSG_REGISTER_CLIENT:
				Log.i(TAG, "Added client: " + msg.replyTo
						+ " NoxDroidLocationService");
				clients.add(msg.replyTo);
				break;
			case NoxDroidService.ACTION_START_TRACK:
				startRecording();
				break;
			case NoxDroidService.ACTION_STOP_TRACK:
				stopRecording();
				break;
			case NoxDroidService.ACTION_SKYHOOK_DOTEST :
				retries = 0;
				doCheck = true;
				_xps.getPeriodicLocation(auth,
						WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,updateinterval, 0, _callback);
				break;
			case NoxDroidService.CHANGE_UPDATEINTERVAL_SKYHOOK :
				updateinterval = msg.getData().getInt(getString(R.string.SKYHOOK_UPDATE_INTERVAL));
				break;
			default:
				break;
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		return START_STICKY;
	}

	private void notifyClients(int msg) {
		Log.i(TAG, "Notifying clients # " + clients.size());
		if (clients.size()>0) {
		for (int i = 0; i < clients.size(); i++) {
			try {
				Log.i(TAG, "Sent message to : " + clients.get(i));
				clients.get(i).send(Message.obtain(null, msg));
				for (Integer m : msgQueue) {
					clients.get(i).send(Message.obtain(null, m));
				}
				msgQueue.clear();
			} catch (RemoteException e) {
				// If we get here, the client is dead, and we should remove it
				// from the list
				Log.e(TAG, "Removing client: " + clients.get(i));
				clients.remove(i);
			}
		}
		} else {
			msgQueue.add(msg);
		}
	}

	private class SkyhookLocationCallBack implements
			WPSPeriodicLocationCallback, WPSLocationCallback  {
		@Override
		public WPSContinuation handleError(WPSReturnCode error) {
			Log.e(TAG, error.toString());
			if (doCheck) {
				// Allow 1 retry
				if (retries >=1 ) {
					notifyClients(NoxDroidService.ERROR_NO_SKYHOOK);
					retries = 0;
					doCheck = false;
					_xps.abort();
					return WPSContinuation.WPS_STOP;
				} 
				else {
					Log.d(TAG, "Skyhook retry");
					retries++;
				}
			} 
			return WPSContinuation.WPS_CONTINUE;
			
		}

		@Override
		public void done() {
			Log.i(TAG, "Skyhook done");
		}

		@Override
		public WPSContinuation handleWPSPeriodicLocation(WPSLocation location) {
			
			Log.i(TAG, "Got PeriodicLocation");
			Log.i(TAG, String.format("lat: %s - long: %s - acc: %s",
					location.getLatitude(), location.getLongitude(),
					location.getSpeed()));
			if (doCheck) {
				Log.i(TAG, "check Performed");
				notifyClients(NoxDroidService.STATUS_SKYHOOK_OK);
				doCheck = false;
				retries = 0;
				return WPSContinuation.WPS_STOP;
			} else {
				GlobalCoordinates newPosition = new GlobalCoordinates(location.getLatitude(), location.getLatitude());
				if (lastKnownPosition == null || GPSUtil.getGPSDelta(lastKnownPosition, newPosition) >= NoxDroidApp.getGPSDelta()) {
					mDbHelper.createLocationPoint(location.getLatitude(),
						location.getLongitude(), "skyhook");
					Log.i(TAG, "Saved Location to database");
					lastKnownPosition = newPosition; 
				} 

				try {
					Thread.sleep(updateinterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return WPSContinuation.WPS_CONTINUE;	
			}
		}
		
		@Override
		public void handleWPSLocation(WPSLocation location) {
			Log.i(TAG, "Got Location");
			Log.i(TAG, String.format("lat: %s - long: %s",
					location.getLatitude(), location.getLongitude()));

			if (doCheck) {
				Log.d(TAG, "check Performed");
				notifyClients(NoxDroidService.STATUS_SKYHOOK_OK);
				doCheck = false;
				retries = 0;
			} else {
				mDbHelper.createLocationPoint(location.getLatitude(),
						location.getLongitude(), "skyhook");
				Log.d(TAG, "Saved Location to database");
			}
		}
	}

	public void startRecording() {
		_xps.getPeriodicLocation(auth,
				WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,updateinterval, 0, _callback);
	}

	public void stopRecording() {
		_xps.abort();
	}	
}