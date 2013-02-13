package com.iheanyiekechukwu.irishsearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Course implements Serializable{

		
	private String courseName;
	private String credit;
	private String time;
	private String openSpots;
	private String maxspots;
	private String crn;
	private String coursenumber;
	private String description;
	private ArrayList<Section> sections;
	
	public Course(String courseNum, String course, String credits) {
		// TODO Auto-generated constructor stub
		this.courseName = course;
		this.coursenumber = courseNum;
		this.credit = credits;
		
		sections = new ArrayList<Section>();
	}
	
	public void add(Section s) {	
		sections.add(s);
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	
	public String getCourseNumber() {
		return coursenumber;
	}

	public ArrayList<Section> getSections() {
		return sections;
	}

	public void setSections(ArrayList<Section> sections) {
		this.sections = sections;
	}

}
