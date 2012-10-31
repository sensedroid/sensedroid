package lmu.ifi.sensedroid;

/**
 * Constants for the SenseDroid Application that are used by the Service and the Activity
 */
public class SenseDroidConstants {
	
	//Used for the Keys in HashMaps
	public static final String	MAPPING_SENSORTYPE			= "sensortype";
	public static final String	MAPPING_DATA				= "data";
	public static final String	MAPPING_PINNUM				= "pinnum";
	
	//used for the Status of the service
	public static final String STATUS_WAITCONNECTION = "Connecting";
	public static final String STATUS_CONNECTED = "Connected";
	public static final String STATUS_CONNECTIONFAILED = "Connection Failed, Retrying";
	public static final String STATUS_SERVICEEXIT = "Service Exited";
}
