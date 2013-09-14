package com.simple.ipeer.iutil2.minecraft.servicestatus;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author iPeer
 */
public class MinecraftServiceStatus implements AnnouncerHandler, Runnable {
    
    private long lastUpdate = 0L;
    private Main engine;
    private String sessionID = "";
    private HashMap<String, IMinecraftService> services;
    private boolean isRunning = false;
    private Thread thread;
    private HashMap<String, HashMap<String, String>> statusData;
    
    public static void main (String[] args) {
	MinecraftServiceStatus a = new MinecraftServiceStatus(null);
	a.updateAll();
    }
    
    public MinecraftServiceStatus(Main engine) {
	this.engine = engine;
	
	if (engine != null)
	    engine.log("Minecraft Service Status Checker is preparing to start.", "MinecraftServiceStatus");
	
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
	services.put("Account", new MinecraftService("https://account.mojang.com/"));
	services.put("Yggdrasil Auth", new MinecraftService("https://authserver.mojang.com/"));
	services.put("Login", new MinecraftLoginService("https://login.minecraft.net/", this));
	services.put("Session", new MinecraftService("https://session.minecraft.net/game/checkserver.jsp"));
	
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
	String line = "";
	for (Iterator<String> it = services.keySet().iterator(); it.hasNext();) {
	    String key = it.next();
	    IMinecraftService s = services.get(key);
	    s.update();
	    HashMap<String, String> data = s.getData();
	    line += s.getAddress()+"\01"+data.get("ping")+"\01"+
		    (data.containsKey("status") && data.get("status").equals("200") ? "up" : "down")+"\01"+
		    (data.containsKey("errorMessage") ? data.get("errorMessage") : data.get("status"))+"\n";
	    statusData.put(key, data);
	}
	try {
	    FileWriter fw = new FileWriter(new File("./Minecraft/status.api"));
	    fw.write(line);
	    fw.close();
	} catch (IOException iOException) {
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
	return (lastUpdate + getUpdateDelay()) - System.currentTimeMillis();
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
    
}
