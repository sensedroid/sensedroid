package dk.itu.noxdroid.ioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.DigitalOutput.Spec;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import dk.itu.noxdroid.NoxDroidApp;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.database.NoxDroidDbAdapter;
import dk.itu.noxdroid.service.NoxDroidService;

public class NoxDroidIOIOThread extends Thread {
	
	private NoxDroidDbAdapter dbAdapter;
	private String TAG;
	/** Subclasses should use this field for controlling the IOIO. */
	protected IOIO ioio_;
	private boolean abort_ = false;
	private boolean connected_ = false;

	private AnalogInput input_;

	private DigitalOutput ledGreen_;
	private DigitalOutput ledYellow_;
	private DigitalOutput ledRed_;
	private int pinGreen = 16;
	private int pinYellow = 18;
	private int pinledRed = 20;
	private int pinAnalogIn = 41;
	
	private double green_upper_bound;
	private double yellow_upper_bound;
	
	private float lastKnownNOX = -1;	
	private int updateinterval = 2000;
	
	private ArrayList<IOIOEventListener> listeners = new ArrayList<IOIOEventListener>();
	private NoxDroidService service;

	public NoxDroidIOIOThread(NoxDroidService service) {
		this.service = service;
		listeners.add(service);
		dbAdapter = ((NoxDroidApp) service.getApplication()).getDbAdapter();
		TAG = service.getString(R.string.LOGCAT_TAG, service
				.getString(R.string.app_name), this.getClass().getSimpleName());

		updateinterval = Integer.valueOf((String)service.getPrefs().get("IOIO_UPDATE_INTERVAL"));
		Log.d(TAG, "IOIO updateinterval: " +  String.valueOf(updateinterval));
		pinAnalogIn = Integer.valueOf((String) service.getPrefs().get("IOIO_NO2_PIN"));
		Log.d(TAG, "NO2 pin: " + pinAnalogIn);
		green_upper_bound = (double) Double.valueOf((String) service.getPrefs().get("NOX_GREEN_UPPER_BOUND"))/100;
		yellow_upper_bound = (double) Double.valueOf((String) service.getPrefs().get("NOX_YELLOW_UPPER_BOUND"))/100;
		Log.i(TAG, "green: " + green_upper_bound + " yellow: " + yellow_upper_bound);
	}

	/** Not relevant to subclasses. */
	@Override
	public final void run() {
		super.run();
		while (true) {
			try {
				synchronized (this) {
					if (abort_) {
						notifyEventchanged(NoxDroidService.ERROR_IOIO_ABORTED);
						break;
					}
					ioio_ = IOIOFactory.create();
				}
				ioio_.waitForConnect();

				connected_ = true;
				notifyEventchanged(NoxDroidService.STATUS_IOIO_CONNECTED);
				setup();
				while (!abort_) {
					loop();
				}
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				
				Log.e(TAG, "RUN" + e.getMessage());
				if (abort_) {
					notifyEventchanged(NoxDroidService.ERROR_IOIO_CONNECTION_LOST);
					break;
				}
			} catch (InterruptedException e) {
				notifyEventchanged(NoxDroidService.ERROR_IOIO_INTERRUPTED);
				Log.e(TAG, "RUN" + e.getMessage());
				ioio_.disconnect();
				break;
			} catch (IncompatibilityException e) {
				Log.e("AbstractIOIOActivity", "Incompatible IOIO firmware", e);
				notifyEventchanged(NoxDroidService.ERROR_IOIO_INCOMPATIBLE);
				incompatible();
				// nothing to do - just wait until physical disconnection
				try {
					ioio_.waitForDisconnect();
				} catch (InterruptedException e1) {
					Log.e(TAG, "RUN: " +  e.getMessage());
					ioio_.disconnect();
				}
			} catch (Exception e) {
				Log.e("AbstractIOIOActivity", "RUN: Unexpected exception caught", e);
				notifyEventchanged(NoxDroidService.ERROR_IOIO_CONNECTION_LOST);
				ioio_.disconnect();
				break;
			} finally {
				try {
					if (ioio_ != null) {
						ioio_.waitForDisconnect();
						if (connected_) {
							disconnected();
						}
					}
				} catch (InterruptedException e) {
					Log.e(TAG, "RUN" +  e.getMessage());
					notifyEventchanged(NoxDroidService.ERROR_IOIO_INTERRUPTED);
				}
			}
		}
	}

	/**
	 * Subclasses should override this method for performing operations to be
	 * done once as soon as IOIO communication is established. Typically, this
	 * will include opening pins and modules using the openXXX() methods of the
	 * {@link #ioio_} field.
	 */
	protected void setup() throws ConnectionLostException, InterruptedException {
		try {
			input_ = ioio_.openAnalogInput(pinAnalogIn);
			ledGreen_ = ioio_.openDigitalOutput(pinGreen, Spec.Mode.NORMAL,
					true);
			ledYellow_ = ioio_.openDigitalOutput(pinYellow, Spec.Mode.NORMAL,
					true);
			ledRed_ = ioio_
					.openDigitalOutput(pinledRed, Spec.Mode.NORMAL, true);

			/*
			 * Set up data base
			 * TODO: Not 100% sure about if service can be used as context ? 
			 */
		} catch (ConnectionLostException e) {			
			/* 
			 * Close database - also done in other exceptions
			 * TODO: verify when it should be closed
			 */

			Log.e(TAG, e.getMessage());
			notifyEventchanged(NoxDroidService.ERROR_IOIO_CONNECTION_LOST);

			throw e;
		}
	}

	/**
	 * Subclasses should override this method for performing operations to be
	 * done repetitively as long as IOIO communication persists. Typically, this
	 * will be the main logic of the application, processing inputs and
	 * producing outputs.
	 */
	protected void loop() throws ConnectionLostException, InterruptedException {
		try {
			//final float reading = SensorDataUtil.muAtoMuGrames(input_.read());
			final float reading = input_.read();
			
			if (reading < green_upper_bound ) {
				ledRed_.write(false);
				ledYellow_.write(false);
				ledGreen_.write(true);
			} else if (reading < yellow_upper_bound) {
				ledRed_.write(false);
				ledYellow_.write(true);
				ledGreen_.write(false);
			} else {
				ledRed_.write(true);
				ledYellow_.write(false);
				ledGreen_.write(false);
				
			}
			// TODO: probably just disable this part
			// because we are not going to send data directly back to the UI
			Object obj = (Object) reading;
			service.update(this.getClass(), obj);
			
			Log.d(TAG, "NOx level: " +  reading);
			if (lastKnownNOX==-1 || Math.abs(reading-lastKnownNOX) >= NoxDroidApp.getNOXDelta() ) {
				dbAdapter.createNox(reading, 0.0);
				lastKnownNOX = reading;
			}
			
			
			
			sleep(updateinterval);
			
			Log.i(TAG, "calling mDbHelper.createNox(nox, temperature) - should add row to the nox table in noxdroid.db");

		} catch (InterruptedException e) {
			notifyEventchanged(NoxDroidService.ERROR_IOIO_INTERRUPTED);
			Log.i(TAG, "LOOP: " + e.getMessage());
			ioio_.disconnect();
		} catch (ConnectionLostException e) {
			notifyEventchanged(NoxDroidService.ERROR_IOIO_CONNECTION_LOST);
			// Notify service;
			Log.e(TAG, "LOOP:" +  e.getMessage());
			throw e;
		}
	}

	/**
	 * Subclasses should override this method for performing operations to be
	 * done once as soon as IOIO communication is lost or closed. Typically,
	 * this will include GUI changes corresponding to the change. This method
	 * will only be called if setup() has been called. The {@link #ioio_} member
	 * must not be used from within this method.
	 */
	protected void disconnected() throws InterruptedException {
		/* 
		 * Close database - also done in other exceptions
		 * TODO: verify when it should be closed
		 */
//        mDbHelper.close();
		// Temporarily disabled it crashed application - probably not the right place to stop or
		
		
	}

	/**
	 * Subclasses should override this method for performing operations to be
	 * done if an incompatible IOIO firmware is detected. The {@link #ioio_}
	 * member must not be used from within this method. This method will only be
	 * called once, until a compatible IOIO is connected (i.e. {@link #setup()}
	 * gets called).
	 */
	protected void incompatible() {
	}

	/** Not relevant to subclasses. */
	public synchronized final void abort() {
		abort_ = true;
		if (ioio_ != null) {
			ioio_.disconnect();
		}
		if (connected_) {
			interrupt();
		}
		//notifyEventchanged(NoxDroidService.ERROR_IOIO_ABORTED);
	}
	
	public synchronized void stopRecording() {
		if (ioio_ != null) {
			ioio_.disconnect();
		}
		if (connected_) {
			interrupt();
		}
		notifyEventchanged(NoxDroidService.STATUS_IOIO_STOPPED_RECORDING);
	}

	public synchronized Float getReading() throws ConnectionLostException {
		if (input_ != null) {
			try {
				return input_.read();
			} catch (InterruptedException e) {
				Log.e(TAG, e.getMessage());
				abort();
			}
		}
		return null;
	}
	
	public synchronized void setUpdateSettings(int interval, int no2pin) {
		updateinterval = interval;
		pinAnalogIn = no2pin;
		Log.d(TAG, "updated IOIO settings. updateinterval - " + updateinterval + " no2pin - " + no2pin);
	}

	private void notifyEventchanged(int msg) {
		Iterator<IOIOEventListener> it = listeners.iterator();
		while (it.hasNext()) {
			it.next().notify(msg);
		}
	}
	
	public boolean isConnected() {
		return this.connected_;
	}
}