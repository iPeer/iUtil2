/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author iPeer
 */
public class AWeSomeStatus implements AnnouncerHandler, Runnable {
    
    private boolean isRunning = false;
    private Thread thread;
    private long lastUpdate = 0L;
    private Main engine;
    private HashMap<String, Long> downServers;
    
    public AWeSomeStatus(Main engine) {
	this.engine = engine;
	engine.log("AWeSome Status is starting up...", "AWeSomeStatus");
	this.downServers = new HashMap<String, Long>();
	HashMap<String, String> s = new HashMap<String, String>();
	s.put("asUpdateDelay", "60000");
	s.put("asServers", "auron.co.uk:35565,auron.co.uk:35566,auron.co.uk:35567");
	s.put("asServerNames", "auron.co.uk:35565|Survival,auron.co.uk:35566|Creative,auron.co.uk:35567|Events");
	s.put("asReportFailures", "false");
	s.put("asIgnoreFailuresFrom", "auron.co.uk:35567");
	s.put("asRereportFailuresAfter", "600000");
	engine.createConfigDefaults(s);
    }
    
    public void start() {
	if (!this.isRunning) {
	    this.isRunning = true;
	    (this.thread = new Thread(this, "AWeSome server status thread")).start();
	}
    }
    
    public void stop() {
	this.isRunning = false;
	this.thread.interrupt();
	this.engine.log("AWeSome server status thread is no longer running.", "AWeSomeStatus");
    }
    
    @Override
    public void run() {
	this.engine.log("AWeSome server status thread is now running", "AWeSomeStatus");
	while (this.isRunning && !this.thread.isInterrupted()) {
	    Set<String> servers = new HashSet<String>();
	    List<String> noAnnounce = Arrays.asList(engine.config.getProperty("asIgnoreFailuresFrom").split(","));
	    String[] server = (engine == null ? "127.0.0.1:35565" : engine.config.getProperty("asServers")).split(",");
	    HashMap<String, String> serverNames = new HashMap<String, String>();
	    for (String a1 : (engine == null ? "127.0.0.1:35565|Local" : engine.config.getProperty("asServerNames")).split(","))
		serverNames.put(a1.split("\\|")[0], a1.split("\\|")[1]);
	    for (String a : server) {
		String aAddr = (a.contains(":") || a.contains("]:") ? a.split("]?:")[0] : a);
		int aPort = (a.contains(":") || a.contains("]:") ? Integer.valueOf(a.split("]?:")[1]) : 25565);
		String apiString = "";
		Query q = null;
		try {
		    q = new Query(new InetSocketAddress(aAddr, aPort));
		    q.sendQuery();
		    Map<String, String> data = q.getData();
		    String[] players = q.getPlayers();
		    apiString += q.getInetAddress().getAddress()+":"+q.getInetAddress().getPort();
		    for (String k : data.keySet())
			apiString += (apiString.length() > 0 ? "\01" : "")+k+":"+data.get(k);
		    if (players.length > 0) {
			apiString += "\01";
			for (int x = 0; x < players.length; x++)
			    apiString += (x > 0 ? "," : "")+players[x];
		    }
//		    if (engine == null)
//			System.err.println(apiString);
//		    else
//			engine.log(apiString, "AWeSomeStatus");
		    if ((engine == null || engine.config.getProperty("asReportFailures").equals("true")) && downServers.containsKey(q.getAddress())) {
			downServers.remove(q.getAddress());
			if (engine == null)
			    System.err.println(serverNames.get(q.getAddress())+" is back online");
			else {
			    engine.send("PRIVMSG #QuestHelp :"+(serverNames.containsKey(q.getAddress()) ? serverNames.get(q.getAddress()) : "Unknown")+" server is back online!");
			    engine.log(serverNames.get(q.getAddress())+" is back online", "AWeSome Status");
			}
		    }
		}
		catch (Throwable e) {
		    if (!noAnnounce.contains(a)) {
			if (!downServers.containsKey(q.getAddress()) || (downServers.get(q.getAddress()) - System.currentTimeMillis()) > (engine == null ? 600000 : Long.valueOf(engine.config.getProperty("asRereportFailuresAfter")))) {
			    if (engine == null)
				System.err.println(q.getAddress()+" is down!");
			    else {
				engine.send("PRIVMSG #QuestHelp :"+serverNames.get(q.getAddress())+(((downServers.containsKey(q.getAddress()) ? downServers.get(q.getAddress()) : 0L) - System.currentTimeMillis()) > (engine == null ? 600000 : Long.valueOf(engine.config.getProperty("asRereportFailuresAfter"))) ? " is still down!" : " is down!"));
				engine.log(q.getAddress()+" is reporting downtime!", "AWeSomeStatus");
			    }
			    downServers.put(q.getAddress(), System.currentTimeMillis());
			}
		    }
		    apiString = q.getInetAddress().getAddress()+":"+q.getInetAddress().getPort()+"\01"+e.toString();
//		    if (engine != null)
//			engine.logError(e, "AWeSomeStatus", q.getAddress());
		}
		servers.add(apiString);
	    }
	    writeAPI(servers);
	    this.lastUpdate = System.currentTimeMillis();
	    try {
		Thread.sleep((engine == null ? 60000 : Long.valueOf(engine.config.getProperty("asUpdateDelay"))));
	    } catch (InterruptedException e) {
		if (engine != null) {
		    this.isRunning = false; // Safety
		    engine.log("Thread couldn't sleep!", "AWeSomeStatus");
		    engine.logError(e, "AWeSomeStatus");
		}
	    }
	}
    }
    
    private void writeAPI(Set<String> servers) {
	File out = new File((engine == null ? "./api" : engine.config.getProperty("apiDataDir")), "AWeSomeStatus.api");
	try (FileWriter fr = new FileWriter(out, false)) {
	    for (Iterator<String> it = servers.iterator(); it.hasNext();) {
		String a = it.next();
		fr.write(a+"\n");
	    }
	}
	catch (IOException ex) {
	    if (engine == null)
		System.err.println("Couldn't update API Data!");
	    else {
		engine.log("Couldn't update API Data!", "AWeSomeStatus");
		engine.logError(ex, "AWeSomeStatus", out.getAbsolutePath());
	    }
	}
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
	
    }
    
    @Override
    public void startAll() {
	start();
    }
    
    @Override
    public void stopAll() {
	
    }
    
    @Override
    public void update(String name) {
	
    }
    
    @Override
    public long timeTilUpdate() {
	return (this.lastUpdate + getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public long getUpdateDelay() {
	return (this.engine == null ? 60000 : Long.valueOf(engine.config.getProperty("asUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
	
    }
    
}
