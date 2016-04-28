package com.tomorrowdev.beacons;

public class MM {

	int minor, major;
	private int deathCount = 0;
	double distance;
	
	public MM(int major, int minor) {
		this.minor = minor;
		this.major = major;
	}
	
	public MM(int major, int minor, double distance) {
		this.minor = minor;
		this.major = major;
		this.distance = distance;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}
	
	public int getDeathCount() {
		return deathCount;
	}

	public void setDeathCount(int deathCount) {
		this.deathCount = deathCount;
	}
	
	public void increaseDeathCountByOne() {
		deathCount++;
	}
	
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	/**
	 * Overrided method used by the list to check if it's the same object.
	 */
	@Override
	public boolean equals(Object object){
		
		boolean sameSame = false;
		
		if(object != null && object instanceof MM){
			sameSame = this.major == ((MM) object).getMajor() && this.minor == ((MM) object).getMinor();
		}
		
		return sameSame;		
	}
}
