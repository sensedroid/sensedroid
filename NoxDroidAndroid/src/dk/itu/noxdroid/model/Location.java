package dk.itu.noxdroid.model;

public class Location {

	String timeStamp;
	double latitude;
	double longitude;
	String provider;
	
	public Location() {
	}

	public Location(String timeStamp, double latitude, double longitude, String provider) {
		this.timeStamp = timeStamp;
		this.latitude = latitude;
		this.longitude = longitude;
		this.provider = provider;
	}

	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}	
	
}
