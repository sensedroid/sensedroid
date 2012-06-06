package dk.itu.noxdroid.experiments;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.DigitalOutput.Spec;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.service.NoxDroidService;

public class IOIOSensorActivity extends AbstractIOIOActivity {
	
	private NoxDroidService service;
	private String TAG;  
	private boolean flag = true;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		addToDebug("OnCreate");
		//doBindService();
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
//			service = ((NoxDroidService.ServiceBinder) binder).getService();
//			Log.i(TAG, "Connected to NoxDroidService");
		}
	};
	
	void doBindService() {
		bindService(new Intent(this, NoxDroidService.class), mConnection, Context.BIND_AUTO_CREATE);		
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private AnalogInput input_;

		private DigitalOutput led_;
		private int pinLed = 9;
		private int pinIn = 40;

		@Override
		public void setup() throws ConnectionLostException {
			addToDebug("Setup()");
			try {
				input_ = ioio_.openAnalogInput(pinIn);
				led_ = ioio_.openDigitalOutput(pinLed, Spec.Mode.NORMAL, true);

			} catch (ConnectionLostException e) {

				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			addToDebug("Loop");
			try {
				final float reading = input_.read();
				addToDebug(Float.toString(reading));
				led_.write(!flag);
				flag = flag ? false : true;
				sleep(1000);
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				throw e;
			}
		}
	}

	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		addToDebug("Return new IOIOThread");
		return new IOIOThread();
	}

	private void addToDebug(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, str);
			};
		});
		
	}
	
	@Override
	public void onBackPressed() {
		moveTaskToBack (true); 
	}

}