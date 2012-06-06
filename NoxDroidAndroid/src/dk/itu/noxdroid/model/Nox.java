package dk.itu.noxdroid.model;

public class Nox {

	double nox;
	String timeStamp;
	
	
	public Nox() {
	}

	public Nox(String timeStamp, double nox) {
		this.timeStamp = timeStamp;
		this.nox = nox;
	}

	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public double getNox() {
		return nox;
	}
	public void setNox(double nox) {
		this.nox = nox;
	}
	
	
}
