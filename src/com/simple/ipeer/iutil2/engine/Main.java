package com.simple.ipeer.iutil2.engine;

import com.simple.ipeer.iutil2.commands.base.CommandManager;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.Key;
import java.security.SecureRandom;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.simple.ipeer.iutil2.console.Console;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import com.simple.ipeer.iutil2.irc.ial.IAL;
import com.simple.ipeer.iutil2.irc.protocol.Protocol;
import com.simple.ipeer.iutil2.minecraft.AWeSomeChat;
import com.simple.ipeer.iutil2.minecraft.AWeSomeStatus;
import com.simple.ipeer.iutil2.minecraft.servicestatus.MinecraftServiceStatus;
import com.simple.ipeer.iutil2.profiler.Profiler;
import com.simple.ipeer.iutil2.reddit.GameDeals;
import com.simple.ipeer.iutil2.tell.Tell;
import com.simple.ipeer.iutil2.twitch.Twitch;
import com.simple.ipeer.iutil2.twitter.Twitter;
import com.simple.ipeer.iutil2.util.Filesize;
import com.simple.ipeer.iutil2.util.Util;
import com.simple.ipeer.iutil2.youtube.YouTube;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main implements Runnable {
    
    public final String BOT_VERSION = "799";
    
    private static final File DEFAULT_CONFIG_DIR = new File("./config");
    //private static final Server DEFAULT_SERVER = new Server("127.0.0.1", false, 6667);
    private static final String DEFAULT_NICK = "iUtil";
    private static final File DEFAULT_LOGS_DIR = new File("./logs");
    
    public static final char COLOUR = 0x03;
    public static final char BOLD = 0x02;
    public static final char UNDERLINE = 0x1F;
    public static final char ITALICS = 0x1D;
    public static final char HIGHLIGHT = 0x16;
    public static final char ENDALL = 0xF;
    public static final char DASH = 8212;
    
    public static PreparedStatement statusUpdateTimeQ;
    
    public Properties config = new Properties();
    public File configDir;
    public File configFile;
    public File logDir;
    public File logFile;
    public File errorFile;
    public FileWriter logWriter;
    public FileWriter errorWriter;
    public Boolean REQUESTED_QUIT = false;
    public String CURRENT_SERVER;
    public String CURRENT_NETWORK;
    public HashMap<String, String> NETWORK_SETTINGS;
    public HashMap<String, AnnouncerHandler> announcers;
    public Debugger debugger;
    
    private Thread engineThread;
    private boolean engineRunning = false;
    
    private Socket connection;
    private BufferedReader in;
    private BufferedWriter out;
    private boolean SERVER_REGISTERED = false;
    private boolean textFormatting = true;
    private int connectionRetries = 0;
    private static Main engine;
    private Profiler profiler;
    private long bytesSent = 0L;
    private long bytesReceived = 0L;
    private Protocol protocol;
    
    public List<OfflineMessage> offlineMessages = new ArrayList<OfflineMessage>();
    private Console console;
    private Tell tell;
    private IAL ial;
    private CommandManager commandManager;
    private Twitter twitter;
    private Connection sqlConnection;
    private Connection mcSQLConnection;
    private HashMap<String, String> cmdLine;
    private boolean creatingSQLConnection = false;
    private long lastSQLReconnectTry = 0L;
    
    
    public static void main(String[] args) {
	HashMap<String, String> startArgs = new HashMap<String, String>();
	for (String a : args) {
	    String[] c = {};
	    try {
		c = a.split("=");
		startArgs.put(c[0].replace("-", ""), c[1]);
	    }
	    catch (ArrayIndexOutOfBoundsException e) {
		System.err.println("Invalid command line parameter passed '"+c[0]+"', ignoring.");
		//continue;
	    }
	}
	
	// Start the bot
	new Main(startArgs);
	
    }
    
    public Main(HashMap<String, String> args) {
	
	engine = this;
	
	this.cmdLine = args;
	
	// Handle config files and stuff, creating directories should they not exist.
	//		if (!DEFAULT_CONFIG_DIR.exists())
	//			DEFAULT_CONFIG_DIR.mkdirs();
	try {
	    configDir = new File(args.get("configDir"));
	}
	catch (Exception | Error e) {
	    configDir = DEFAULT_CONFIG_DIR;
	}
	if (!configDir.exists())
	    configDir.mkdirs();
	configFile = new File(configDir, "config.cfg");
	
	// Once logging is created, we can actually log stuff.
	// Can't actually change where this file is because there's no real point.
	
	try {
	    logDir = new File(args.get("logDir"));
	}
	catch (Exception | Error e) {
	    logDir = DEFAULT_LOGS_DIR;
	}
	if (!logDir.exists())
	    logDir.mkdirs();
	logFile = new File(logDir, "main.log");
	errorFile = new File(logDir, "error.log");
	try {
	    logWriter = new FileWriter(logFile, true);
	    errorWriter = new FileWriter(errorFile, true);
	}
	catch (IOException e) {
	    System.err.println("Couldn't create logging object, cannot continue!");
	    logError(e);
	    System.exit(1);
	}
	
	// Now that that stuff is done, load the config.
	loadConfig();
	
	String cmdLine = "Command line args:";
	for (String k : args.keySet())
	    cmdLine += " -"+k+"="+args.get(k);
	log(cmdLine);
	// Now we change the config should anything differ in the command line.
	
	if (args.containsKey("server") && !config.getProperty("server").equals(args.get("server")))
	    config.put("server", args.get("server"));
	
	if (args.containsKey("port") && !config.getProperty("port").equals(args.get("port")))
	    config.put("port", args.get("port"));
	
	if (args.containsKey("ssl") && !config.getProperty("ssl").equals(args.get("ssl")))
	    config.put("ssl", args.get("ssl"));
	if (args.containsKey("debug"))
	    config.put("debug", args.get("debug"));
	if (config.get("debug").equals("true"))
	    log("Working directory is "+System.getProperty("user.dir"));
	
	// Update existing config files with new settings.
	
	HashMap<String, String> defaultConfig = new HashMap<String, String>();
	defaultConfig.put("connectModes", "+Bp-x");
	defaultConfig.put("commandCharacters", "#@.!");
	defaultConfig.put("publicCommandCharacters", "#@");
	defaultConfig.put("quitMessageFormat", "QUIT command from %NICK%");
	defaultConfig.put("partMessageFormat", "PART command from %NICK%");
	defaultConfig.put("identificationString", "PRIVMSG NickServ :IDENTIFY %PASSWORD%");
	defaultConfig.put("autoJoin", "#QuestHelp,#Peer.Dev,#AWeSome");
	defaultConfig.put("colour1", "14");
	defaultConfig.put("colour2", "13");
	defaultConfig.put("noAMSG", "#Peer.Dev");
	defaultConfig.put("maxConnectionRetries", "5");
	defaultConfig.put("reconnectDelay", "5000");
	defaultConfig.put("debugChannel", "#peer.dev");
	defaultConfig.put("profilingEnabled", "true");
	defaultConfig.put("apiDataDir", "./api");
	defaultConfig.put("sqlHostPort", "localhost:5432");
	createConfigDefaults(defaultConfig);
	
	new File(config.getProperty("apiDataDir")).mkdirs();
	
	// Create the SQL Database connection
	createSQLConnection();
	
	// Announcers and other threads that should run while the bot is
	
	announcers = new HashMap<String, AnnouncerHandler>();
	announcers.put("YouTube", new YouTube(this));
	announcers.put("Twitch", new Twitch(this));
	announcers.put("AWeSome Status", new AWeSomeStatus(this));
	announcers.put("AWeSome Chat", new AWeSomeChat(this));
	announcers.put("Minecraft Service Status", new MinecraftServiceStatus(this));
	announcers.put("/r/GameDeals Announcer", new GameDeals(this));
	tell = new Tell(this);
	console = new Console(this);
	profiler = new Profiler(this);
	ial = new IAL(this);
	debugger = new Debugger(this);
	commandManager = new CommandManager(this);
	twitter = new Twitter(this);
	
	// Load any offline messages, if they exist.
	
	if (new File("offlinemessages.uic").exists())
	    loadOfflineMessages();
	
	// Now everything should be okay and we can start the bot...
	
	this.engineThread = new Thread(this, "iUtil 2 Main Thread");
	
	// Oh god.
	start();
	
    }
    
    public void reconnect() {
	this.connectionRetries++;
	if (this.connectionRetries > Integer.valueOf(config.getProperty("maxConnectionRetries"))) {
	    log("Reached maximum number of connection retries, terminating.");
	    System.exit(0);
	}
	try {
	    log("Retrying connection in "+Long.valueOf(config.getProperty("reconnectDelay")) / 1000L+" seconds.");
	    Thread.sleep(Long.valueOf(config.getProperty("reconnectDelay")));
	} catch (Exception e) {
	    log("Reconnect thread failed to sleep, terminating for safety purposes.");
	    logError(e);
	    System.exit(1);
	}
	finally {
	    log("Connection rety #"+this.connectionRetries);
	    this.SERVER_REGISTERED = false;
	    this.engineThread.interrupt();
	    this.engineRunning = false;
	    this.engineThread = new Thread(this, "iUtil 2 Main Thread");
	    start();
	}
    }
    
    public void log(String line, Class clazz) {
	Logger.getLogger(clazz.getName()).log(Level.INFO, line);
    }
    
    public void log(String line) {
	log(line, "DEBUG");
    }
    
    public void log(String line, String type, LogLevel level) {
	if (level == LogLevel.NEVER || level == LogLevel.NONE) { return; }
	String time = (new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date(System.currentTimeMillis()));
	String logOut = time+" ["+type+"] "+line;
	if ((level == LogLevel.DEBUG_ONLY || level == LogLevel.LOG_AND_DEBUG || level == LogLevel.ALL || level == LogLevel.LOG_DEBUG_AND_CHANNEL) && config.getProperty("debug").equals("true")) {
	    System.out.println(logOut);
	}
	if (level == LogLevel.LOG_DEBUG_AND_CHANNEL && config.getProperty("enableChannelLogging", "true").equals("true") && isConnected() && getIAL().amIOnChannel("#peer.dev")) {
		    send("PRIVMSG #Peer.Dev :[LOG] ("+type+"): "+line);
	}
	if (level == LogLevel.LOG_AND_DEBUG || level == LogLevel.LOG_ONLY || level == LogLevel.ALL || level == LogLevel.LOG_DEBUG_AND_CHANNEL) {
	    try {
		logWriter.write(logOut.replaceAll("(\\r\\n|\\n\\r)", "")+"\r\n");
		logWriter.flush();
	    }
	    catch (IOException e) {
		System.err.println("Cannot write to log file!");
		logError(e);
	    }
	}
    }
    
    public void log(String line, String type) {
	log(line, type, LogLevel.LOG_AND_DEBUG);
    }
    
    public void logException(Throwable e) {
	logError(e, "GENERAL");
    }
    
    public void logException(Throwable e, String type) {
	logError(e, type);
    }
    
    public void logError(Throwable e) {
	logError(e, "GENERAL");
    }
    
    public void logError(Throwable e, String type) {
	logError(e, type, new String[0]);
    }
    
    public void logError(Throwable e, String type, String... extraData) {
	String time = (new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date(System.currentTimeMillis()));
	//String out = time+" ["+type+"] "+;
	List<String> outLines = new ArrayList<String>();
	outLines.add("---- Caught "+type+" exception at "+time.toUpperCase()+" ----\n");
	if (extraData.length > 0) {
	    int x = 0;
	    for (String a : extraData)
		outLines.add("extraData["+x+++"]: "+a+"\n");
	}
	outLines.add(e.toString());
	for (StackTraceElement a : e.getStackTrace())
	    outLines.add("        at "+a.toString());
	outLines.add("\n---- End of stack trace ----\n\n");
	if (config.containsKey("debug") && config.getProperty("debug").equals("true"))
	    e.printStackTrace();
	try {
	    for (String a : outLines)
		errorWriter.write(a+"\r\n");
	    errorWriter.flush();
	}
	catch (IOException e1) {
	    System.err.println("Cannot write to log file!");
	    e1.printStackTrace(); // Probably the only error that should ever be printed like this (unless we're in debug mode, of course)
	}
	
    }
    
    public void loadConfig() {
	//log("Attempting to load config from "+configFile.getAbsolutePath()+"...");
	if (config == null)
	    config = new Properties();
	if (!configFile.exists()) {
	    log("Config file not found, creating default settings...");
	    // Basic, default settings
	    config.put("server", "127.0.0.1");
	    config.put("port", 6667);
	    config.put("ssl", false);
	    saveConfig();
	}
	else {
	    try {
		config.load(new FileInputStream(configFile));
	    }
	    catch (Exception e) {
		log("Couldn't load options from file "+configFile.getAbsolutePath()+"!");
		logError(e);
	    }
	}
	log("Config loaded succesfully.");
    }
    
    public void saveConfig() {
	log("Attempting to save config to "+configFile.getAbsolutePath()+"...");
	try {
	    config.store(new FileOutputStream(configFile), "iUtil 2 Main Config");
	    log("Config succesfully saved.");
	} catch (Exception e) {
	    log("Couldn't save config to "+configFile.getAbsolutePath()+"!");
	    logError(e);
	}
	
    }
    
    @SuppressWarnings("unused")
    private void createConfigDefaults(String string, String string2) {
	createConfigDefaults(string, string2, true);
    }
    
    private void createConfigDefaults(String string, String string2, boolean check) {
	HashMap<String, String> t = new HashMap<String, String>();
	t.put(string, string2);
	createConfigDefaults(t, check);
    }
    
    public void createConfigDefaults(HashMap<String, String> c) {
	createConfigDefaults(c, true);
    }
    
    public void createConfigDefaults(HashMap<String, String> c, boolean check) {
	//log("Creating config entries...");
	int additions = 0;
	for (String k : c.keySet()) {
	    //			if (config.containsKey(k) && check)
	    //				log("Entry "+k+" already present in config. ("+config.getProperty(k)+")");
	    //			else {
	    if (!config.containsKey(k) || !check) {
		log("Creating default config entry: "+k+" = "+c.get(k));
		config.put(k, c.get(k));
		additions++;
	    }
	}
	c.clear(); // Helps save a wee bit of memory.
	if (additions > 0)
	    saveConfig();
    }
    
    public void start() {
	this.engineRunning = true;
	this.engineThread.start();
    }
    
    @Override
    public void run() {
	
	console.startIfNotRunning();
	
	setConnection(new Socket());
	
	// Connecting to IRC
	
	log("Attempting to connect to "+config.getProperty("server")+":"+config.getProperty("port")+(config.getProperty("ssl").equals("true") ? " (SSL)" : "")+"...");
	try {
	    if (Boolean.valueOf(config.getProperty("ssl"))) {
		log("Creating SSL connection to server...");
		SSLContext ssl = SSLContext.getInstance("SSL");
		ssl.init(null, SSLUtils.trustAll, new SecureRandom());
		SSLSocketFactory sslsf = ssl.getSocketFactory();
		SSLSocket ssls = (SSLSocket)sslsf.createSocket(config.getProperty("server"), Integer.valueOf(config.getProperty("port")));
		in = new BufferedReader(new InputStreamReader(ssls.getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(ssls.getOutputStream()));
		setConnection(ssls);
	    }
	    else {
		setConnection(new Socket(config.getProperty("server"), Integer.valueOf(config.getProperty("port"))));
		in = new BufferedReader(new InputStreamReader(getConnection().getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(getConnection().getOutputStream()));
	    }
	}
	catch (Exception | Error e) {
	    log("Lost connection to server: "+e.getLocalizedMessage());
	    reconnect();
	}
	/*		catch (Exception | Error e) {
	* log("FAILED to connect to server, cannot continue!");
	* logError(e);
	* System.exit(1);
	* }*/
	if (isConnected()) {
	    this.connectionRetries = 0;
	    log("Connected to server "+config.getProperty("server")+":"+config.getProperty("port")+".");
	    log(getConnection().toString());
	    
	    changeNick(config.getProperty("nick", DEFAULT_NICK));
	    log("Registered nick with server, waiting for response...");
	    
	    String line = "";
	    try {
		this.protocol = new Protocol();
		ial.clearIAL();
		while ((line = in.readLine()) != null && this.engineRunning && !this.engineThread.isInterrupted()) {
		    getProfiler().startSection("Incoming");
		    addReceivedBytes(line.getBytes().length);
		    try {
			this.protocol.parse(line, this);
		    }
		    catch (Throwable e) {
			log("Encountered a fatal error!");
			logError(e, "General", line);
			if (isConnected())
			    send("QUIT :FATAL: "+e.toString()+" at "+e.getStackTrace()[0], false);
		    }
		    try { getProfiler().end(); } catch (Exception e) { }
		}
		
	    }
	    catch (SocketException | SSLException e) { log("Server disconnected unexpectedly, attempting to reconnect."); reconnect(); }
	    catch (Exception e) {
		log("Connection error, cannot continue!");
		logError(e);
		System.exit(1);
	    }
	}
	
    }
    
    public void sendQueuedMessages() {
	Iterator<OfflineMessage> it = this.offlineMessages.iterator();
	while (it.hasNext()) {
	    OfflineMessage m = it.next();
	    if (m.shouldLog())
		log("Sending queued message: "+m.getText());
	    send(m.getText(), m.shouldLog(), m.shouldSendWhenNotConnected());
	    it.remove();
	}
    }
    
    public void changeNick(String nick) {
	getIAL().processNickChange(getIAL().getCurrentNick(), nick);
	getIAL().setCurrentNick(nick);
	send("NICK "+nick+"\r\n", true, true);
	if (!SERVER_REGISTERED) {
	    send("USER "+nick+" ipeer.auron.co.uk "+nick+": iPeer's Java Utility Bot\r\n", true, true);
	    SERVER_REGISTERED = true;
	}
	
    }
    
    public void joinChannel(String channel) {
	send("JOIN "+channel);
    }
    
    public void send(String prefix, List<String> data, boolean log, boolean sinc) {
	for (String a : data)
	    send(prefix+" :"+a, log, sinc);
    }
    
    public void send(String data) {
	send(data, true);
    }
    
    public void send(String data, boolean log) {
	send(data, log, false);
    }
    
    public void send(String data, boolean log, boolean sendIfNotConnected) {
	engine.getProfiler().start("Outgoing");
	try {
	    if (!isConnected() && !sendIfNotConnected) {
		log("Tried to send message while offline, queuing for sending when (if) we reconnect.");
		this.offlineMessages.add(new OfflineMessage(data, log, sendIfNotConnected));
		saveOfflineMessages();
		engine.getProfiler().end();
		return;
	    }
	    
	    if (!(data.endsWith("\r\n") || data.endsWith("\n\r")))
		data = data+"\r\n";
	    if (this.textFormatting) {
		data = data
			.replaceAll("%C1%", Main.COLOUR+(String.format("%02d", Integer.valueOf(config.getProperty("colour1")))))
			.replaceAll("%C2%", Main.COLOUR+(String.format("%02d", Integer.valueOf(config.getProperty("colour2")))))
			.replaceAll("%C%", String.valueOf(Main.COLOUR))
			.replaceAll("%B%", String.valueOf(Main.BOLD))
			.replaceAll("%I%", String.valueOf(Main.ITALICS))
			.replaceAll("%U%", String.valueOf(Main.UNDERLINE))
			.replaceAll("%[HR]%", String.valueOf(Main.HIGHLIGHT))
			.replaceAll("%E%", String.valueOf(Main.ENDALL))
			.replaceAll("%DASH%", String.valueOf(Main.DASH));
		Matcher s = Pattern.compile("(%K[0-9]{1,2}(,[0-9]{1,2})?%)").matcher(data); // Custom colours, yay
		while (s.find()) {
		    String colours = s.group().split("%K|%")[1];
		    for (String a : colours.split(","))
			if (a.length() == 1)
			    colours = colours.replaceAll(a, String.format("%02d", Integer.valueOf(a)));
		    data = data.replaceAll(s.group(), Main.COLOUR+colours);
		}
	    }
	    addSentBytes(data.getBytes().length);
	    out.write(data);
	    out.flush();
	    if (log)
		log("-> "+data.replaceAll("\\[rn]", ""), "IRC");
	    
	} catch (IOException e) {
	    log("Couldn't send data to socket!");
	    logError(e);
	    if (e.getMessage().equals("Connection closed by remote host")) {
		log("Storing the data that was attempted to be sent to be sent once (if) we reconnect...");
		this.offlineMessages.add(new OfflineMessage(data, log, sendIfNotConnected));
		saveOfflineMessages();
		log("Server disconnected us but left the connection open. Disconnecting and attempting to reestablish a connection.");
		REQUESTED_QUIT = false;
		this.protocol.handleDisconnect(this, "Connection was closed unexpectedly.");
	    }
	    else if (e.getMessage().equals("Unrecognized SSL message, plaintext connection?")) {
		log("We appear to be using an SSL connection on a non-SSL port, let me fix that.");
		config.put("ssl", "false");
		REQUESTED_QUIT = false;
		this.protocol.handleDisconnect(this, "Current connection does not support SSL.");
	    }
	    else {
		REQUESTED_QUIT = false;
		this.protocol.handleDisconnect(this, e.getMessage());
	    }
	}
	engine.getProfiler().end();
    }
    
    public void quit(String quitMessage) {
	quit(quitMessage, false);
    }
    
    public void quit(String quitMessage, boolean reconnect) {
	if (!reconnect)
	    REQUESTED_QUIT = true;
	send("QUIT :"+quitMessage);
    }
    
    public char[] readPassword() {
	try {
	    log("Attempting to read password from file...");
	    File b = new File(configDir, "key");
	    File c = new File(configDir, "password");
	    DataInputStream f = new DataInputStream(new FileInputStream(b));
	    int x = f.readInt();
	    byte[] key = new byte[x];
	    f.readFully(key);
	    Key k = new SecretKeySpec(key, "AES");
	    f.close();
	    f = new DataInputStream(new FileInputStream(c));
	    x = f.readInt();
	    byte[] pass = new byte[x];
	    f.readFully(pass);
	    f.close();
	    Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    aes.init(Cipher.DECRYPT_MODE, k);
	    log("Succesfully read password from file");
	    return new String(aes.doFinal(pass)).toCharArray();
	} catch (Exception e) {
	    log("Couldn't read password from file!");
	    //logError(e);
	    throw new RuntimeException(e.toString());
	}
    }
    
    public void partChannel(String channel, String message) {
	if (!message.equals(""))
	    send("PART "+channel+" :"+message);
	else
	    send("PART "+channel);
    }
    
    public void amsg(String msg) {
	for (String c : getIAL().getChannelList().keySet())
	    if (!config.getProperty("noAMSG").toLowerCase().contains(c))
		send("PRIVMSG "+c+" :"+msg);
	
    }
    
    public HashMap<String, AnnouncerHandler> getAnnouncers() {
	return this.announcers;
    }
    
    public void disableFormatProcessing() {
	this.textFormatting = false;
    }
    
    public void enableFormatProcessing() {
	this.textFormatting = true;
    }
    
    public Socket getConnection() {
	return connection;
    }
    
    public void setConnection(Socket connection) {
	this.connection = connection;
    }
    
    public static Main getEngine() {
	return engine;
    }
    
    public static Main getMain() {
	return getEngine();
    }
    
    public boolean profilingEnabled() {
	return engine == null || config.getProperty("profilingEnabled").equals("true");
    }
    
    public Profiler getProfiler() {
	return this.profiler;
    }
    
    public boolean isConnected() {
	return (getConnection() != null && getConnection().isConnected());
    }
    
    public void addSentBytes(long bytes) {
	this.bytesSent += bytes;
    }
    
    public void addReceivedBytes(long bytes) {
	this.bytesReceived += bytes;
    }
    
    public long getBytesSent() {
	return this.bytesSent;
    }
    
    public long getBytesReceived() {
	return this.bytesReceived;
    }
    
    public long[] getBandwidth() {
	long[] a = {this.bytesSent, this.bytesReceived};
	return a;
    }
    
    public Tell getTell() {
	return this.tell;
    }
    
    public IAL getIAL() {
	return this.ial;
    }
    
    public Debugger getDebugger() {
	return this.debugger;
    }
    
    public List<String> generateInfoOutput() {
	long totalMemory = Runtime.getRuntime().totalMemory();
	long freeMemory = Runtime.getRuntime().freeMemory();
	long usedMemory = totalMemory - freeMemory;
	List<String> out = new ArrayList<String>();
	out.add("iUtil Build #"+BOT_VERSION);
	out.add("Memory: "+(usedMemory / 1024L / 1024L)+"MB/"+(totalMemory / 1024L / 1024L)+"MB");
	String threads = "";
	for (String a : engine.getAnnouncers().keySet()) {
	    String time = "%B%stopped%B%";
	    AnnouncerHandler ah = engine.getAnnouncers().get(a);
	    int deadThreads = ah.getDeadThreads();
	    for (Announcer an : ah.getAnnouncerList()) { // Loop through to the first alive thread (if any)
		if (!an.isDead()) {
		    int ttu = (int)(an.timeTilUpdate() / 1000L);
		    int seconds = ttu % 60;
		    int minutes = (int)ttu / 60;
		    int hours = (int)(Math.floor(minutes / 60));
		    if (hours > 0)
			minutes -= hours*60;
		    time = (hours > 0 ? String.format("%02d", hours)+":" : "")+String.format("%02d", minutes)+":"+String.format("%02d", seconds);
		}
	    }
	    threads = threads+(threads.length() > 0 ? ", " : "")+a+": "+time+" ("+deadThreads+" of "+ah.getTotalThreads()+" dead)";
	}
	out.add("Announcers (updating in): "+threads);
	out.add("Java: "+System.getProperty("sun.arch.data.model")+"-bit "+System.getProperty("java.version")+", C: "+System.getProperty("java.class.version")+" VM: "+System.getProperty("java.vm.version")+" / "+System.getProperty("java.vm.specification.version"));
	out.add("OS: "+System.getProperty("os.name")+" / "+System.getProperty("os.version"));
	out.add("Connection: "+engine.getConnection().toString());
	out.add("IRC Traffic (estimate): Sent: "+Filesize.calculate(engine.getBytesSent())+", Received: "+Filesize.calculate(engine.getBytesReceived()));
	return out;
    }
    
    public CommandManager getCommandManager() {
	return this.commandManager;
    }
    
    public void sendCommandHelpReply(ICommandSender sender, String sendPrefix, String line) {
	sendCommandReply(sender, sendPrefix, String.format(line, (sender instanceof Console ? "help" : ".help")));
    }
    
    public void sendCommandReply(ICommandSender sender, String sendPrefix, String line) {
	if (sender instanceof Console) {
	    Pattern p = Pattern.compile("%([BUIRO]|C[1-2]|K[0-9](,[0-9])?)%");
	    Matcher m = p.matcher(line);
	    line = m.replaceAll("");
	    System.err.println(line);
	}
	else {
	    send(sendPrefix+" :"+line);
	}
	
    }
    
    public void terminate() {
	try {
	    getSQLConnection().close();
	} catch (SQLException ex) { }
	System.exit(0);
    }
    
    public Protocol getProtocol() {
	return this.protocol;
    }
    
    public Twitter getTwitter() {
	return this.twitter;
    }
    
    public void saveOfflineMessages() {
	try {
	    File f = null;
	    if ((f = new File("offlinemessages.iuc")).exists())
		f.delete();
	    FileWriter w = new FileWriter("offlinemessages.iuc", false);
	    for (OfflineMessage om : this.offlineMessages)
		w.write(om.getText()+"\0x01"+Boolean.toString(om.shouldLog())+"\0x01"+Boolean.toString(om.shouldSendWhenNotConnected())+"\n");
	} catch (IOException ex) {
	    log("Couldn't save offline messages to file.");
	    logError(ex);
	}
    }
    
    public void loadOfflineMessages() {
	Scanner s = new Scanner("offlinemessages.iuc");
	while (s.hasNext()) {
	    String[] a = s.nextLine().split("\0x01");
	    OfflineMessage om = new OfflineMessage(a[0], Boolean.parseBoolean(a[1]), Boolean.parseBoolean(a[2]));
	    this.offlineMessages.add(om);
	}
	s.close();
	new File("offlinemessage.iuc").delete();
    }
    
    public void setSQLConnection(Connection c) {
	setSQLConnection("", c);
    }
    
    public void setSQLConnection(String s, Connection c) {
	if (s.equals("mc"))
	    this.mcSQLConnection = c;
	else
	    this.sqlConnection = c;
    }
    
    public Connection getSQLConnection() {
	return this.sqlConnection;
    }
    
    public Connection getSQLConnection(String t) {
	if (t.equals("mc"))
	    return this.mcSQLConnection;
	else
	    return this.sqlConnection;
    }
    
    public void createSQLConnection() {
	if (!this.creatingSQLConnection && System.currentTimeMillis() - this.lastSQLReconnectTry >= 60000) {
	    this.creatingSQLConnection = true;
	    this.lastSQLReconnectTry = System.currentTimeMillis();
	    log("Creating SQL connections.", "SQL", LogLevel.LOG_DEBUG_AND_CHANNEL);
	    Properties p = new Properties();
	    p.put("user", "iPeer");
	    p.put("password", Util.readEncrypted(new File("./sql/pwd")));
	    p.put("logUnclosedConnections", "true");
	    p.put("tcpKeepAlive", "true");
	    //p.put("loglevel", "2");
//	if (cmdLine.get("sqlUseSSL").equals("true"))
//	    p.put("ssl", "true");
	    Connection c = null;
	    Connection mcC = null;
	    try {
		c = DriverManager.getConnection("jdbc:postgresql://"+config.getProperty("sqlHostPort", "localhost:5432")+"/iPeer", p);
		mcC = DriverManager.getConnection("jdbc:postgresql://"+config.getProperty("sqlHostPort", "localhost:5432")+"/postgres", p);
		log("Succesfully created SQL connections.", "SQL", LogLevel.LOG_DEBUG_AND_CHANNEL);
		statusUpdateTimeQ = c.prepareStatement("UPDATE update_times SET time = ? WHERE name = ?");
	    } catch (SQLException ex) {
		logError(ex, "SQL");
		log("Couldn't recreate SQL connections. Waiting at least 1 minute before trying again.", "SQL", LogLevel.LOG_DEBUG_AND_CHANNEL);
//		log("FATAL: Couldn't acquire connection to SQL server. Terminating.", "SQL", LogLevel.LOG_DEBUG_AND_CHANNEL);
//		System.exit(1);
		return;
	    }
	    setSQLConnection(c);
	    setSQLConnection("mc", mcC);
	    this.creatingSQLConnection = false;
	}
    }
}
