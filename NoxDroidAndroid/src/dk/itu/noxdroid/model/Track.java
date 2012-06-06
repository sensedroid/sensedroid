package dk.itu.noxdroid.model;

import java.util.ArrayList;

public class Track {

	private String id;
	private String startTime;
	private String endTime;
	private int syncFlag;
	
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	
	public Track () {
	}		

	public Track (String id, String startTime, String endTime, int syncFlag){
		this.id = id;
		this.startTime = startTime;
		this.endTime = endTime;
		this.syncFlag = syncFlag;
	}
	
	public Track(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}	

	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}		

	
	public int getSyncFlag() {
		return syncFlag;
	}
	public void setSyncFlag(int syncFlag) {
		this.syncFlag = syncFlag;
	}		
	
	
}
