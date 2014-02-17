package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author iPeer
 */
public class AWeSomeChatTailer implements Runnable, IAWeSomeChatTailer, Announcer, DebuggableSub {
    
    private File tailFile, outFile, cacheDir/*, serverCacheDir*/;
    private Main engine;
    private String serverName = "";
    public long lastUpdate = 0L;
    private boolean isRunning = false;
    private Thread thread;
    private List<String> onlineUsers = new ArrayList<String>();
    private long startupTime = 0L;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    private boolean serverStopped = false;
    
    public static void main(String[] args) {
	/*AWeSomeChatTailer act = new AWeSomeChatTailer(null, "F:\\MC Server\\logs\\latest.log", "Test");
	act.startIfNotRunning();*/
	Date d = new Date();
	d.setTime(System.currentTimeMillis());
	DateFormat df = SimpleDateFormat.getDateInstance();
	System.err.println(df.format(d));
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
	startupTime = System.currentTimeMillis();
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
		if (this.tailFile.exists()) {
		    List<String> lines = Files.readAllLines(Paths.get(this.tailFile.getAbsolutePath()), (System.getProperty("os.name").equals("Linux") ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1));
		    //System.err.println(lines.size());
		    int x = Integer.parseInt(cache.getProperty("marker", "0"));
		    if (x > lines.size())
			x = 0;
		    for(; x < lines.size(); x++) {
			saveCache = true;
			try {
			    parseLine(lines.get(x));
			} catch (Throwable e) {
			    if (engine == null)
				e.printStackTrace();
			    else {
				engine.log("Unable to parse line.", "AWeSomeChat ("+this.serverName+")");
				engine.logError(e, "AWeSomeChat ("+this.serverName+")", line);
			    }
			}
//		    if (this.serverStopped) {
//			cache.put("marker", "0");
//			this.serverStopped = false;
//		    }
//		    else
		    }
		    if (saveCache) {
			saveCache = false;
			cache.put("marker", Integer.toString(lines.size()));
			cache.store(new FileOutputStream(markerFile), "");
		    }
		}
		this.lastUpdate = System.currentTimeMillis();
		Thread.sleep(getUpdateDelay());
	    }
	    if (engine != null)
		engine.log("Chat Tailer for "+this.serverName+" has stopped!", "AWeSomeChat");
	}
	catch (InterruptedException e) { this.isRunning = false; }
	catch (IOException | NumberFormatException e) {
	    this.isRunning = false;
	    this.lastException = e;
	    this.lastExceptionTime = System.currentTimeMillis();
	    if (engine == null)
		e.printStackTrace();
	    else {
		engine.log("Couldn't continue tailing!", "AWeSomeChat");
		engine.logError(e, "AWeSomeChat");
	    }
	}
    }
    
    public void parseLine(String line) {
	//System.err.println(line);
	if (line.length() == 0) { return; }
	
	Date d = new Date();
	d.setTime(this.tailFile.lastModified()); // We use lastModified so the date is never in the future.
	DateFormat df = SimpleDateFormat.getDateInstance();
	String date = df.format(d);
	
	List<String> out = new ArrayList<String>();
	List<String> logLines = new ArrayList<String>();
	if (line.contains("[Server thread/INFO]: <")) { // Chat
	    String[] data = line.split(" ");
	    String user = data[3].replaceAll("[\\<\\>]", "");
	    String message = line.split(user+"> ")[1];
	    user = stripCodes(user);
	    if (!this.onlineUsers.contains(user)) {
		this.onlineUsers.add(user);
		saveOnline();
	    }
	    if (message.startsWith("P ") || message.startsWith("PRIVATE ")) { return; }
	    out.add((engine == null ? "%C2%%USER%%C1%: %MESSAGE%" : engine.config.getProperty("ascOutputFormat"))
		    .replaceAll("%USER(NAME)?%", user)
		    .replaceAll("%MESSAGE%", Matcher.quoteReplacement(message)));
	    logLines.add(data[0]+" "+user+": "+Matcher.quoteReplacement(message));
	}
	
	else if (line.contains("left the game") || line.contains("joined the game")) { // (dis)connects
	    String[] data = line.split(" ");
	    String type = data[4];
	    String user = stripCodes(data[3]);
	    
	    if (type.equals("joined"))
		onlineUsers.add(user);
	    else
		onlineUsers.remove(user);
	    
	    saveOnline();
	    
	    out.add((engine == null ? "%C2%%USER%%C1% %TYPE% the game." : engine.config.getProperty("ascOutputInOutFormat"))
		    .replaceAll("%TYPE%", type)
		    .replaceAll("%USER(NAME)?%", user));
	    logLines.add(data[0]+" "+user+" "+type+" the game.");
	}
	
	else if (line.contains("[Server thread/INFO]: *")) { // Actions
	    String[] data = line.split(" ");
	    String user = data[4];
	    String message = line.split(user+" ")[1];
	    user = stripCodes(user);
	    if (!this.onlineUsers.contains(user))
		this.onlineUsers.add(user);
	    
	    out.add((engine == null ? "%C2%* %USER%%C1% %ACTION%" : engine.config.getProperty("ascOutputActionFormat"))
		    .replaceAll("%ACTION%", message)
		    .replaceAll("%USER(NAME)?%", user));
	    logLines.add(data[0]+" * "+user+" "+message);
	}
	
	else if (line.contains("[Server thread/INFO]:") && onlineUsers.contains(stripCodes(line.split(" ")[3])) && !line.contains("lost connection") && line.contains("achievement")) { // Achievements
	    String[] data = line.split(" ");
	    String user = stripCodes(data[3]);
	    String achievementData = line.split("achievement \\[")[1];
	    String achievementName = achievementData.substring(0, achievementData.length() - 1);
	    out.add((engine == null ? "%C2%%USER%%C1% earned the achievement %C2%%ACHIEVEMENTNAME%%C1%!" : engine.config.getProperty("ascOutputAchievementFormat"))
		    .replaceAll("%ACHIEVEMENTNAME%", achievementName)
		    .replaceAll("%USER(NAME)?%", user));
	    logLines.add(data[0]+" "+user+" has earned the achievement "+achievementName);
	}
	
	else if (line.contains("[Server thread/INFO]:") && onlineUsers.contains(stripCodes(line.split(" ")[3])) && !line.contains("lost connection")) { // Deaths
	    String[] data = line.split(" ");
	    String user = data[3];
	    //System.out.println(user);
	    String deathMessage = line.split(user+" ")[1];
	    user = stripCodes(user);
	    
	    out.add((engine == null ? "%C2%%USER%%C1% %DEATHMESSAGE%" : engine.config.getProperty("ascOutputDeathFormat"))
		    .replaceAll("%DEATHMESSAGE%", deathMessage)
		    .replaceAll("%USER(NAME)?%", user));
	    logLines.add(data[0]+" "+user+" "+deathMessage);
	    Properties deaths = new Properties();
	    try {
		if (new File(this.cacheDir, "deaths.iuc").exists())
		    deaths.load(new FileInputStream(new File(this.cacheDir, "deaths.iuc")));
		int death = (deaths.containsKey(user) ? Integer.valueOf(deaths.getProperty(user)) : 0) + 1;
		out.add("%C2%"+user+"%C1% has died "+(death == 1 ? "for the first time!" : "%C2%"+death+"%C1% times!"));
		deaths.put(user, Integer.toString(death));
		deaths.store(new FileOutputStream(new File(this.cacheDir, "deaths.iuc")), "Death counter file");
		logLines.add(data[0]+" "+user+" has died "+(death == 1 ? "for the first time!" : +death+" times!"));
	    }
	    catch (IOException | NumberFormatException e) {
		if (engine == null)
		    e.printStackTrace();
		else {
		    this.lastException = e;
		    this.lastExceptionTime = System.currentTimeMillis();
		    engine.log("Couldn't load or save deaths file", "AWeSomeChat");
		    engine.logError(e, "AWeSomeChat", new File(this.cacheDir, "deaths.iuc").getAbsolutePath());
		    
		}
	    }
	}
	
	else if (line.contains("Stopping server") && !this.serverStopped) { //Server stop
	    String[] data = line.split(" ");
	    if (!this.onlineUsers.isEmpty()) {
		for (Iterator<String> it = this.onlineUsers.iterator(); it.hasNext();) {
		    String user = stripCodes(it.next());
		    out.add((engine == null ? "%C2%%USER%%C1% %TYPE% the game." : engine.config.getProperty("ascOutputInOutFormat"))
			    .replaceAll("%TYPE%", "left")
			    .replaceAll("%USER(NAME)?%", user));
		    logLines.add(data[0]+" "+user+" left the game.");
		}
		this.onlineUsers.clear();
		saveOnline();
	    }
	    out.add("Server stopping!");
	    this.serverStopped = true;
	}
	
	else if (line.contains("For help, type \"help\" or \"?\"") && this.serverStopped) { // Server started
	    this.serverStopped = false;
	    out.add("Server started!");
	}
	if (!out.isEmpty()) {
	    for (String outLine : out) {
//		try {
//		    outLine = new String(outLine.getBytes("UTF-8"));
//		} catch (UnsupportedEncodingException ex) {
//		    engine.log("Couldn't convert string to UTF-8", "AweSomeChat");
//		}
		if (engine == null)
		    System.err.println(("%C1%[%C2%AWeSome %SERVERNAME%%C1%]: "+outLine).replaceAll("%SERVER(NAME)?%", this.serverName).replaceAll("%C[12]+%", ""));
		else
		    engine.send("PRIVMSG #QuestHelp :"+(engine.config.getProperty("ascOutputPrefix")+outLine).replaceAll("%SERVER(NAME)?%", this.serverName));
	    }
	    
	    if (!logLines.isEmpty())
//		if (!(line.contains("Stopping server") || line.contains("For help, type \"help\" or \"?\"")))
//		    writeToLog(line);
//		else
		for (Iterator<String> it = logLines.iterator(); it.hasNext();)
		    writeToLog("["+date+"] "+it.next());
	    
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
		w.write(it.next()+"\n");
	    w.close();
	} catch (IOException e) {
	    this.lastException = e;
	    this.lastExceptionTime = System.currentTimeMillis();
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
	    this.lastException = ex;
	    this.lastExceptionTime = System.currentTimeMillis();
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
	    this.lastException = ex;
	    this.lastExceptionTime = System.currentTimeMillis();
	    if (engine == null)
		ex.printStackTrace();
	    else {
		engine.log("Unable to add parsed line to log." , "AWeSomeChat");
		engine.logError(ex, "AWeSomeChat", line, new File(this.cacheDir, "chat.txt").getAbsolutePath());
	    }
	}
    }
    
    @Override
    public long getUpdateDelay() {
	return (engine == null ? 500 : Long.valueOf(engine.config.getProperty("ascUpdateDelay")));
    }
    
    @Override
    public long timeTilUpdate() {
	return (this.lastUpdate + this.getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public boolean isDead() {
	return this.timeTilUpdate() < 0;
    }
    
    @Override
    public void update() throws IOException, ParserConfigurationException, SAXException {
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
    public long getStartupTime() {
	return this.startupTime;
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
    public String getThreadName() {
	return this.thread.getName();
    }
    
    public String stripCodes(String a) {
	return a.replaceAll("Â?§[0-9a-z]", "");
    }
    
}
