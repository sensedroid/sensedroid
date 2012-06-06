package dk.itu.noxdroid.ioio;

import ioio.lib.api.DigitalOutput.Spec;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import android.util.Log;

public class IOIOConnectedTestThread extends Thread {
	public static enum STATUS {CONNECTED, INCOMPATIBLE, CONNECTION_WAITING, NULL, ABORTED, CONNECTION_LOST}
	
	private STATUS status = STATUS.NULL;

	private IOIO ioio_;
	private int pinLed = 9;
	private int pinIn = 40;

	private boolean abort_ = false;
	private boolean connected_ = true;
	
	private String TAG = "NoxDroid_IOIOConnectedTestThread";

	/** Not relevant to subclasses. */
	@Override
	public final void run() {
		super.run();
		while (true) {
			try {
				synchronized (this) {
					if (abort_) {
						break;
					}
					ioio_ = IOIOFactory.create();
				}
				status = STATUS.CONNECTION_WAITING;
				ioio_.waitForConnect();
				connected_ = true;
				status = STATUS.CONNECTED;
				setup();
				abort();
			} catch (ConnectionLostException e) {
				if (abort_) {
					status = STATUS.CONNECTION_LOST;
					abort();
					break;
				}
			} catch (IncompatibilityException e) {
				Log.e("AbstractIOIOActivity", "Incompatible IOIO firmware", e);
				status = STATUS.INCOMPATIBLE;
				// nothing to do - just wait until physical disconnection
				try {
					ioio_.waitForDisconnect();
					break;
				} catch (InterruptedException e1) {
					ioio_.disconnect();
					break;
				}
			} catch (Exception e) {
				Log.e("AbstractIOIOActivity", "Unexpected exception caught", e);
				status = STATUS.ABORTED;
				ioio_.disconnect();
				break;
			} finally {
				try {
					if (ioio_ != null) {
						ioio_.waitForDisconnect();
						if (connected_) {
							abort();
						}
					}
				} catch (InterruptedException e) {
					
				}
			}
		}
	}

	public void setup() throws ConnectionLostException {
		try {
			ioio_.openAnalogInput(pinIn);
			ioio_.openDigitalOutput(pinLed, Spec.Mode.NORMAL, true);
		} catch (ConnectionLostException e) {
			connected_ = false;
		}
	}

	/** Not relevant to subclasses. */
	public synchronized final void abort() {
		Log.d(TAG, "Aborting ");
		abort_ = true;
		if (ioio_ != null) {
			ioio_.disconnect();
		}
		if (connected_) {
			interrupt();
		}
	}
	
	public STATUS getStatus() {
		return this.status;
	}
}