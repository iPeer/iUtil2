package com.simple.ipeer.iutil2.engine;

import java.util.List;

public interface AnnouncerHandler {

	public boolean addUser(String name);
	public boolean removeUser(String name);
	public void updateAll();
	public void startAll();
	public void stopAll();
	public void update(String name);
	public long timeTilUpdate();
	public long getUpdateDelay();
	public void scheduleThreadRestart(Object channel);
	public int getDeadThreads();
	public int getTotalThreads();
	public List<Announcer> getAnnouncerList();
	
}
