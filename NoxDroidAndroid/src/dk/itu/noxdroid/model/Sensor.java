package dk.itu.noxdroid.model;


public abstract class Sensor {
	
	private String title;
	
	public Sensor () {
	}		

	public Sensor (String title){
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

}
