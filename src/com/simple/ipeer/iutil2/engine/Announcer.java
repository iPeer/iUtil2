package com.simple.ipeer.iutil2.engine;

public interface Announcer {

	public void update();
	public long timeTilUpdate();
	public void stop();
	public void start();
	public void startIfNotRunning();
	public void removeCache();
	public void stopIfRunning();
	
}
