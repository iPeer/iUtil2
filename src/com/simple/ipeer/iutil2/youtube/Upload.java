package com.simple.ipeer.iutil2.youtube;

public class Upload {
	
	private String title, ID;
	private int length;
	
	public Upload(String title, String length, String id) {
		this(title, Integer.valueOf(length), id);
	}

	public Upload(String title, int length, String ID) {
		
		this.title = title;
		this.ID = ID;
		this.length = length;
		
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getID() {
		return this.ID;
	}
	
	public int getLength() {
		return this.length;
	}
	
}
