package com.simple.ipeer.iutil2.youtube;

public class Upload {

    private String title, ID;
    private String length;

    public Upload(String title, String length, String id) {
	this.title = title;
	this.ID = id;
	this.length = length;
    }

    /*public Upload(String title, int length, String ID) {
		
     this.title = title;
     this.ID = ID;
     this.length = length;
		
     }*/
    public String getTitle() {
	return this.title;
    }

    public String getID() {
	return this.ID;
    }

    public String getLength() {
	return this.length;
    }

}
