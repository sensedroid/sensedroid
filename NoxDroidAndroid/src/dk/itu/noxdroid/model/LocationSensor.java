package dk.itu.noxdroid.model;

import java.util.ArrayList;

public class LocationSensor extends Sensor {

	private ArrayList<Location> locations = new ArrayList<Location>();
	
	public LocationSensor() {
	}

	public LocationSensor(String title) {
		super(title);
	}

}
