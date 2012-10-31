package lmu.ifi.sensedroid;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.SeekBar;

/**
 * An activity for testing the {@link SenseDroidService}
 * 
 * It shows an simple interface that lets you configure the
 * {@link SenseDroidService}. This includes Buttons for starting, stopping,
 * configuring the service and also an Button to request extra data.
 * 
 */
public class SenseDroidActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener {
	// Logging TAG
	private static final String TAG = "SenseDroidActivity";

	// Used UI Elements
	Spinner mqslot1_spinner;
	Spinner mqslot2_spinner;
	Button startservice_Button;
	Button stopservice_Button;
	Button configure_Button;
	Button extra_Button;
	CheckBox tmp102_CheckBox;
	TextView debug_TextView;
	TextView debugslot1_TextView;
	TextView debugslot2_TextView;
	TextView debugslot3_TextView;
	TextView looptime_TextView;
	SeekBar looptime_SeekBar;

	// used for the Preferences of the Activity to save settings
	public static final String PREFERENCES_LOOPTIME = "looptime";
	public static final String PREFERENCES_TMP102 = "tmp102checkbox";
	public static final String PREFERENCES_MQSLOT1 = "mqslot1_spinner";
	public static final String PREFERENCES_MQSLOT2 = "mqslot2_spinner";
	public static final String PREFS_NAME = "SenseDroid Storage";

	// Measurement intervall for the {@link SenseDroidService}
	private int looptime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// to get saved Interface Status
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		looptime = settings.getInt(PREFERENCES_LOOPTIME, 15);
		Boolean tmp102check = settings.getBoolean(PREFERENCES_TMP102, false);
		int mqslot1_position = settings.getInt(PREFERENCES_MQSLOT1, 0);
		int mqslot2_position = settings.getInt(PREFERENCES_MQSLOT2, 0);

		// initialize UI Elements
		// Spinner setup
		mqslot1_spinner = (Spinner) findViewById(R.id.configuremqslot1_spinner);
		mqslot2_spinner = (Spinner) findViewById(R.id.configuremqslot2_spinner);
		ArrayList<String> spinner_sensorsArray = new ArrayList<String>();
		spinner_sensorsArray.add("None");
		spinner_sensorsArray.add(SenseDroidSensorTypes.mq5);
		spinner_sensorsArray.add(SenseDroidSensorTypes.mq131);
		spinner_sensorsArray.add(SenseDroidSensorTypes.mq135);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinner_sensorsArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mqslot1_spinner.setAdapter(adapter);
		mqslot2_spinner.setAdapter(adapter);

		// set the spinners according to the saved Interface status
		mqslot1_spinner.setSelection(mqslot1_position);
		mqslot2_spinner.setSelection(mqslot2_position);

		// Seekbar setup
		looptime_SeekBar = (SeekBar) findViewById(R.id.looptime_seekBar);
		// set default value
		looptime_SeekBar.setProgress(looptime / 10);
		looptime_SeekBar.setOnSeekBarChangeListener(this);

		// TMP102CheckBox setup
		tmp102_CheckBox = (CheckBox) findViewById(R.id.temp2_checkbox);
		// set checkbox according to the saved Interface status
		tmp102_CheckBox.setChecked(tmp102check);

		// other UI Elements
		startservice_Button = (Button) findViewById(R.id.startservice_button);
		stopservice_Button = (Button) findViewById(R.id.stopservice_button);
		configure_Button = (Button) findViewById(R.id.configure_button);
		extra_Button = (Button) findViewById(R.id.extra_button);
		looptime_TextView = (TextView) findViewById(R.id.looptime_loopValue);
		looptime_TextView.setText("" + looptime);
		debug_TextView = (TextView) findViewById(R.id.debug);
		debugslot1_TextView = (TextView) findViewById(R.id.debugslot1);
		debugslot2_TextView = (TextView) findViewById(R.id.debugslot2);
		debugslot3_TextView = (TextView) findViewById(R.id.debugslot3);

		// tries to get the status if the SenseDroidService is already service
		Intent intent = new Intent(SenseDroidIntents.SENSEDROID_GETSTATUS);
		sendBroadcast(intent);

		Log.d(TAG, "SenseDroidActivity created");
	}

	@Override
	public void onPause() {
		super.onPause();
		// to unregister the BroadcastReceiver
		unregisterReceiver(sens_recv);

		Log.d(TAG, "SenseDroidActivity paused");
	}

	@Override
	public void onResume() {
		super.onResume();
		// to register the BroadcastReceiver for Intents from the {@link
		// SenseDroidService}
		IntentFilter filter = new IntentFilter(
				SenseDroidIntents.SENSEDROID_DATA);
		filter.addAction(SenseDroidIntents.SENSEDROID_STATUS);
		registerReceiver(sens_recv, filter);

		Log.d(TAG, "SenseDroidActivity resumed");
	}

	@Override
	public void onStop() {
		super.onStop();

		// save Interface Status
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(PREFERENCES_LOOPTIME, looptime);
		editor.putBoolean(PREFERENCES_TMP102, tmp102_CheckBox.isChecked());
		editor.putInt(PREFERENCES_MQSLOT1,
				mqslot1_spinner.getSelectedItemPosition());
		editor.putInt(PREFERENCES_MQSLOT2,
				mqslot2_spinner.getSelectedItemPosition());
		editor.commit();

		Log.d(TAG, "SenseDroidActivity stopped ");
	}

	/**
	 * Handler for the Start Button, tries to start the
	 * {@link SenseDroidService}
	 * 
	 * @param view
	 */
	public void startservice(View view) {
		// start the {@link SenseDroidService} with the Configuration added
		Intent StartIntent = new Intent(this, SenseDroidService.class);
		addConfiguration(StartIntent);
		startService(StartIntent);

		Log.d(TAG, "SenseDroidService start requested");
	}

	/**
	 * Handler for the Stop Button, tries to stop the {@link SenseDroidService}
	 * 
	 * @param view
	 */
	public void stopservice(View view) {
		// stops the {@link SenseDroidService}
		stopService(new Intent(this, SenseDroidService.class));

		Log.d(TAG, "SenseDroidService stop requested");
	}

	/**
	 * Handler for the Configuration Button, tries to configure an running
	 * {@link SenseDroidService}
	 * 
	 * @param view
	 */
	public void configureService(View view) {

		// sends a configuration Intent
		Intent intent = (new Intent(
				SenseDroidIntents.SENSEDROID_CONFIGURESERVICE));
		addConfiguration(intent);
		sendBroadcast(intent);
		Log.d(TAG, "SenseDroidService configuration requested");
	}

	/**
	 * Handler for the Extra Reading Button, tries to get an extra Reading from
	 * an running {@link SenseDroidService}
	 * 
	 * @param view
	 */
	public void requestextraReading(View view) {
		// sends an Extra_Reading Intent to request an extra Reading
		Intent intent = (new Intent(SenseDroidIntents.SENSEDROID_EXTRAREADING));
		sendBroadcast(intent);
		Log.d(TAG, "SenseDroidService extra reading requested");
	}

	/**
	 * extract Sensor Configuration out of the Interface and create a
	 * Configuration structure.
	 * 
	 * This Configuration is a ArrayList<HashMap<String, Object>>, where every
	 * HashMap<String,Object> stands for one sensor. Each Hashmap includes the
	 * sensortype as String and an int array as object, which for sensor types
	 * that are analog Inputs holds pin number and for sensors that use TWI hold
	 * the module number
	 * 
	 * @return returns the Configuration for the {@link SenseDroidService}
	 */
	private ArrayList<HashMap<String, Object>> getConfiguration() {
		ArrayList<HashMap<String, Object>> configuration = new ArrayList<HashMap<String, Object>>();

		// First Spinner
		if (mqslot1_spinner.getSelectedItemPosition() > 0) {
			HashMap<String, Object> sensors = new HashMap<String, Object>();
			sensors.put(SenseDroidConstants.MAPPING_SENSORTYPE,
					mqslot1_spinner.getSelectedItem());
			int pin[] = new int[1];
			pin[0] = 32;
			sensors.put(SenseDroidConstants.MAPPING_PINNUM, pin);
			configuration.add(sensors);
		}

		// Second Spinner
		if (mqslot2_spinner.getSelectedItemPosition() > 0) {
			HashMap<String, Object> sensors = new HashMap<String, Object>();
			sensors.put(SenseDroidConstants.MAPPING_SENSORTYPE,
					mqslot2_spinner.getSelectedItem());
			int pin[] = new int[1];
			pin[0] = 33;
			sensors.put(SenseDroidConstants.MAPPING_PINNUM, pin);
			configuration.add(sensors);
		}

		// TMP102 Checkbox
		if (tmp102_CheckBox.isChecked()) {
			HashMap<String, Object> sensors = new HashMap<String, Object>();
			sensors.put(SenseDroidConstants.MAPPING_SENSORTYPE,
					SenseDroidSensorTypes.tmp102);
			int module[] = new int[1];
			module[0] = 1;
			sensors.put(SenseDroidConstants.MAPPING_PINNUM, module);
			configuration.add(sensors);
		}

		return configuration;
	}

	/**
	 * Adds a Configuration for the {@link SenseDroidService} to an Intent
	 * 
	 * @param intent
	 */
	private void addConfiguration(Intent intent) {
		// add looptime to configuration intent
		intent.putExtra(SenseDroidIntents.EXTRA_LOOPTIME, looptime);

		ArrayList<HashMap<String, Object>> configuration = getConfiguration();
		// add used sensors to configuration intent
		intent.putExtra(SenseDroidIntents.EXTRA_SENSORCONFIGURATION,
				configuration);
	}

	// Broadcast Receiver for incoming Intents from the {@link
	// SenseDroidService}
	BroadcastReceiver sens_recv = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// Incoming Data from the {@link SenseDroidService}
			if (action.equals(SenseDroidIntents.SENSEDROID_DATA)) {
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String, Object>> result = (ArrayList<HashMap<String, Object>>) intent
						.getSerializableExtra(SenseDroidIntents.EXTRA_DATA);
				// check if extra was Serializable
				if (result != null) {
					handleIncome(result);
				}
			}

			// Incoming Status from the Ser{@link SenseDroidService}vice
			if (action.equals(SenseDroidIntents.SENSEDROID_STATUS)) {
				String status = intent
						.getStringExtra(SenseDroidIntents.EXTRA_STATUS);
				// check if status string found
				if (status != null) {
					debug_TextView.setText(" Service Status: " + status);
				}
			}
		}
	};

	/**
	 * A function to extract the the sensortypes and sensorreadings out of the
	 * the incoming data from the {@link SenseDroidService}. Also displays the
	 * first value of the first three sensors in the Data
	 * 
	 * @param result
	 *            Incoming Data
	 */
	private void handleIncome(ArrayList<HashMap<String, Object>> result) {
		int i = 1;
		// resets the text for the displayed data
		resetReading();
		for (HashMap<String, Object> map : result) {
			Object valueo = map.get(SenseDroidConstants.MAPPING_DATA);
			Object sensortypeo = map
					.get(SenseDroidConstants.MAPPING_SENSORTYPE);
			if (valueo != null && sensortypeo != null) {
				float[] value = (float[]) valueo;
				String sensortype = (String) sensortypeo;

				// To display the Information of the first three incoming
				// Sensors
				Log.d(TAG, "Sensor " + sensortype + " value1: " + value[0]);
				handleReading(sensortype, value, i);
				i++;
			}
		}
	}

	/**
	 * A Method to reset the text for the displayed data
	 */
	private void resetReading() {
		debugslot1_TextView.setText("");
		debugslot2_TextView.setText("");
		debugslot3_TextView.setText("");
	}

	/**
	 * A Method to display the text for the first three sensors in Incoming data
	 */
	public void handleReading(String sensortype, float value[], int i) {
		switch (i) {
		case 1:
			debugslot1_TextView.setText("Type: " + sensortype + " Value: "
					+ value[0]);
			break;
		case 2:
			debugslot2_TextView.setText("Type: " + sensortype + " Value: "
					+ value[0]);
			break;
		case 3:
			debugslot3_TextView.setText("Type: " + sensortype + " Value: "
					+ value[0]);
			break;
		default: {
		}
		}
	}

	/**
	 * Handler for the Looptime Seekbar, configures the measurement interval
	 */
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		int loop = arg1 * 10;
		if (loop == 0) {
			loop = 1;
		}
		looptime_TextView.setText("" + loop);
		looptime = loop;
	}

	/**
	 * unused, necessary for the Looptime Seekbar
	 */
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	/**
	 * unused, necessary for the Looptime Seekbar
	 */
	public void onStopTrackingTouch(SeekBar arg0) {
	}
}