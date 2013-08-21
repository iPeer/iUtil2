package com.simple.ipeer.iutil2.tell;

public class TellMessage {

	private String sender, message;
	private long sendTime;
	
	public TellMessage(String s, String m) {
		this(s, System.currentTimeMillis(), m);
	}
	
	public TellMessage(String s, long t, String m) {
		this.sender = s;
		this.sendTime = t;
		this.message = m;
	}
	
	public TellMessage(String s, String t, String m) {
		this(s, Long.valueOf(t), m);
	}

	public String getMessage() {
		return this.message;
	}
	
	public long getTime() {
		return this.sendTime;
	}
	
	public String getSender() {
		return this.sender;
	}
	
}
