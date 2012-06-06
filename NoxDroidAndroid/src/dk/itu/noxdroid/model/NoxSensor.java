package dk.itu.noxdroid.model;

import java.util.ArrayList;

public class NoxSensor extends Sensor {

	private ArrayList<Nox> nox = new ArrayList<Nox>();
	
	public NoxSensor() {
	}

	public NoxSensor(String title) {
		super(title);
	}

}
