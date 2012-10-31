package lmu.ifi.sensedroid;

/**
 * In this class are the Intents described, that the {@link SenseDroidService}
 * can receive or send *
 */

public class SenseDroidIntents {

	// The following Strings are the actions of Intents that are send by the
	// {@link SenseDroidService}

	
	
	// The SENSEDROID_DATA Intent is send when data is queried by the Service
	// this intent has an Extra with a name of the String
	// EXTRA_DATA which holds a Serializable content
	//
	// This Serializable content is an ArrayList<HashMap<String, Object>>,
	// where every HashMap<String,Object> in the List stands for one sensor.
	// Each HashMap includes the sensortype as String and an
	// float array as object, which holds the queried values of
	// the sensor. The used HashMap keys are in the class SenseDroidConstants.
	//
	public static final String SENSEDROID_DATA = "lmu.ifi.sensedroid.DATA";

	// the SENSEDROID_STATUS Intent is send when the {@link SenseDroidService}
	// wants to send its status to other components. It has an extra with
	// EXTRA_STATUS as name and as a String the status of the {@link
	// SenseDroidService}
	public static final String SENSEDROID_STATUS = "lmu.ifi.sensedroid.STATUS";

	
	
	// The following Strings are the actions of Intents that can be received and
	// handled by the {@link SenseDroidService}

	
	
	
	// The SENSEDROID_CONFIGURESERVICE Intent is send by a another application
	// or application component when the {@link SenseDroidService} should be
	// configured. This intent has normally two Extras:
	// 1. An Extra with a name of the String EXTRA_LOOPTIME which holds a int to
	// set the Measurement intervall.
	//
	// 2. An Extra with a name of the String EXTRA_SENSORCONFIGURATION which
	// holds a Serializable content.
	//
	// This Serializable content is an ArrayList<HashMap<String, Object>>,
	// where every HashMap<String,Object> in the List stands for one sensor.
	// Each HashMap includes the sensortype as String and an
	// int array as object, which holds the pin the sensor is connected, or for
	// sensor that uses TWI the used TWI module of the sensor. The used HashMap
	// keys are in the class SenseDroidConstants.
	public static final String SENSEDROID_CONFIGURESERVICE = "lmu.ifi.sensedroid.CONFIGURE_SERVICE";

	// The SENSEDROID_GETSTATUS Intent is send by a another application
	// or application component to get the {@link SenseDroidService} to send an
	// SENSEDROID_STATUS Intent
	public static final String SENSEDROID_GETSTATUS = "lmu.ifi.sensedroid.GETSTATUS";

	// The SENSEDROID_EXTRAREADING Intent is send by a another application
	// or application component to get the {@link SenseDroidService} to query an
	// extra reading outside of the measurement interval
	public static final String SENSEDROID_EXTRAREADING = "lmu.ifi.sensedroid.EXTRAREADING";

	
	
	
	// Strings for the name of the extras for the the Intents like described
	// above
	public static final String EXTRA_DATA = "lmu.ifi.sensedroid.EXTRA_DATA";

	public static final String EXTRA_STATUS = "lmu.ifi.sensedroid.EXTRA_STATUS";

	public static final String EXTRA_LOOPTIME = "lmu.ifi.sensedroid.EXTRA_LOOPTIME";

	public static final String EXTRA_SENSORCONFIGURATION = "lmu.ifi.sensedroid.EXTRA_SENSORCONFIGURATION";

}
