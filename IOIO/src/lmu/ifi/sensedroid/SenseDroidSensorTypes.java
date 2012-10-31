package lmu.ifi.sensedroid;


/**
 * The implemented Sensor Types, that can be queried in the {@link SenseDroidService}
 */
public class SenseDroidSensorTypes {
	
	//sensors of this type return one value and this value is the raw voltage data sampled
	public static final String mq5 = "MQ-5 RAW";
	
	//sensors of this type return one value and this value is the raw voltage data sampled
	public static final String mq131 = "MQ-131 RAW";

	//sensors of this type return one value and this value is the raw voltage data sampled
	public static final String mq135 = "MQ-135 RAW";

	//sensors of this type return one value and this value is the measured temperature
	public static final String tmp102 = "TMP102";
}
