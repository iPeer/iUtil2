package com.simple.ipeer.iutil2.irc;

import java.util.HashMap;
import java.util.Map;

public class Channel {

	private String name;
	private final Map<String, User> users;
	
	public Channel(String name) {
		this.name = name;
		this.users = new HashMap<String, User>();
	}
	
	public String getName() {
		return name.toLowerCase();
	}
	
	public Map<String, User> getUserList() {
		return users;
	}

	
}
