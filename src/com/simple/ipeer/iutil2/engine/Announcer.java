package com.simple.ipeer.iutil2.engine;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public interface Announcer {

	public void update() throws IOException, ParserConfigurationException, SAXException;
	public long timeTilUpdate();
	public void stop();
	public void start();
	public void startIfNotRunning();
	public void removeCache();
	public void stopIfRunning();
	public void shouldUpdate(boolean b);
	public boolean isDead();
	
}
