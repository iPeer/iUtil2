package com.simple.ipeer.iutil2.engine;

public interface AnnouncerHandler {

	public boolean addUser(String name);
	public boolean removeUser(String name);
	public void updateAll();
	public void startAll();
	public void stopAll();
	public void update(String name);
	public long timeTilUpdate();
	
}
