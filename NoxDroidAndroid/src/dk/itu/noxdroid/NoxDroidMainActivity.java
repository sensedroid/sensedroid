package dk.itu.noxdroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import dk.itu.noxdroid.service.NoxDroidService;
import dk.itu.noxdroid.setup.PreferencesActivity;
import dk.itu.noxdroid.tracks.TracksListActivity;
import dk.itu.noxdroid.util.Line;

public class NoxDroidMainActivity extends Activity {

	private String TAG;

	/********** DECLARES *************/

	private RelativeLayout layoutGPS;
	private RelativeLayout layoutIOIO;
	private RelativeLayout layoutConn;
	private RelativeLayout layoutWrapper;
	private RelativeLayout wrapper;
	private RelativeLayout parentWrapper;
	private ImageButton imgBtnStart;
	private ImageButton imgBtnGPS;
	private ImageView imgGPS;
	private ImageButton imgBtnIOIO;
	private ImageView imgIOIO;
	private ImageButton imgBtnConn;
	private ImageView imgConn;
	private ImageButton imgBtnStop;

	private RelativeLayout.LayoutParams lp;
	private boolean isBound;
	private Messenger msg_service;
	private NoxDroidApp app;
	private Vibrator vibrator;

	private static final int SHOW_EXIT_DIALOG = 1;
	private static final int SHOW_LOCATION = 2;
	private static final int SHOW_ABOUT = 3;
	private static final int SHOW_IOIO = 4;
	private static final int SHOW_HELP = 5;
	private static final int SHOW_CONNECTIVITY = 6;
	private Builder builder;
	private boolean updateSensorStates = false;  
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			msg_service = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			// service = ((NoxDroidService.ServiceBinder) binder).getService();
			Log.i(TAG, "Connected to NoxDroidService");

			try {
				msg_service = new Messenger(binder);
				Message msg = Message.obtain(null,
						NoxDroidService.MSG_REGISTER_CLIENT);
				msg.replyTo = messenger;
				msg_service.send(msg);
				
				Log.i(TAG, "Registered messenger to NoxDroidService");
				if (updateSensorStates) {
					msg = Message.obtain(null,
							NoxDroidService.GET_SENSOR_STATES);
					msg.replyTo = messenger;
					msg_service.send(msg);
					updateSensorStates = false;
				}
				
			} catch (RemoteException e) {

			}
		}
	};
	
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
		setContentView(R.layout.main2);

		app = (NoxDroidApp) getApplication();

		/********** INITIALIZES *************/
		imgBtnStart = (ImageButton) findViewById(R.id.imgBtnStart);
		imgBtnStart.setEnabled(false);
		imgBtnStop = (ImageButton) findViewById(R.id.imgBtnStop);
		imgBtnStop.setEnabled(false);
		imgBtnGPS = (ImageButton) findViewById(R.id.imgBtnGPS);
		imgBtnGPS.setTag(NoxDroidService.ERROR_NO_LOCATION);
		imgGPS = (ImageView) findViewById(R.id.imgGPS);
		imgBtnIOIO = (ImageButton) findViewById(R.id.imgBtnIOIO);
		imgBtnIOIO.setEnabled(true);
		imgIOIO = (ImageView) findViewById(R.id.imgIOIO);
		imgBtnConn = (ImageButton) findViewById(R.id.imgBtnConn);
		imgConn = (ImageView) findViewById(R.id.imgConn);
		layoutConn = (RelativeLayout) findViewById(R.id.relLayoutConnection);
		layoutGPS = (RelativeLayout) findViewById(R.id.relLayoutGPS);
		layoutIOIO = (RelativeLayout) findViewById(R.id.relLayoutIOIO);
		layoutWrapper = (RelativeLayout) findViewById(R.id.relLayoutWrapper);
		wrapper = (RelativeLayout) findViewById(R.id.wrapper);
		parentWrapper = (RelativeLayout) findViewById(R.id.parentWrapper);

		/* Please visit http://www.ryangmattison.com for updates */
		((ImageView) findViewById(R.id.imgIOIO)).setAlpha(80);
		((ImageView) findViewById(R.id.imgGPS)).setAlpha(80);
		((ImageView) findViewById(R.id.imgConn)).setAlpha(80);

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT);

		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// X, y of left is 60, (height - 360) + 180
		float heightDP = (metrics.heightPixels - (60 * metrics.density))
				/ metrics.density;
		float[] points = { metrics.widthPixels / 2, heightDP,
				60 * metrics.density, 360 * metrics.density / 2 };
		float[] points2 = { metrics.widthPixels / 2, heightDP,
				metrics.widthPixels - (60 * metrics.density),
				360 * metrics.density / 2 };
		Line l = new Line(this, points);
		Line l2 = new Line(this, points2);
		layoutWrapper.addView(l, 0);
		layoutWrapper.addView(l2, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		doUnbindService();
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onCreate");
		
		
		if (app.getCurrentTrack() != null) {
			updateGUI(NoxDroidService.STATUS_RECORDING);
		} else if (msg_service != null) {
			Message msg = Message.obtain(null,
					NoxDroidService.GET_SENSOR_STATES);
			msg.replyTo = messenger;
			try {
				msg_service.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}else {
			updateSensorStates = true;
		}
		doBindService();
	}

	void doBindService() {
		Intent intent = new Intent(this, NoxDroidService.class);
		intent.putExtra("Main Activity", messenger);
		bindService(new Intent(this, NoxDroidService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	void doUnbindService() {
		Log.d(TAG, "doUnbindService");
		if (isBound) {
			if (msg_service != null) {
				try {
					Message msg = Message.obtain(null,
							NoxDroidService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = messenger;
					msg_service.send(msg);
				} catch (RemoteException e) {
				}
			}

			// Detach our existing connection.
			unbindService(mConnection);
			isBound = false;
		}
	}

	/*
	 * Start track - send message(s) to the underlying service(s)
	 */
	public void startTrack(View view) {
		vibrator.vibrate(100);
		updateGUI(NoxDroidService.ACTION_START_TRACK);
		Message msg = Message.obtain(null, NoxDroidService.ACTION_START_TRACK);
		msg.replyTo = messenger;
		try {
			msg_service.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage());
		}
		Log.i(TAG, "ACTION_START_TRACK sent to NoxDroidService");

	}

	/*
	 * Stop track - send message(s) to the underlying service(s)
	 */
	public void endTrack(View view) {
		vibrator.vibrate(100);
		updateGUI(NoxDroidService.ACTION_STOP_TRACK);
		Toast.makeText(this, "stopping service", Toast.LENGTH_SHORT);
		Message msg = Message.obtain(null, NoxDroidService.ACTION_STOP_TRACK);
		msg.replyTo = messenger;
		try {
			msg_service.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage());
		}
		Log.i(TAG, "ACTION_STOP_TRACK sent to NoxDroidService");
	}

	public void imgBtnGPS_onClick(View view) {
		vibrator.vibrate(100);
		showDialog(SHOW_LOCATION);
	}
	
	public void imgBtnIOIO_onClick(View view) {
		vibrator.vibrate(100);
		showDialog(SHOW_IOIO);
	}
	
	public void btnHelp_onClick(View view) {
		vibrator.vibrate(100);
		Log.d(TAG, "Help clicked");
		showDialog(SHOW_HELP);
	}
	
	public void imgBtnConn_onClick(View view) {
		vibrator.vibrate(100);
		showDialog(SHOW_CONNECTIVITY);
	}

	private boolean isServiceRunning(Class<?> service) {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo rs : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (service.getName().equals(rs.service.getClassName())) {
				Log.i(TAG, "Noxdroid Service running");
				return true;
			}
		}
		return false;
	}

	/**
	 * Section
	 */

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG,
					"Handling incoming message from " + msg.describeContents());
			updateGUI(msg.what);
		}
	}

	final Messenger messenger = new Messenger(new IncomingHandler());

	private void updateGUI(int status) {
		switch (status) {
		case NoxDroidService.STATUS_IOIO_CONNECTED:
			imgBtnIOIO.setImageResource(R.drawable.circle_green);
			Toast.makeText(getBaseContext(), "IOIO Connected",
					Toast.LENGTH_LONG);
			Log.i(TAG, "IOIO Connected");
			break;
		case NoxDroidService.ERROR_IOIO_ABORTED:
		case NoxDroidService.ERROR_IOIO_CONNECTION_LOST:
			imgBtnIOIO.setImageResource(R.drawable.circle_red);
			Toast.makeText(getBaseContext(), "IOIO Lost connection",
					Toast.LENGTH_LONG);
			Log.i(TAG, "IOIO Not Connected"); // this is strictly not an error right?
			break;
		case NoxDroidService.ACTION_START_TRACK:
			imgBtnStart.setVisibility(View.GONE);
			imgBtnStop.setVisibility(View.VISIBLE);
			imgBtnStop.setEnabled(true);
			break;
		case NoxDroidService.ACTION_STOP_TRACK:
			imgBtnStop.setEnabled(false);
			imgBtnStart.setVisibility(View.VISIBLE);
			imgBtnStop.setVisibility(View.GONE);
			imgBtnStart.setEnabled(true);
			Log.d(TAG, "Stop track");

			break;
		case NoxDroidService.STATUS_SERVICE_READY:
			imgBtnConn.setImageResource(R.drawable.circle_green);
			imgBtnIOIO.setImageResource(R.drawable.circle_green);
			imgBtnGPS.setImageResource(R.drawable.circle_green);
			imgBtnGPS.setTag(NoxDroidService.STATUS_LOCATION_OK);
			imgBtnStart.setImageResource(R.drawable.play);
			imgBtnStart.setEnabled(true);
			break;
		case NoxDroidService.STATUS_RECORDING:
			imgBtnStart.setVisibility(View.GONE);
			imgBtnStop.setVisibility(View.VISIBLE);
			break;
		case NoxDroidService.STATUS_CONNECTIVITY_OK:
			imgBtnConn.setImageResource(R.drawable.circle_green);
			imgConn.setVisibility(View.VISIBLE);
			break;
		case NoxDroidService.ERROR_NO_CONNECTIVITY:
			imgBtnConn.setImageResource(R.drawable.circle_red);
			imgConn.setVisibility(View.VISIBLE);
			break;
		case NoxDroidService.STATUS_LOCATION_OK:
			imgBtnGPS.setImageResource(R.drawable.circle_green);
			imgGPS.setVisibility(View.VISIBLE);
			imgBtnGPS.setTag(NoxDroidService.STATUS_LOCATION_OK);
			break;
		
		case NoxDroidService.ERROR_NO_LOCATION:
			if (imgBtnGPS.getTag().equals(NoxDroidService.STATUS_LOCATION_OK)) {
				showDialog(SHOW_LOCATION);
			}
			imgBtnGPS.setImageResource(R.drawable.circle_red);
			imgGPS.setVisibility(View.VISIBLE);
			imgBtnGPS.setTag(NoxDroidService.ERROR_NO_LOCATION);
		default:
			break;
		}
	}

	/*
	 * Menu
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			startActivity(new Intent(NoxDroidMainActivity.this,
					PreferencesActivity.class));
			// test toast:
			// Toast.makeText(this, "Just a test", Toast.LENGTH_SHORT).show();
			break;
		case R.id.post_to_cloud:
			startActivity(new Intent(NoxDroidMainActivity.this,
					TracksListActivity.class));
			break;
		case R.id.exitapp:
			openDialog(SHOW_EXIT_DIALOG);
			break;
		case R.id.about : 
			openDialog(SHOW_ABOUT);
			break;
		}
		
		return true;
	}

	/***
	 * 
	 */

	public void openDialog(int id) {
		showDialog(id);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case SHOW_EXIT_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.DIALOG_MSG_EXIT));
			builder.setCancelable(true);
			builder.setTitle("NOxDroid");
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							stopService(new Intent(NoxDroidMainActivity.this,
									NoxDroidService.class));
							NoxDroidMainActivity.this.finish();
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(getApplicationContext(),
									"NOxDroid will continue", Toast.LENGTH_LONG)
									.show();
						}
					});
			builder.create().show();
			
			break;
		case SHOW_LOCATION:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(dk.itu.noxdroid.R.string.DIALOG_MSG_LOCATION));
			builder.setTitle("Location service");
			builder.setCancelable(true);
			builder.setPositiveButton("GPS", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent gpsOptionsIntent = new Intent(
							android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(gpsOptionsIntent);
				}
			});
			builder.setNeutralButton("Mobile Data", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final  Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					final ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
					intent.setComponent(cn);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			});
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
			break;
			
		case SHOW_ABOUT : 
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.DIALOG_MSG_NOXDROID_ABOUT));
			builder.setTitle("About NOxDroid");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
				
			});
			builder.create().show();
			break;
		case SHOW_IOIO : 
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.DIALOG_MSG_IOIO_HELP));
			builder.setTitle("IOIO");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
			break;
		case SHOW_HELP :
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.DIALOG_MSG_HELP));
			builder.setTitle("Help");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
			break;
		case SHOW_CONNECTIVITY :
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.DIALOG_MSG_CONNECTIVITY));
			builder.setTitle("Connectivity");
			builder.setPositiveButton("Mobile Data", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final  Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					final ComponentName cn = new ComponentName("com.android.phone","com.android.phone.Settings");
					intent.setComponent(cn);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			});
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			builder.create().show();
			break;
		}
		return super.onCreateDialog(id);
	}
	
}