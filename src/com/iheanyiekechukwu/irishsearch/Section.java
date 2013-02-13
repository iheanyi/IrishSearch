package com.iheanyiekechukwu.irishsearch;

import java.io.Serializable;

public class Section implements Serializable{
	
	private String max;
	private String crn;
	private String instructor;
	private String time;
	private String location;

	private String sectionnum;
	private String open;
	
	public String getOpen() {
		return open;
	}

	public void setOpen(String open) {
		this.open = open;
	}

	public String getMax() {
		return max;
	}

	public void setMax(String max) {
		this.max = max;
	}

	public String getCrn() {
		return crn;
	}

	public void setCrn(String crn) {
		this.crn = crn;
	}

	public String getInstructor() {
		return instructor;
	}

	public void setInstructor(String instructor) {
		this.instructor = instructor;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}


	public Section(String section, String op_spots, String max_spots, String regnum, String instruct, String when, String where) {
		// TODO Auto-generated constructor stub
		this.sectionnum = section;
		this.open = op_spots;
		this.max = max_spots;
		this.crn = regnum;
		this.instructor = instruct;
		this.time = when;
		this.location = where;
	}
	
	public String getSectionnum() {
		return sectionnum;
	}

	public void setSectionnum(String sectionnum) {
		this.sectionnum = sectionnum;
	}

}
