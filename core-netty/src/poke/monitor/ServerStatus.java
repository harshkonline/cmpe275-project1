package poke.monitor;

import java.util.HashMap;

public class ServerStatus {

	public HashMap<String,Boolean> availability = new HashMap<String,Boolean>();

	public HashMap<String, Boolean> getAvailability() {
		return availability;
	}

	public void setAvailability(HashMap<String, Boolean> availability) {
		this.availability = availability;
	}
	
}
