package lmu.ifi.sensedroid;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

/**
 * An Service for connecting to an IOIO Microcontroller Board. When this service
 * is started by an Intent with startservice(Intent) the Intent needs to include
 * a configuration file, with the desired looptime (Measuring Interval) and the
 * list of connected sensors. Please note that sensors that use TWI have to be
 * physically connected if they are included in the configuration, because the
 * IOIO library methods for TWI are blocking. For more infos about the Intents,
 * see class SenseDroidIntents.
 * 
 * When this service is started with an appropriate Intent the used IOIO library
 * will try to make an connection to an IOIO. If this connection is successful,
 * first the setup() method of the BaseIOIOLooper in this service will be run,
 * and then the loop() method will run in a continuous loop.
 * 
 * In the setup() method LEDs are set to glow in the SenseDroid to show the
 * Status of the connection on the physical device and also the status is sent
 * to other components. In the loop() method the sampling takes place.If the
 * IOIO disconnects the method disconnected is called and then the IOIO library
 * tries to reconnect.
 * 
 * To add methods for handling new sensor types the new sensortypes have to be
 * added in the method float[] handleSensor(String sensortype, int pin[], IOIO
 * ioio_)
 * 
 * This Service shows an Notification and also sends out the Status in a Intent
 * at important points of the lifetime. Further it has a BroadcastReceiver to
 * get Intents for configuration, extra data, or getstatus requests and handles
 * this requests appropriately.
 * 
 */
public class SenseDroidService extends IOIOService {
	// Logging TAG
	private static final String TAG = "SenseDroidService";

	// necessary for Notification Manager
	private static final int NOTIFY_ID = 1;
	private NotificationManager notificationManager;
	private Notification notification;
	private PendingIntent contentIntent;

	// status of the service, uses Constants out of SenseDroidConstants
	private String status;

	// This Configuration is a ArrayList<HashMap<String, Object>>, where every
	// HashMap<String,Object> stands for one sensor. Each Hashmap includes
	// the sensortype as String and an int array as object, which for
	// sensor types that are analog Inputs holds pin number and for sensors that
	// use TWI hold the module number
	private ArrayList<HashMap<String, Object>> configuration = new ArrayList<HashMap<String, Object>>();

	// looptime in seconds for setting the sleep time of the loop function in
	// IOIOLooper, it sets therefore the measuring interval
	private int looptime = 1;

	// loopint is used to let the service not sleep for the full time between
	// measuremnets but to let sleep it in 1 second intervals
	// this construction means that the service is much more responsive
	private int loopint = 0;

	// for handling extra readings outside of the measurement interval
	// that don´t reset the measurement interval
	private boolean extrareading = false;

	/**
	 * A setter for the measuring interval looptime, also gets an immediate
	 * measurement after using this setter
	 * 
	 * @param loopt
	 *            measuring interval in seconds
	 */
	private void setlooptime(int loopt) {
		looptime = loopt;
		loopint = looptime;
	}

	// Broadcast Receiver for incoming Intents
	BroadcastReceiver sensserv_recv = new BroadcastReceiver() {

		// handles incoming Intents
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// If an configuration Intent is received the service tries to
			// update its configuration
			if (action.equals(SenseDroidIntents.SENSEDROID_CONFIGURESERVICE)) {
				configureService(intent);
			}
			// If an GET_STATUS Intent is received the service sends out an
			// intent with it´s current status
			if (action.equals(SenseDroidIntents.SENSEDROID_GETSTATUS)) {
				sendStatus();
			}
			// An extra Reading is requested
			if (action.equals(SenseDroidIntents.SENSEDROID_EXTRAREADING)) {
				extrareading = true;
			}

		}
	};

	/**
	 * Called by the system at the Creation of the service. Initializes
	 * necessary parts of the service, like the BroadCastreceiver, the
	 * Notification and the status
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// Initialize BroadcastReceiver
		IntentFilter filter = new IntentFilter(
				SenseDroidIntents.SENSEDROID_CONFIGURESERVICE);
		filter.addAction(SenseDroidIntents.SENSEDROID_GETSTATUS);
		filter.addAction(SenseDroidIntents.SENSEDROID_EXTRAREADING);
		registerReceiver(sensserv_recv, filter);

		// Initialize Notification Manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		startNotification(SenseDroidConstants.STATUS_WAITCONNECTION);
		// Initialize status
		status = SenseDroidConstants.STATUS_WAITCONNECTION;
		sendStatus();

		Log.d(TAG, "SenseDroidService created");
	}

	/**
	 * Called by the system when the service is started by startService(Intent)
	 * Calls a method to configure the service
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStart(intent, startId);

		// gets the configuration that is send with the start intent
		configureService(intent);
		return START_NOT_STICKY;
	}

	/**
	 * Called by the system the service is no longer used. Cleans up the
	 * BroadcastReceiver and the Notification. And sends an status Intent out
	 * that the service is exiting
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		// unregisters the BroadcastReceiver
		unregisterReceiver(sensserv_recv);
		// closes the Notifications
		closeNotification();
		// send Exit status
		status = SenseDroidConstants.STATUS_SERVICEEXIT;
		sendStatus();

		Log.d("tag", "SenseDroidService stoped");
	}

	/**
	 * sends the status of the service with an Intent
	 */
	private void sendStatus() {
		Intent intent = new Intent(SenseDroidIntents.SENSEDROID_STATUS);
		intent.putExtra(SenseDroidIntents.EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	/**
	 * Initializes the Notification Manager
	 * 
	 * @param text
	 *            Initial Text for the Notification
	 */
	private void startNotification(String text) {
		// new Notification
		notification = new Notification(R.drawable.ic_launcher,
				"SenseDroid Service", System.currentTimeMillis());
		// Setup a Intent to open the SenseDroid Activity when clicked
		Intent notificationIntent = new Intent(this, SenseDroidActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				0);

		// update the notification with the text
		updateNotification(text);
	}

	/**
	 * Change the displayed text of a Notification
	 * 
	 * @param text
	 *            new text for the Notification
	 */
	private void updateNotification(String text) {
		notification.setLatestEventInfo(this, "SenseDroid Service", text,
				contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notificationManager.notify(NOTIFY_ID, notification);
	}

	/**
	 * Ends the Notification Manager
	 */
	private void closeNotification() {
		notificationManager.cancel(NOTIFY_ID);
	}

	/**
	 * in this Method the Configuration for the service is retrieved from an
	 * Intent and the configuration of the service is then updated
	 * 
	 * @param intent
	 *            Intent to check for the configuration
	 */
	private synchronized void configureService(Intent intent) {

		// to retrieve the looptime
		int conf_looptime = intent.getIntExtra(
				SenseDroidIntents.EXTRA_LOOPTIME, -1);
		// check if send Extra is valid
		if (conf_looptime > 0) {
			// update the looptime
			setlooptime(conf_looptime);
		}

		@SuppressWarnings("unchecked")
		ArrayList<HashMap<String, Object>> result = (ArrayList<HashMap<String, Object>>) intent
				.getSerializableExtra(SenseDroidIntents.EXTRA_SENSORCONFIGURATION);
		// check if extra was Serializable
		if (result != null) {
			configuration = result;
		}

		Log.d(TAG, "SenseDroidService Configuration");
	}

	/**
	 * Necessary for extending IOIO Service
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Method to create an IOIOLooper In this IOIOLooper the actions of the IOIO
	 * are configured
	 */
	protected IOIOLooper createIOIOLooper() {
		return new BaseIOIOLooper() {

			@SuppressWarnings("unused")
			private DigitalOutput led_;
			private DigitalOutput led2_;

			/**
			 * In the setup() method LEDs are set to glow in the SenseDroid to
			 * show the Status of the connection on the physical device and also
			 * the status and the Notifcation is updated
			 */
			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {

				// here we know that we have an conncection to a IOIO, so the
				// status and the Notification are updated
				status = SenseDroidConstants.STATUS_CONNECTED;
				sendStatus();
				updateNotification(SenseDroidConstants.STATUS_CONNECTED);

				// led 0 functions the other way to other leds in the used IOIO
				// false = high is used to let the stat led glow on the IOIO to
				// show that we have an connection
				led_ = ioio_.openDigitalOutput(0, false);
				// led2 for an external stat led
				led2_ = ioio_.openDigitalOutput(40, false);
				led2_.write(true);
			}

			/**
			 * In the loop() method the sampling takes place.
			 */
			@Override
			public void loop() throws ConnectionLostException,
					InterruptedException {

				// this construction is used to let the Thread be responsive for
				// configuration changes, also it allows for extra readings
				// outside of the measurement interval
				if (loopint < looptime && extrareading == false) {
					Thread.sleep(1000);
					loopint++;
				} else {
					if (loopint >= looptime) {
						loopint = 0;
					}
					extrareading = false;

					// creates an object for the result
					// The result is an ArrayList<HashMap<String, Object>>,
					// where every HashMap<String,Object> stands for one sensor.
					// Each HashMap includes the sensortype as String and an
					// float array as object, which holds the queried values of
					// this sensor
					ArrayList<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();

					// for every Sensor in the configuration File the necessary
					// actions for querying the sensors are taken here
					for (HashMap<String, Object> conf : configuration) {

						// to get the sensor configuration
						Object sensortypeo = conf
								.get(SenseDroidConstants.MAPPING_SENSORTYPE);
						Object pino = conf
								.get(SenseDroidConstants.MAPPING_PINNUM);
						if (pino != null && sensortypeo != null) {
							int pin[] = (int[]) pino;
							String sensortype = (String) sensortypeo;

							// here the value is gathered
							float value[] = handleSensor(sensortype, pin, ioio_);
							Log.d(TAG, "Sensor " + sensortype + " value1: "
									+ value[0]);

							// and the Sensortype and Value are added to the
							// result
							HashMap<String, Object> res = new HashMap<String, Object>();
							res.put(SenseDroidConstants.MAPPING_SENSORTYPE,
									sensortype);
							res.put(SenseDroidConstants.MAPPING_DATA, value);
							result.add(res);
						}
					}

					// sends an Broadcast with the result
					forwardResult(result);
				}
			}

			/**
			 * If the IOIO disconnects the method disconnected is called and
			 * then the IOIO library tries to reconnect. The status and
			 * Notification in this Method.
			 */
			@Override
			public void disconnected() {
				updateNotification(SenseDroidConstants.STATUS_CONNECTIONFAILED);
				status = SenseDroidConstants.STATUS_CONNECTIONFAILED;
				sendStatus();
				Log.d(TAG, "SenseDroidService disconnected");
				super.disconnected();
			}

		};
	}

	/**
	 * This method sends an Intent with the result of a measurement intervall
	 * 
	 * @param result
	 *            the Result of a measurement intervall
	 */
	private void forwardResult(ArrayList<HashMap<String, Object>> result) {
		Intent intent = new Intent(SenseDroidIntents.SENSEDROID_DATA);
		intent.putExtra(SenseDroidIntents.EXTRA_DATA, result);
		sendBroadcast(intent);
	}

	/**
	 * Calls for a sensortype the appropriate method to get the sensor data
	 * 
	 * 
	 * @param sensortype
	 *            the type of the sensor to query
	 * @param pin
	 *            the pins the sensor is connected, or for sensor that uses TWI
	 *            the used TWI module
	 * @param ioio_
	 *            an instance of the IOIO Interface
	 * @return the queried values of the sensor or -1000f as first array member
	 *         in case of an error
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	private float[] handleSensor(String sensortype, int pin[], IOIO ioio_)
			throws ConnectionLostException, InterruptedException {
		// handles the sensortype mq5 (set in the class SenseDroidSensorTypes)
		if (sensortype.equals(SenseDroidSensorTypes.mq5)) {
			int usedpin = pin[0];
			float value = readSensor_Analog(ioio_, usedpin);
			float values[] = new float[1];
			values[0] = value;
			return values;
		}

		// handles the sensortype mq131 (set in the class SenseDroidSensorTypes)
		if (sensortype.equals(SenseDroidSensorTypes.mq131)) {
			int usedpin = pin[0];
			float value = readSensor_Analog(ioio_, usedpin);
			float values[] = new float[1];
			values[0] = value;
			return values;
		}

		// handles the sensortype mq135 (set in the class SenseDroidSensorTypes)
		if (sensortype.equals(SenseDroidSensorTypes.mq135)) {
			int usedpin = pin[0];
			float value = readSensor_Analog(ioio_, usedpin);
			float values[] = new float[1];
			values[0] = value;
			return values;
		}

		// handles the sensortype tmp102 (set in the class
		// SenseDroidSensorTypes)
		if (sensortype.equals(SenseDroidSensorTypes.tmp102)) {
			int usedmodule = pin[0];
			float value = readTMP102(ioio_, usedmodule);
			float values[] = new float[1];
			values[0] = value;
			return values;
		}

		float values[] = new float[1];
		values[0] = -1000f;
		return values;
	}

	/**
	 * simple method for reading Analog sensors
	 * 
	 * @param ioio_
	 *            an instance of the IOIO Interface
	 * @param pin
	 *            the pin the sensor is connected
	 * @return
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	private float readSensor_Analog(IOIO ioio_, int pin)
			throws ConnectionLostException, InterruptedException {
		AnalogInput in = ioio_.openAnalogInput(pin);
		Thread.sleep(100);
		float value = in.getVoltage();
		in.close();
		return value;
	}

	/**
	 * method for querying the TMP102 sensor, this sensor needs an
	 * individualized TWI Conenction
	 * 
	 * @param ioio_
	 *            an instance of the IOIO Interface
	 * @param module
	 *            the used module of the IOIO
	 * @return
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */

	private float readTMP102(IOIO ioio_, int module)
			throws ConnectionLostException, InterruptedException {

		// opens the TWI module
		TwiMaster twi = ioio_.openTwiMaster(module, TwiMaster.Rate.RATE_100KHz,
				false);

		// the necessary request according to the datasheet
		byte[] request = new byte[] { 0x00 };
		// the response is two bytes
		byte[] response = new byte[2];

		// address on the TWI is 0x48 if add0 = GND of the sensor
		// its read two times because on the first read it can return 0
		// according to the datasheet
		if (twi.writeRead(0x48, false, request, request.length, response,
				response.length) == false) {
			twi.close();
			return -1000f;
		}
		if (twi.writeRead(0x48, false, request, request.length, response,
				response.length) == false) {
			twi.close();
			return -1000f;
		}
		// closes the tqi module
		twi.close();

		byte byte1 = response[0];
		byte byte2 = response[1];

		// to calculate the temperature value out of the two bytes
		int i1 = (byte1 & 0xFF) << 8;
		int i2 = (byte2 & 0xFF);
		int i3 = i1 | i2;
		short s1 = (short) i3;
		float f = (float) ((s1) / 256F);

		return f;
	}
}
