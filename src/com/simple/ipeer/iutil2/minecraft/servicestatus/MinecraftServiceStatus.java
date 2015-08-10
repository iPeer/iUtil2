package com.simple.ipeer.iutil2.minecraft.servicestatus;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.LogLevel;
import com.simple.ipeer.iutil2.engine.Main;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author iPeer
 */
public class MinecraftServiceStatus implements AnnouncerHandler, Runnable, DebuggableSub {
    
    private long lastUpdate = 0L;
    private Main engine;
    private String sessionID = "";
    private HashMap<String, IMinecraftService> services;
    private boolean isRunning = false;
    private Thread thread;
    private HashMap<String, HashMap<String, String>> statusData;
    private List<Announcer> announcerList = new ArrayList<Announcer>();
    private long lastExceptionTime = 0L;
    private Throwable lastException;
    private long startupTime = 0L;
    private PreparedStatement sqlQ;
    
    public static void main (String[] args) {
	MinecraftServiceStatus a = new MinecraftServiceStatus(null);
	a.updateAll();
    }
    
    public MinecraftServiceStatus(Main engine) {
	this.engine = engine;
	
	if (engine != null)
	    engine.log("Minecraft Service Status Checker is preparing to start.", "MinecraftServiceStatus");
	try {
	    this.sqlQ = engine.getSQLConnection().prepareStatement("UPDATE minecraft_status SET (address, status, ping, servername, error) = (?, ?, ?, ?, ?) WHERE servername = ?");
	} catch (SQLException ex) {
	    engine.logError(ex, "Unable to prepare SQL query for Minecraft status. API can not and will not be updated.", "SQL");
	    engine.log("Couldn't prepare SQL query for Minecraft Status updates.", "SQL", LogLevel.ALL);
	}
	
	statusData = new HashMap<String, HashMap<String, String>>();
	
	HashMap<String, String> s = new HashMap<String, String>();
	s.put("mcssAnnounceFailures", "false");
	s.put("mcssUpdateDelay", "60000");
	s.put("mcssFailureFormat", "%C1%[%C2%Minecraft Status%C1%] %C2%%SERVICE%%C1% is reporting downtime!");
	s.put("mcssBackOnlineFormat", "%C1%[%C2%Minecraft Status%C1%] %C2%%SERVICE%%C1% is back online!");
	
	if (engine != null)
	    engine.createConfigDefaults(s);
	
	File a = new File("./Minecraft");
	if (!a.exists())
	    a.mkdirs();
	
	services = new HashMap<String, IMinecraftService>();
	
	services.put("Skins", new MinecraftService("http://skins.minecraft.net/MinecraftSkins/iPeer.png"));
	services.put("Website", new MinecraftService("https://minecraft.net/"));
	services.put("Accounts", new MinecraftService("https://account.mojang.com/"));
	services.put("Yggdrasil Auth", new MinecraftService("https://authserver.mojang.com/"));
	//services.put("Login", new MinecraftLoginService("https://login.minecraft.net/", this));
	//services.put("Session", new MinecraftService("https://session.minecraft.net/game/checkserver.jsp"));
	services.put("Textures", new MinecraftService("http://textures.minecraft.net:8080/"));
	services.put("Player Data", new MinecraftService("https://sessionserver.mojang.com/"));
	//services.put("Multiplayer Sessions", new MinecraftService("https://session.minecraft.net/game/checkserver.jsp"));
	//services.put("Realms", new MinecraftService(""));
	for (IMinecraftService b : services.values())
	    announcerList.add((Announcer)b);
	
	startupTime = System.currentTimeMillis();
	
    }
    
    @Override
    public boolean addUser(String name) {
	return false;
    }
    
    @Override
    public boolean removeUser(String name) {
	return false;
    }
    
    @Override
    public void updateAll() {
	try {
	    String line = "";
	    for (Iterator<String> it = services.keySet().iterator(); it.hasNext();) {
		String key = it.next();
		IMinecraftService s = services.get(key);
		try {
		    s.update();
		    HashMap<String, String> data = s.getData();
//		line += s.getAddress()+"\01"+data.get("ping")+"\01"+
//			(data.containsKey("status") && data.get("status").equals("200") ? "up" : "down")+"\01"+
//			(data.containsKey("errorMessage") ? data.get("errorMessage") : data.get("status"))+"\n";
//		statusData.put(key, data);
		    /* address, status, ping, servername, error */
		    
		    this.sqlQ.setString(1, s.getAddress());
		    this.sqlQ.setInt(2, (data.get("status").equals("200") && !data.containsKey("errorMessage")) ? 1 : 2);
		    this.sqlQ.setInt(3, Integer.parseInt(data.get("ping")));
		    this.sqlQ.setString(4, key);
		    this.sqlQ.setString(5, data.containsKey("errorMessage") ? data.get("errorMessage") : "");
		    this.sqlQ.setString(6, key);
		    engine.log("SQL QUERY: "+this.sqlQ.toString(), "SQL", LogLevel.DEBUG_ONLY);
		    this.sqlQ.executeUpdate();
		    
//		if (engine == null || engine.config.get("debug").equals("true"))
//		    for (String a : statusData.keySet())
//			System.err.println(a+": "+statusData.get(a));
		} catch (Throwable e) { }
	    }
	    Main.statusUpdateTimeQ.setInt(1, (int)(System.currentTimeMillis() / 1000L));
	    Main.statusUpdateTimeQ.setString(2, "minecraft");
	    Main.statusUpdateTimeQ.executeUpdate();
	} catch (SQLException ex) {
	    engine.logError(ex, "SQL", this.sqlQ.toString());
	    if (ex.getMessage().equals("This connection has been closed.")) {
		engine.log("SQL Connection is no longer valid. Another will be created.", "SQL", LogLevel.LOG_DEBUG_AND_CHANNEL);
		engine.createSQLConnection();
		try {
		    this.sqlQ = engine.getSQLConnection().prepareStatement("UPDATE minecraft_status SET (address, status, ping, servername, error) = (?, ?, ?, ?, ?) WHERE servername = ?");
		} catch (SQLException ex1) {
		    
		}
	    }
	}
    }
    
    public void stop() {
	this.isRunning = false;
	thread.interrupt();
	if (engine != null)
	    engine.log("Minecraft Service Status Checker is stopping.", "MinecraftServiceStatus");
    }
    
    public void start() {
	this.isRunning = true;
	(thread = new Thread(this, "Minecraft Service Status Checker")).start();
	if (engine != null)
	    engine.log("Minecraft Service Status Checker is starting.", "MinecraftServiceStatus");
    }
    
    public void startIfNotRunning() {
	if (!this.isRunning)
	    start();
    }
    
    @Override
    public void startAll() {
	startIfNotRunning();
    }
    
    @Override
    public void stopAll() {
    }
    
    @Override
    public void update(String name) {
    }
    
    @Override
    public long timeTilUpdate() {
	return (this.lastUpdate + this.getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public long getUpdateDelay() {
	return (engine == null ? 60000 : Long.valueOf(engine.config.getProperty("mcssUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
    }
    
    @Override
    public void run() {
	while (isRunning && !thread.isInterrupted()) {
	    updateAll();
	    lastUpdate = System.currentTimeMillis();
	    try {
		Thread.sleep(getUpdateDelay());
	    } catch (InterruptedException ex) {
		lastException = ex;
		lastExceptionTime = System.currentTimeMillis();
		this.isRunning = false;
		engine.log("Couldn't sleep!", "MinecraftServiceStatus");
		engine.logError(ex, "MinecraftServiceStatus");
	    }
	}
    }
    
    public void setSSID(String id) {
	this.sessionID = id;
    }
    
    public HashMap<String, HashMap<String, String>> getStatusData() {
	return this.statusData;
    }
    
    @Override
    public int getDeadThreads() {
	return (timeTilUpdate() < 0 ? 1 : 0);
    }
    
    @Override
    public int getTotalThreads() {
	return 1;
    }
    
    @Override
    public List<Announcer> getAnnouncerList() {
	return announcerList;
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
	return lastUpdate;
    }
    
    public long getStartupTime() {
	return startupTime;
    }

    @Override
    public boolean addYTUser(String channel, boolean isUsername) {
	return true;
    }
    
}
