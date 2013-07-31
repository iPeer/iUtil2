package com.simple.ipeer.iutil2.engine;

public class OfflineMessage {
	
	private String msg;
	private boolean log = true, sendIfNotConnected = false;

	public OfflineMessage(String msg, boolean log, boolean sendIfNotConnected) {
		this.msg = msg;
		this.log = log;
		this.sendIfNotConnected = sendIfNotConnected;
	}
	
	public String getText() {
		return this.msg;
	}
	
	public boolean shouldLog() { 
		return this.log;
	}
	
	public boolean shouldSendWhenNotConnected() { // Long winded ass method name
		return this.sendIfNotConnected;
	}
	
}
