package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.Main;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author iPeer
 */
public class AWeSomeChatTailer implements Runnable, IAWeSomeChatTailer {
    
    private File tailFile, outFile, cacheDir/*, serverCacheDir*/;
    private Main engine;
    private String serverName = "";
    
    private boolean isRunning = false;
    private Thread thread;
    
    private List<String> onlineUsers = new ArrayList<String>();
    
    public static void main(String[] args) {
	AWeSomeChatTailer act = new AWeSomeChatTailer(null, "F:\\MC Server\\server.log", "Test");
	act.startIfNotRunning();
    }
    
    public AWeSomeChatTailer (Main engine, String file, String serverName) {
	this(engine, new File(file), serverName);
    }
    
    public AWeSomeChatTailer (Main engine, File file, String serverName) {
	this.engine = engine;
	this.tailFile = file;
	this.serverName = serverName;
	
	this.cacheDir = new File("./AWeSome/chat/"+this.serverName.toLowerCase());
	if (!this.cacheDir.exists())
	    this.cacheDir.mkdirs();
	//	this.serverCacheDir = new File(cacheDir, this.serverName.toLowerCase()+"/");
	//	if (!this.serverCacheDir.exists())
	//	    this.serverCacheDir.mkdirs();
	if (engine != null)
	    engine.log("AWeSomeChat Thread for "+serverName+" is waiting for start signal.", "AWeSomeChat");
    }
    
    @Override
    public void start() {
	this.isRunning = true;
	(this.thread = new Thread(this, "AWeSome "+this.serverName+" Chat")).start();
	if (engine != null)
	    engine.log("AWeSome "+this.serverName+" Chat thread is starting", "AWeSomeChat");
    }
    
    @Override
    public void startIfNotRunning() {
	if (!this.isRunning)
	    start();
    }
    
    @Override
    public void stop() {
	if (this.isRunning) {
	    if (engine != null)
		engine.log("AWeSome "+this.serverName+" Chat thread is stopping", "AWeSomeChat");
	    this.isRunning = false;
	    this.thread.interrupt();
	}
    }
    
    @Override
    public void run() {
	try {
	    RandomAccessFile handle = new RandomAccessFile(this.tailFile, "r");
	    Properties cache = new Properties();
	    File markerFile = new File(this.cacheDir, "marker.iuc");
	    if (markerFile.exists())
		cache.load(new FileInputStream(markerFile));
	    if (engine != null)
		engine.log("AWeSomeChat Thread for "+this.serverName+" is now running", "AWeSomeChat");
	    else
		System.err.println("AWeSomeChat Thread for "+this.serverName+" is now running");
	    String line = "";
	    boolean saveCache = false;
	    while (this.isRunning && !thread.isInterrupted()) {
		long marker = Long.valueOf(cache.getProperty("marker", "0"));
		long newMarker = 0L;
		handle.seek(marker);
		if ((line = handle.readLine()) != null) {
		    saveCache = true;
		    newMarker = handle.getFilePointer();
		    try {
			parseLine(line);
		    } catch (Throwable e) {
			if (engine == null)
			    e.printStackTrace(); 
			else {
			    engine.log("Unable to parse line.", "AWeSomeChat");
			    engine.logError(e, "AWeSomeChat", line);
			}
		    }
		    cache.put("marker", Long.toString(newMarker));
		}
		else {
		    if (saveCache) {
			saveCache = false;
			cache.store(new FileOutputStream(markerFile), "");
		    }
		    Thread.sleep((engine == null ? 500 : Long.valueOf(engine.config.getProperty("ascUpdateDelay"))));
		}
	    }
	    handle.close();
	}
	catch (InterruptedException e) { this.isRunning = false; }
	catch (IOException | NumberFormatException e) {
	    this.isRunning = false;
	    if (engine == null)
		e.printStackTrace();
	    else {
		engine.log("Couldn't continue tailing!", "AWeSomeChat");
		engine.logError(e, "AWeSomeChat");
	    }
	}
    }
    
    public void parseLine(String line) {
	if (line.length() == 0) { return; }
	
	List<String> out = new ArrayList<String>();
	List<String> logLines = new ArrayList<String>();
	if (line.contains("[INFO] <")) { // Chat
	    String[] data = line.split(" ");
	    String user = data[3].replaceAll("[\\<\\>]", "");
	    String message = line.split(user+"> ")[1];
	    if (!this.onlineUsers.contains(user))
		this.onlineUsers.add(user);
	    out.add((engine == null ? "%C2%%USER%%C1%: %MESSAGE%" : engine.config.getProperty("ascOutputFormat"))
		    .replaceAll("%USER(NAME)?%", user)
		    .replaceAll("%MESSAGE%", message));
	    logLines.add(data[0]+" "+data[1]+" "+user+": "+message);
	}
	
	else if (line.contains("left the game") || line.contains("joined the game")) { // (dis)connects
	    String[] data = line.split(" ");
	    String type = data[4];
	    String user = data[3];
	    
	    if (type.equals("joined"))
		onlineUsers.add(user);
	    else
		onlineUsers.remove(user);
	    
	    saveOnline();
	    
	    out.add((engine == null ? "%C2%%USER%%C1% %TYPE% the game." : engine.config.getProperty("ascOutputInOutFormat"))
		    .replaceAll("%TYPE%", type)
		    .replaceAll("%USER(NAME)?%", user));
	}
	
	else if (line.contains("[INFO] *")) { // Actions
	    String[] data = line.split(" ");
	    String user = data[4];
	    String message = line.split(user+" ")[1];
	    if (!this.onlineUsers.contains(user))
		this.onlineUsers.add(user);
	    
	    out.add((engine == null ? "%C2%%USER%%C1% %ACTION%" : engine.config.getProperty("ascOutputActionFormat"))
		    .replaceAll("%ACTION%", message)
		    .replaceAll("%USER(NAME)?%", user));
	}
	
	else if (line.contains("[INFO]") && onlineUsers.contains(line.split(" ")[3]) && !line.contains("lost connection")) { // Deaths
	    String[] data = line.split(" ");
	    String user = data[3];
	    String deathMessage = line.split(user+" ")[1];
	    
	    out.add((engine == null ? "%C2%%USER%%C1% %DEATHMESSAGE%" : engine.config.getProperty("ascOutputDeathFormat"))
		    .replaceAll("%DEATHMESSAGE%", deathMessage)
		    .replaceAll("%USER(NAME)?%", user));
	    Properties deaths = new Properties();
	    try {
		if (new File(this.cacheDir, "deaths.iuc").exists())
		    deaths.load(new FileInputStream(new File(this.cacheDir, "deaths.iuc")));
		int death = (deaths.containsKey(user) ? Integer.valueOf(deaths.getProperty(user)) : 0) + 1;
		out.add("%C2%"+user+"%C1% has died "+(death == 1 ? "for the first time!" : "%C2%"+death+"%C1% times!"));
		deaths.put(user, Integer.toString(death));
		deaths.store(new FileOutputStream(new File(this.cacheDir, "deaths.iuc")), "Death counter file");
	    }
	    catch (IOException | NumberFormatException e) {
		if (engine == null)
		    e.printStackTrace();
		else {
		    engine.log("Couldn't load or save deaths file", "AWeSomeChat");
		    engine.logError(e, "AWeSomeChat", new File(this.cacheDir, "deaths.iuc").getAbsolutePath());
		    
		}
	    }
	}
	
	else if (line.contains("Stopping the server")) { //Server stop
	    String[] data = line.split(" ");
	    if (!this.onlineUsers.isEmpty()) {
		for (Iterator<String> it = this.onlineUsers.iterator(); it.hasNext();) {
		    String user = it.next();
		    out.add((engine == null ? "%C2%%USER%%C1% %TYPE% the game." : engine.config.getProperty("ascOutputInOutFormat"))
		    .replaceAll("%TYPE%", "left")
		    .replaceAll("%USER(NAME)?%", user));
		    logLines.add(data[0]+" "+data[1]+" "+user+" left the game");
		}
		this.onlineUsers.clear();
		saveOnline();
	    }
	    out.add("Server stopping!");
	}
	
	else if (line.contains("For help, type \"help\" or \"?\"")) { // Server started
	    out.add("Server started.");
	}
	
	if (!out.isEmpty()) {
	    for (Iterator<String> it = out.iterator(); it.hasNext();) {
		String outLine = it.next();
		if (engine == null)
		    System.err.println(("%C1%[%C2%AWeSome %SERVERNAME%%C1%]: "+outLine).replaceAll("%SERVER(NAME)?%", this.serverName).replaceAll("%C[12]+%", ""));
		else
		    engine.send("PRIVMSG #QuestHelp :"+(engine.config.getProperty("ascOutputPrefix")+outLine).replaceAll("%SERVER(NAME)?%", this.serverName));
	    }
	    
	    if (logLines.isEmpty())
		writeToLog(line.replaceAll(" \\[INFO\\]", ""));
	    else
		for (Iterator<String> it = logLines.iterator(); it.hasNext();)
		    writeToLog(it.next());
	    
	}
	
    }
    
    @Override
    public File getFile() {
	return this.tailFile;
    }
    
    @Override
    public File getInputFile() {
	return getFile();
    }
    
    @Override
    public File getOutput() {
	return this.outFile;
    }
    
    @Override
    public File getOutputFile() {
	return getOutput();
    }
    
    @Override
    public String getName() {
	return this.serverName;
    }
    
    @Override
    public void setServerName(String name) {
	this.serverName = name;
    }
    
    public void saveOnline() {
	if (this.onlineUsers.isEmpty()) {
	    new File(this.cacheDir, "online.iuc").delete();
	    return;
	}
	try (FileWriter w = new FileWriter(new File(this.cacheDir, "online.iuc"), false)) {
	    for (Iterator<String> it = onlineUsers.iterator(); it.hasNext();)
		w.write(it.next());
	    w.close();
	} catch (IOException e) {
	    if (engine == null)
		e.printStackTrace();
	    else {
		engine.log("Unable to save online users list." , "AWeSomeChat");
		String[] data = new String[this.onlineUsers.size() + 1];
		int x = 0;
		for (Iterator<String> it = onlineUsers.iterator(); it.hasNext(); x++)
		    data[x] = it.next();
		data[x++] = new File(this.cacheDir, "online.iuc").getAbsolutePath();
		engine.logError(e, "AWeSomeChat", data);
	    }
	}
    }
    
    public void loadOnline() {
	try (BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(new File(this.cacheDir, "online.iuc")))))) {
	    String line = "";
	    while ((line = br.readLine()) != null)
		this.onlineUsers.add(line);
	    br.close();
	}
	catch (FileNotFoundException ex) { }
	catch (IOException ex) {
	    if (engine == null)
		ex.printStackTrace();
	    else {
		engine.log("Unable to load online users from cache." , "AWeSomeChat");
		engine.logError(ex, "AWeSomeChat", new File(this.cacheDir, "online.iuc").getAbsolutePath());
	    }
	}
    }
    
    private void writeToLog(String line) {
	try (FileWriter w = new FileWriter(new File(this.cacheDir, "chat.txt"), true)) {
	    w.write(line+"\r\n");
	    w.close();
	} catch (IOException ex) {
	    if (engine == null)
		ex.printStackTrace();
	    else {
		engine.log("Unable to add parsed line to log." , "AWeSomeChat");
		engine.logError(ex, "AWeSomeChat", line, new File(this.cacheDir, "chat.txt").getAbsolutePath());
	    }
	}
    }
    
}
