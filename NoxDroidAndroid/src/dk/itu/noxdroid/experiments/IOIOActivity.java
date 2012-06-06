package dk.itu.noxdroid.experiments;


import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.DigitalOutput.Spec;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ToggleButton;
import dk.itu.noxdroid.R;

public class IOIOActivity extends AbstractIOIOActivity {
	private TextView textView_;
	private TextView debug_;
	private ToggleButton toggleButton_;
	private String TAG;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		
		setContentView(R.layout.ioio);

		textView_ = (TextView) findViewById(R.id.textView1);
		toggleButton_ = (ToggleButton) findViewById(R.id.toggleButton1);
		debug_ = (TextView) findViewById(R.id.textView2);
		addToDebug("OnCreate");
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private AnalogInput input_;

		private DigitalOutput led_;
		private int pinLed = 9;
		private int pinIn = 11;

		@Override
		public void setup() throws ConnectionLostException {
			addToDebug("Setup()");
			try {
				// input_ = ioio_.openAnalogInput(pinIn);
				led_ = ioio_.openDigitalOutput(pinLed, Spec.Mode.NORMAL, true);

			} catch (ConnectionLostException e) {

				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			addToDebug("Loop");
			try {
//				final float reading = input_.read();
//				setText(Float.toString(reading));
				led_.write(!toggleButton_.isChecked());
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
				debug_.append(str + "\n");
			};
		});
		
	}

	private void setText(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				debug_.append(str + "\n");
				textView_.setText(str);
			}
		});
	}
}
