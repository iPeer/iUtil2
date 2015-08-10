package com.simple.ipeer.iutil2.twitch;

import com.simple.ipeer.iutil2.engine.Announcer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Debuggable;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;

public class Twitch implements AnnouncerHandler, Debuggable, DebuggableSub {
    
    private Main engine;
    private HashMap<String, TwitchChannel> users;
    public List<TwitchChannel> waitingToSync = new ArrayList<TwitchChannel>();
    private boolean isSyncing = false;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    private long lastForcedUpdate = 0L;
    private List<Announcer> announcerList = new ArrayList<Announcer>();
    
    public Twitch (Main engine) {
	engine.log("Twitch announcer is starting up.", "Twitch");
	this.engine = engine;
	HashMap<String, String> settings = new HashMap<String, String>();
	settings.put("twitchUpdateDelay", "600000");
	settings.put("twitchAnnounceFormat", "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%(%C2%%STREAMDESC%%C1%) [%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
	settings.put("twitchAnnounceFormatNoGame", "%C2%%USER% %C1%is streaming %C2%%STREAMDESC% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
	settings.put("twitchAnnounceFormatNoDesc", "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
	settings.put("twitchAnnounceFormatNoGameOrDesc", "%C2%%USER% %C1%is streaming [%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
	settings.put("twitchAnnounceFormatNotStreaming", "%C2%%USER% %C1%is no longer streaming.");
	settings.put("twitchURLPrefix", "http://twitch.tv/");
	settings.put("twitchDir", "./Twitch");
	settings.put("twitchDefaultChannels", "BdoubleO100,GenerikB,Kurtjmac,Harumei,Nebris,TotalBiscuit");
	engine.createConfigDefaults(settings);
	File a = new File(engine.config.getProperty("twitchDir"), "cache");
	if (!a.exists())
	    a.mkdirs();
	a = new File(engine.config.getProperty("twitchDir"), "config");
	if (!a.exists())
	    a.mkdirs();
	this.users = loadChannels();
	if (engine != null)
	    engine.log("Twitch announcer started up succesfully.", "Twitch");
    }
    
    public void saveChannels() {
	if (engine != null)
	    engine.log("Attempting to save Twitch usernames...", "Twitch");
	File config = new File((engine == null ? "./Twitch" : engine.config.getProperty("twitchDir"))+"/config/usernames.cfg");
	try {
	    FileWriter out = new FileWriter(config);
	    for (TwitchChannel c : this.users.values())
		out.write(c.getName()+"\n");
	    out.flush();
	    out.close();
	    if (engine != null)
		engine.log("Twitch usernames saved succesfully.", "Twitch");
	} catch (IOException e) {
	    if (engine != null)
		engine.log("Couldn't save Twitch channels!", "Twitch");
	    engine.logError(e);
	}
    }
    
    public HashMap<String, TwitchChannel> loadChannels() {
	if (engine != null)
	    engine.log("Attempting to load Twitch usernames from file", "Twitch");
	File config = new File((engine == null ? "./Twitch" : engine.config.getProperty("twitchDir"))+"/config/usernames.cfg");
	HashMap<String, TwitchChannel> ret = new HashMap<String, TwitchChannel>();
	try {
	    Scanner s = new Scanner(new FileInputStream(config), "UTF-8");
	    while (s.hasNextLine()) {
		String user = s.nextLine();
		ret.put(user.toLowerCase(), new TwitchChannel(user, engine, this));
		announcerList.add(ret.get(user.toLowerCase()));
	    }
	    s.close();
	    if (engine != null)
		engine.log("Succesfully loaded usernames from file", "Twitch");
	    return ret;
	} catch (FileNotFoundException e) {
	    if (engine != null)
		engine.log("Username file not found, loading default usernames.", "Twitch");
	    for (String u : engine.config.getProperty("twitchDefaultChannels").split(","))
		ret.put(u.toLowerCase(), new TwitchChannel(u, engine, this));
	    return ret;
	}
    }
    
    @Override
    public boolean addUser(String name) {
	if (engine != null)
	    engine.log("Attempting to add user "+name+" to Twitch user list...", "Twitch");
	if (this.users.containsKey(name.toLowerCase())) {
	    if (engine != null)
		engine.log("User "+name+" already appears in the list.", "Twitch");
	    return false;
	}
	TwitchChannel a = new TwitchChannel(name, engine, this);
	announcerList.add(a);
	try {
	    a.update();
	} catch (Throwable ex) {
	    engine.log("Couldn't add user to update list, see error log for details.", "Twitch");
	    engine.logError(ex, "Twitch", name);
	    throw new RuntimeException("Couldn't add user to watch list: "+ex.toString()+" @ "+ex.getStackTrace()[0]);
	}
	this.users.put(name.toLowerCase(), a);
	this.waitingToSync.add(a);
	if (engine != null)
	    engine.log("User "+name+" succesfully added to users list and is waiting to sync.", "Twitch");
	saveChannels();
	return true;
    }
    
    @Override
    public boolean removeUser(String name) {
	if (this.users.containsKey(name.toLowerCase())) {
	    if (!this.waitingToSync.isEmpty()) {
		Iterator<TwitchChannel> it = this.waitingToSync.iterator();
		while (it.hasNext())
		    if (it.next().getName().toLowerCase().equals(name.toLowerCase())) {
			it.remove();
			if (engine != null)
			    engine.log(name+" was waiting to sync, but its removal is being requested.", "Twitch");
		    }
	    }
	    TwitchChannel a = this.users.get(name.toLowerCase());
	    announcerList.remove(a);
	    a.stop();
	    a.removeCache();
	    this.users.remove(name.toLowerCase());
	    saveChannels();
	    return true;
	}
	return false;
    }
    
    @Override
    public void updateAll() {
	if (engine != null)
	    engine.log("Updating all watched channels.", "Twitch");
	for (TwitchChannel c : this.users.values())
	    try {
		c.update();
	    } catch (Throwable ex) {
		engine.logError(ex, "Twitch");
		engine.log("Couldn't update channel "+c.getName()+" due to error. See error log for details.", "Twitch");
	    }
    }
    
    @Override
    public void startAll() {
	//		if (announcerHelper == null) {
	//			announcerHelper = new AnnouncerHelper(this, "Twitch", engine, 900000);
	//			announcerHelper.start();
	//		}
	for (TwitchChannel c : this.users.values())
	    c.startIfNotRunning();
    }
    
    @Override
    public void stopAll() {
	for (TwitchChannel c : this.users.values()) {
	    if (engine != null)
		engine.log("Stopping TwitchChannel thread for user "+c.getName(), "Twitch");
	    c.stop();
	}
    }
    
    @Override
    public void update(String name) {
	if (this.users.containsKey(name.toLowerCase())) {
	    if (engine != null)
		engine.log("Updating "+name, "Twitch");
	    try {
		this.users.get(name.toLowerCase()).update();
	    } catch (Throwable ex) {
		engine.logError(ex, "Twitch");
		engine.log("Couldn't update channel "+name.toLowerCase()+" due to error. See error log for details.", "Twitch");
	    }
	}
    }
    
    @Override
    public long timeTilUpdate() {
	return this.users.values().iterator().next().timeTilUpdate();
    }
    
    public void syncChannelsIfNotSyncing() {
	if (!waitingToSync.isEmpty() && !isSyncing) {
	    isSyncing = true;
	    Iterator<TwitchChannel> it = waitingToSync.iterator();
	    while (it.hasNext()) {
		(it.next()).startIfNotRunning();
		it.remove();
	    }
	    isSyncing = false;
	}
    }
    
    @Override
    public long getUpdateDelay() {
	return Long.valueOf((engine == null ? "600000" : engine.config.getProperty("twitchUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
	((TwitchChannel)channel).stop();
	((TwitchChannel)channel).startIfNotRunning();
    }
    
    @Override
    public int getDeadThreads() {
	int x = 0;
	for (TwitchChannel a : this.users.values()) {
	    if (a.isDead())
		x++;
	}
	return x;
    }
    
    @Override
    public int getTotalThreads() {
	return users.size();
    }

    @Override
    public void writeDebug(FileWriter fw) {
    }

    @Override
    public Throwable getLastExeption() {
	return lastException;
    }

    @Override
    public long getLastExceptionTime() {
	return lastExceptionTime;
    }

    @Override
    public long getLastUpdateTime() {
	return lastForcedUpdate;
    }

    @Override
    public List<Announcer> getAnnouncerList() {
	return announcerList;
    }

    @Override
    public boolean addYTUser(String channel, boolean isUsername) {
	return true;
    }
    
}
