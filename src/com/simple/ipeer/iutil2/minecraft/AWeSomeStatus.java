package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.LogLevel;
import com.simple.ipeer.iutil2.engine.Main;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author iPeer
 */
public class AWeSomeStatus implements AnnouncerHandler, Runnable, DebuggableSub, Announcer {
    
    private boolean isRunning = false;
    private Thread thread;
    private long lastUpdate = 0L;
    private Main engine;
    private HashMap<String, Long> downServers;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    private long startupTime = 0L;
    
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
	this.startupTime = System.currentTimeMillis();
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
	    String query = "";
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
		    apiString += "'"+q.getInetAddress().getAddress()+":"+q.getInetAddress().getPort()+"'";
		    for (String k : data.keySet()) {
			if (k.equals("plitnum")) { continue; }
			apiString += (apiString.length() > 0 ? "," : "")+"'"+(k.equals("ping") ? data.get(k).replaceAll("ms", "") : data.get(k)).trim().replaceAll("\n", "{NEWLINE}").replaceAll("'", "''")+"'";
		    }
		    if (players.length > 0) {
			apiString += ",'";
			for (int x = 0; x < players.length; x++)
			    apiString += (x > 0 ? "," : "")+players[x];
			apiString += "'";
		    }
		    else {
			apiString +=",NULL";
		    }
		    query = "UPDATE awesome_status SET (address, maxplayers, plugins, gametype, game_id, map, hostname, numplayers, ping, hostip, hostport, version, players, errormessage) =  ("+apiString+",NULL) WHERE servername = '"+serverNames.get(q.getAddress())+"'";
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
		    apiString = "'"+q.getInetAddress().getAddress()+":"+q.getInetAddress().getPort()+"','"+e.toString()+"'";
		    query = "UPDATE awesome_status SET (address, errormessage) = ("+apiString+") WHERE servername = '"+serverNames.get(q.getAddress())+"'";
//		    if (engine != null)
//			engine.logError(e, "AWeSomeStatus", q.getAddress());
		}
		Statement s;
		try {
		    s = engine.getSQLConnection().createStatement();
		    engine.log("SQL QUERY: "+query, "SQL", LogLevel.DEBUG_ONLY);
		    s.executeUpdate(query);
		    Main.statusUpdateTimeQ.setInt(1, (int)(System.currentTimeMillis() / 1000L));
		    Main.statusUpdateTimeQ.setString(2, "awesome");
		    Main.statusUpdateTimeQ.executeUpdate();

		} catch (SQLException ex) {
		    engine.logError(ex, "SQL", query);
		    if (ex.getMessage().equals("This connection has been closed.")) {
			engine.log("SQL Connection is no longer valid. Another will be created.", "SQL", LogLevel.LOG_AND_DEBUG);
			engine.createSQLConnection();
		    }
		}
		//servers.add(apiString);
	    }
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
	return (this.lastUpdate + this.getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public long getUpdateDelay() {
	return (this.engine == null ? 60000 : Long.valueOf(engine.config.getProperty("asUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
	
    }
    
    @Override
    public int getDeadThreads() {
	return (this.timeTilUpdate() < 0 ? 1 : 0);
    }
    
    	@Override
	public int getTotalThreads() {
	    return 1;
	}

    @Override
    public List<Announcer> getAnnouncerList() {
	List<Announcer> a = new ArrayList<Announcer>();
	a.add((Announcer)this);
	return a;
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
    public void update() throws IOException, ParserConfigurationException, SAXException {
    }

    @Override
    public void startIfNotRunning() {
    }

    @Override
    public void removeCache() {
    }

    @Override
    public void stopIfRunning() {
    }

    @Override
    public void shouldUpdate(boolean b) {
    }

    @Override
    public boolean isDead() {
	return this.timeTilUpdate() < 0;
    }

    @Override
    public String getThreadName() {
	return this.thread.getName();
    }

    @Override
    public long getStartupTime() {
	return this.startupTime;
    }
    
}
