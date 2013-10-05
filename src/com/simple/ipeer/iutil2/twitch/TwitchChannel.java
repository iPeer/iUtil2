package com.simple.ipeer.iutil2.twitch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.Main;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class TwitchChannel implements Announcer, Runnable {
    
    private boolean isRunning = false;
    private Main engine;
    private String channelName;
    private long lastUpdate = 0L;
    private Thread thread;
    private Twitch twitch;
    private File cacheFile;
    private boolean shouldUpdate = true;
    
    public TwitchChannel(String name, Main engine, Twitch twitch) {
	this.engine = engine;
	this.channelName = name;
	this.twitch = twitch;
	this.cacheFile = new File((engine == null ? "./Twitch" : engine.config.getProperty("twitchDir"))+"/cache/"+name+".iuc");
    }
    
    
    //    	public static void main(String[] args) {
    //    		TwitchChannel a = new TwitchChannel("Pakratt0013", null, null);
    //    		a.removeCache();
    //    		a.update();
    //    	}
    
    @Override
    public void run() {
	while (this.isRunning && !this.thread.isInterrupted()) {
	    twitch.syncChannelsIfNotSyncing();
	    if (shouldUpdate) {
		try {
		    update();
		} catch (Throwable e) {
		    if (engine != null) {
			engine.log("There was a problem while updating Twitch user "+this.channelName);
			engine.logError(e, "Twitch", this.channelName);
		    }
		}
		lastUpdate = System.currentTimeMillis();
	    }
	    else
		this.shouldUpdate(true);
	    try {
		Thread.sleep(Long.valueOf((engine == null ? "600000" : engine.config.getProperty("twitchUpdateDelay"))));
	    }
	    catch (InterruptedException e) { }
	    catch (Exception e) {
		if (engine != null)
		    engine.log("An error occurred during run() method of twitch user "+this.channelName, "Twitch");
		engine.logError(e, "Twitch", this.channelName);
	    }
	}
	if (engine != null)
	    engine.log("Twitch thread for user "+this.channelName+" is stopping.", "Twitch");
	this.isRunning = false;
    }
    
    @Override
    public void update() throws IOException, SAXException, ParserConfigurationException {
	List<String> streamData = new LinkedList<String>();
	DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
	DocumentBuilder a;
	a = f.newDocumentBuilder();
	Document doc = a.newDocument();
	doc = a.parse("https://api.justin.tv/api/stream/list.xml?channel="+this.channelName);
	doc.getDocumentElement().normalize();
	
	NodeList data = doc.getDocumentElement().getElementsByTagName("stream");
	if (data.getLength() > 0) {
	    String gameName = "", streamDesc = "", streamQuality = "", streamID = "";
	    data = data.item(0).getChildNodes();
	    streamID = ((Element)data).getElementsByTagName("id").item(0).getFirstChild().getNodeValue();
	    //data.item(11).getFirstChild().getNodeValue();
	    // The Stream title changes place (doesn't that defeat the point of an API?), so we have to do it this way...
	    try {
		streamDesc = ((Element)data).getElementsByTagName("title").item(0).getFirstChild().getNodeValue().trim();
	    }
	    catch (NullPointerException e) { }
	    // Seems the quality can sometimes be contaminated by the title of the stream.
	    streamQuality = ((Element)data).getElementsByTagName("video_height").item(0).getFirstChild().getNodeValue();
	    //data.item(17).getFirstChild().getNodeValue();
	    try {
		gameName = ((Element)data).getElementsByTagName("meta_game").item(0).getFirstChild().getNodeValue();
	    }
	    catch (NullPointerException e) { }
	    //data.item(27).getFirstChild().getNodeValue();
	    streamData.add(0, streamID);
	    streamData.add(1, streamDesc.replaceAll("&#39;", "'").replaceAll("[\\n\\r]", " "));
	    streamData.add(2, streamQuality);
	    streamData.add(3, gameName.replaceAll("&#39;", "'"));
	}
	announce(streamData);
	
	
    }
    
    private void announce(List<String> data) {
	try {
	    String gameName = "", streamQuality = "", streamDesc = "", outMessage = "";
	    Properties a = new Properties();
	    if (data.isEmpty()) {
		if (this.cacheFile.exists()) {
		    outMessage = (engine == null ? "%C2%%USER% %C1%is no longer streaming." : engine.config.getProperty("twitchAnnounceFormatNotStreaming"));
		    this.cacheFile.delete();
		}
		else { return; }
	    }
	    else {
		if (this.cacheFile.exists()) {
		    a.load(new FileInputStream(this.cacheFile));
		    gameName = a.getProperty("lastGame");
		    streamDesc = a.getProperty("lastStatus");
		    streamQuality = a.getProperty("lastQuality", "");
		}
		/*
		 * 0 = ID
		 * 1 = Desc
		 * 2 = Quality
		 * 3 = game
		 */
		if (!(data.get(3)+data.get(1)+data.get(2)).equals(gameName+streamDesc+streamQuality)) {
		    if (data.get(3).equals("") && data.get(1).equals("")) // No game OR desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming [%C2%%STREAMQUALITY%%C1%] %DASH% %URL%" : engine.config.getProperty("twitchAnnounceFormatNoGameOrDesc"));
		    else if (data.get(3).equals("") && !data.get(1).equals("")) // No game, has desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%STREAMDESC% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %URL%" : engine.config.getProperty("twitchAnnounceFormatNoGame"));
		    else if (!data.get(3).equals("") && data.get(1).equals("")) // Game, no desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %URL%" : engine.config.getProperty("twitchAnnounceFormatNoDesc"));
		    else // everything present
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%(%C2%%STREAMDESC%%C1%) [%C2%%STREAMQUALITY%%C1%] %DASH% %URL%" : engine.config.getProperty("twitchAnnounceFormat"));
		    a.put("lastGame", data.get(3));
		    a.put("lastStatus", data.get(1));
		    a.put("lastID", data.get(0));
		    a.put("lastQuality", data.get(2));
		    a.store(new FileOutputStream(this.cacheFile), "Twitch.TV Cache for "+this.channelName);
		}
		
	    }
	    if (!outMessage.equals("")) {
		outMessage = outMessage
			.replaceAll("%(STREAM)?DESC%", a.getProperty("lastStatus"))
			.replaceAll("%GAME(NAME)?%", a.getProperty("lastGame"))
			.replaceAll("%(STREAM)?QUALITY%", a.getProperty("lastQuality")+"p")
			.replaceAll("%(STREAM)?ID%", a.getProperty("lastID"))
			.replaceAll("%USER%", this.channelName)
			.replaceAll("%URL%", (engine == null ? "https://twitch.tv/" : engine.config.getProperty("twitchURLPrefix"))+this.channelName);
		if (engine != null)
		    engine.amsg(outMessage);
		else
		    System.out.println(outMessage);
	    }
	}
	catch (IOException e) {
	    if (engine != null)
		engine.log("Couldn't announce twitch status for user "+this.channelName, "Twitch");
	    engine.logError(e, "Twitch");
	}
    }
    
    @Override
    public long timeTilUpdate() {
	return (lastUpdate + Long.valueOf((engine == null ? "600000" : engine.config.getProperty("twitchUpdateDelay")))) - System.currentTimeMillis();
    }
    
    @Override
    public void stop() {
	if (engine != null)
	    engine.log("Twitch.tv Stream Announcer Thread "+this.channelName+" is stopping", "Twitch");
	this.isRunning = false;
	if (this.thread != null)
	    this.thread.interrupt();
    }
    
    public void stopIfRunning() {
	if (this.isRunning)
	    stop();
    }
    
    @Override
    public void start() {
	this.isRunning = true;
	(this.thread = new Thread(this, "Twitch.tv Stream Announcer Thread ("+this.channelName+")")).start();
    }
    
    @Override
    public void startIfNotRunning() {
	if (!this.isRunning) {
	    start();
	    if (engine != null)
		engine.log("Twitch.tv Stream Announcer Thread "+this.channelName+" is starting", "Twitch");
	}
    }
    
    
    public void removeCache() {
	System.err.println(this.cacheFile.exists()+", "+this.cacheFile.getAbsolutePath());
	if (this.cacheFile.exists()) {
	    if (!this.cacheFile.delete())
		this.cacheFile.deleteOnExit();
	}
    }
    
    public String getName() {
	return this.channelName;
    }
    
    
    @Override
    public void shouldUpdate(boolean b) {
	this.shouldUpdate = b;
    }
    
    @Override
    public boolean isDead() {
	return timeTilUpdate() < 0;
    }
    
}
