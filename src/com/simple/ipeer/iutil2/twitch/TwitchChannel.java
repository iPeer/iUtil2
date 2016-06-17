package com.simple.ipeer.iutil2.twitch;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;

import javax.xml.parsers.ParserConfigurationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import org.xml.sax.SAXException;

public class TwitchChannel implements Announcer, Runnable, DebuggableSub {
    
    private boolean isRunning = false;
    private Main engine;
    private String channelName;
    private long lastUpdate = 0L;
    private Thread thread;
    private Twitch twitch;
    private File cacheFile;
    private boolean shouldUpdate = true;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    private long startupTime = 0L;
    
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
	TwitchChannel t = new TwitchChannel("blamethecontroller", null, null);
	t.removeCache();
	t.update();
    }
    
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
	try {
	    URL url = new URL("https://api.twitch.tv/kraken/streams?channel="+this.channelName.toLowerCase());
	    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
	    connection.setRequestProperty("Client-ID", this.twitch.CLIENT_ID);
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    String jsonData = in.readLine();
	    
	    JSONObject json = (JSONObject)JSONValue.parse(jsonData);
	    
	    JSONArray streams = (JSONArray)json.get("streams");
	    
	    //System.err.println("DATA: "+streams);
	    
	    json = (JSONObject)JSONValue.parse(streams.get(0).toString());
	    
	    JSONObject channel = (JSONObject)json.get("channel");
	    
	    //System.err.println("NEW DATA: "+channel);
	    
	    String status = ((String)channel.get("status")).replaceAll("\\n", " ").replaceAll("\\r", "").trim();
	    String game = (String)channel.get("game");
	    String userName = (String)channel.get("display_name");
	    streamData.add(0, userName);
	    streamData.add(1, status);
	    streamData.add(2, "RESERVED");
	    streamData.add(3, game);
	} catch (IndexOutOfBoundsException e) { }
	//System.out.println(status+" / "+game+" / "+userName);
	
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
		* 0 = Username
		* 1 = Desc
		* 2 = Quality
		* 3 = game
		*/
		if (!(data.get(3)+data.get(1)/*+data.get(2)*/).equals(gameName+streamDesc/*+streamQuality*/)) {
		    if (data.get(3).equals("") && data.get(1).equals("")) // No game OR desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %DASH% %C2%%URL%" : engine.config.getProperty("twitchAnnounceFormatNoGameOrDesc"));
		    else if (data.get(3).equals("") && !data.get(1).equals("")) // No game, has desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%STREAMDESC% %DASH% %%C2%URL%" : engine.config.getProperty("twitchAnnounceFormatNoGame"));
		    else if (!data.get(3).equals("") && data.get(1).equals("")) // Game, no desc
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %DASH% %C2%%URL%" : engine.config.getProperty("twitchAnnounceFormatNoDesc"));
		    else // everything present
			outMessage = (engine == null ? "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%(%C2%%STREAMDESC%%C1%) %DASH% %C2%%URL%" : engine.config.getProperty("twitchAnnounceFormat"));
		    a.put("lastGame", data.get(3));
		    a.put("lastStatus", data.get(1));
		    //a.put("lastID", data.get(0));
		    
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
	startupTime = System.currentTimeMillis();
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
    
    @Override
    public String getThreadName() {
	return this.thread.getName();
    }
    
    @Override
    public Throwable getLastExeption() {
	return this.lastException;
    }
    
    @Override
    public long getLastExceptionTime() {
	return this.lastExceptionTime;
    }
    
    @Override
    public long getLastUpdateTime() {
	return this.lastUpdate;
    }
    
    @Override
    public long getStartupTime() {
	return this.startupTime;
    }

    @Override
    public boolean addYTUser(String name, boolean isChannel) {
	return true;
    }
    
}
