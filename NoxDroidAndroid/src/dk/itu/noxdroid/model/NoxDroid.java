package dk.itu.noxdroid.model;

import java.util.ArrayList;

public class NoxDroid {

	private String id;
	private String name;

	
	private ArrayList<Track> tracks = new ArrayList<Track>();
	
	public NoxDroid () {
	}		

	public NoxDroid (String id, String name){
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
