package com.simple.ipeer.iutil2.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import com.simple.ipeer.iutil2.irc.Server;
import com.simple.ipeer.iutil2.irc.protocol.Protocol;

public class Main implements Runnable {

	private static final File DEFAULT_CONFIG_DIR = new File("./config");
	private static final Server DEFAULT_SERVER = new Server("irc.swiftirc.net", false, 6667);
	private static final String DEFAULT_NICK = "iUtil";
	private static final File DEFAULT_LOGS_DIR = new File("./logs");

	public Properties config = new Properties();
	public Server server = DEFAULT_SERVER;

	public File configDir;
	public File configFile;
	public File logDir;
	public File logFile;
	public FileWriter logWriter;
	public String CURRENT_NICK;

	private Thread engineThread;
	//private Main engine;
	private boolean engineRunning = false;
	
	private Socket connection;
	private BufferedReader in;
	private BufferedWriter out;
	private String CURRENT_SERVER;
	private String CURRENT_NETWORK;
	private HashMap<String, String> NETWORK_SETTINGS;
	private HashMap<String, Channel> CHANNEL_LIST;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap<String, String> startArgs = new HashMap<String, String>();
		for (String a : args) {
			String[] c = a.split("=");
			startArgs.put(c[0].replace("-", ""), c[1]);
		}

		// Start the bot
		new Main(startArgs);

	}

	public Main(HashMap<String, String> args) {

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

		try {
			logWriter = new FileWriter(logFile, true);
		}
		catch (IOException e) {
			System.err.println("Couldn't create logging object, cannot continue!");
			e.printStackTrace();
			System.exit(1);
		}

		// Now that that stuff is done, load the config.
		loadConfig();

		// Now we change the config should anything differ in the command line.

		if (args.containsKey("server") && config.getProperty("server") != args.get("server"))
			config.put("server", args.get("server"));

		if (args.containsKey("port") && config.getProperty("port") != args.get("port"))
			config.put("port", args.get("port"));

		if (args.containsKey("ssl") && config.getProperty("ssl") != args.get("ssl"))
			config.put("ssl", args.get("ssl"));
		if (args.containsKey("debug"))
			config.put("debug", args.get("debug"));
		
		// Now everything should be okay and we can start the bot...
		
		this.engineThread = new Thread(this, "Main iUtil 2 Thread");
		//this.engine = this;
		// Oh god.
		start();

	}

	public void log(String line) {
		log(line, "DEBUG");
	}
	
	public void log(String line, String type) {
		String time = (new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date(System.currentTimeMillis()));
		String out = time+" ["+type+"] "+line;
		if (Boolean.valueOf(config.getProperty("debug")))
				System.err.println(out.replaceAll("(\\r\\n|\\n\\r)", ""));
		try {
			logWriter.write(out+"\r\n");
			logWriter.flush();
		} 
		catch (IOException e) {
			System.err.println("Cannot write to log file!");
			e.printStackTrace();
		}

	}

	private void loadConfig() {
		log("Attempting to load config from "+configFile.getAbsolutePath()+"...");
		if (config == null)
			config = new Properties();
		if (!configFile.exists()) {
			log("Config file not found, creating default settings...");
			// Basic, default settings
			config.put("server", DEFAULT_SERVER.getAddress());
			config.put("port", Integer.toString(DEFAULT_SERVER.getPort()));
			config.put("ssl", Boolean.toString(DEFAULT_SERVER.isSSL()));
			saveConfig();
		}
		else { 
			try {
				config.load(new FileInputStream(configFile));
			} 
			catch (Exception e) {
				log("Couldn't load options from file "+configFile.getAbsolutePath()+"!");
				e.printStackTrace();
			}
		}
		log("Config loaded succesfully.");
	}

	public void saveConfig() {
		log("Attempting to save config to "+configFile.getAbsolutePath()+"...");
		try {
			config.store(new FileOutputStream(configFile), "iUtil 2 main Config");
		} catch (Exception e) {
			log("Couldn't save config to "+configFile.getAbsolutePath()+"!");
			e.printStackTrace();
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
		log("Creating config entries...");
		int additions = 0;
		for (String k : c.keySet()) {
			if (config.containsKey(k) && check)
				log("Entry "+k+" already present in config. ("+config.getProperty(k)+")");
			else {
				log("Creating default config entry: "+k+" = "+c.get(k));
				config.put(k, c.get(k));
				additions++;
			}
		}
		if (additions > 0)
			saveConfig();
	}
	
	public void start() {
		this.engineRunning = true;
		this.engineThread.start();
	}

	@Override
	public void run() {
		
		HashMap<String, String> defaultConfig = new HashMap<String, String>();
		defaultConfig.put("connectModes", "+Bp-x");
		defaultConfig.put("commandCharacters", "#@.!");
		defaultConfig.put("publicCommandCharacters", "#@");
		createConfigDefaults(defaultConfig);
		
		connection = new Socket();
		
		// Connecting to IRC
		
		log("Attempting to connect to "+config.getProperty("server")+":"+config.getProperty("port")+"/"+config.getProperty("ssl")+"...");
		try {
			if (Boolean.valueOf(config.getProperty("ssl"))) {
				log("Creating SSL connection to server...");
				SSLContext ssl = SSLContext.getInstance("SSL");
				ssl.init(null, SSLUtils.trustAll, new SecureRandom());
				SSLSocketFactory sslsf = ssl.getSocketFactory();
				SSLSocket ssls = (SSLSocket)sslsf.createSocket(config.getProperty("server"), Integer.valueOf(config.getProperty("port")));
				in = new BufferedReader(new InputStreamReader(ssls.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(ssls.getOutputStream()));
				connection = ssls;
			}
			else {
				connection = new Socket(config.getProperty("server"), Integer.valueOf(config.getProperty("port")));
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
			}
		}
		catch (Exception | Error e) {
			log("FAILED to connect to server, cannot continue!");
			e.printStackTrace();
			System.exit(1);
		}
		log("Created server connection.");
		log(connection.toString());
		
		changeNick(config.getProperty("nick", DEFAULT_NICK));
		log("Registered nick with server, waiting for response...");
		
		// On response from the server, go into a loop until we get RAW 251.
		// If the server never sends this RAW, the bot will never exit this loop (possibly dangerous?)
		
		String line = "";
		try {
			// Connection data is parsed here, "regular" IRC traffic is parsed by Protocol.java
			while ((line = in.readLine()) != null && this.engineRunning) {
				log("<- "+line, "IRC");
				if (line.indexOf("001") >= 0) // Server address
					this.CURRENT_SERVER = line.split(" ")[0].substring(1);
				
				else if (line.indexOf("005") >= 0) {	// Network settings	
					if (NETWORK_SETTINGS == null)
						NETWORK_SETTINGS = new HashMap<String, String>();
					String[] a = line.split(" ");
					for (int x = 3; x < a.length; x++) {
						if (a[x].contains("=")) {
							String[] b = a[x].split("=");
							NETWORK_SETTINGS.put(b[0], b[1]);
						}
					}
				}
				
				else if (line.indexOf("004") >= 0) {
					//TODO: Identify with the server
					send("MODE "+CURRENT_NICK+" "+config.getProperty("connectModes"));
				}
				
				else if (line.indexOf("433") >= 0) {
					log("Nick is in use, trying "+CURRENT_NICK+"2");
					String newNick = "";
					try {
						int n = Integer.valueOf(CURRENT_NICK.substring(CURRENT_NICK.length() - 1));
						newNick = CURRENT_NICK.substring(0, CURRENT_NICK.length() - 1)+n++;
					}
					catch (NumberFormatException e) {
						newNick = CURRENT_NICK+"2";
					}
					changeNick(newNick);
				}
				
				else if (line.indexOf("251") >= 0) {
					CURRENT_NETWORK = NETWORK_SETTINGS.get("NETWORK");
					if (CURRENT_NETWORK == null || CURRENT_NETWORK.equals(""))
						CURRENT_NETWORK = "UNKNOWN";
					log("Connected to the "+CURRENT_NETWORK+" network on server "+CURRENT_SERVER);
					break;
				}
				
			}
			
			//TODO: Temporary join of channels (which doesn't work).
			String[] chans = "#QuestHelp,#Peer.Dev,#AweSome".split(",");
			for (String a : chans)
				joinChannel(a);
			
			Protocol protocol = new Protocol();
			log("Protocol class is now handling incoming traffic.");
			log(protocol.toString());
			while ((line = in.readLine()) != null && this.engineRunning){
				protocol.parse(line, this);
			}
			
		}
		catch (IOException e) {
			log("Connection error, cannot continue!");
			e.printStackTrace();
			System.exit(1);			
		}
			
	}


	public void changeNick(String nick) {
		CURRENT_NICK = nick;
		send("NICK "+nick+"\r\n");
		send("USER "+nick+" ipeer.auron.co.uk "+nick+": iPeer's Java Utility Bot\r\n");
		
	}
	
	public void send(String data) {
		send(data, true);
	}
	
	public void joinChannel(String channel) {
		if (CHANNEL_LIST == null)
			CHANNEL_LIST = new HashMap<String, Channel>();
		send("JOIN "+channel);
		CHANNEL_LIST.put(channel.toLowerCase(), new Channel(channel.toLowerCase()));
		log("Now in "+CHANNEL_LIST.size()+" channels.");
		send("WHO "+channel);
	}
	
	public void send(String data, boolean flush) {
		try {
			if (!(data.endsWith("\r\n") || data.endsWith("\n\r")))
				data = data+"\r\n";
			out.write(data);
			if (flush)
				out.flush();
			if (Boolean.valueOf(config.getProperty("debug")))
					log("-> "+data.replaceAll("\\[rn]", ""), "IRC");
			
		} catch (IOException e) {
			log("Couldn't send data to socket!");
			e.printStackTrace();
		}
	}

	public HashMap<String, Channel> getChannelList() {
		return CHANNEL_LIST;
	}


}
