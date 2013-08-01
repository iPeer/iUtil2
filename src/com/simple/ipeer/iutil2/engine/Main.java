package com.simple.ipeer.iutil2.engine;

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

import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import com.simple.ipeer.iutil2.irc.Server;
import com.simple.ipeer.iutil2.irc.protocol.Protocol;
import com.simple.ipeer.iutil2.twitch.Twitch;
import com.simple.ipeer.iutil2.youtube.YouTube;

public class Main implements Runnable {

	public final String BOT_VERSION = "0.1";

	private static final File DEFAULT_CONFIG_DIR = new File("./config");
	private static final Server DEFAULT_SERVER = new Server("irc.swiftirc.net", false, 6667);
	private static final String DEFAULT_NICK = "iUtil";
	private static final File DEFAULT_LOGS_DIR = new File("./logs");

	public static final char COLOUR = 0x03;
	public static final char BOLD = 0x02;
	public static final char UNDERLINE = 0x1F;
	public static final char ITALICS = 0x1D;
	public static final char HIGHLIGHT = 0x16;
	public static final char ENDALL = 0xF;
	public static final char DASH = 8212;

	public Properties config = new Properties();
	public Server server = DEFAULT_SERVER;
	public File configDir;
	public File configFile;
	public File logDir;
	public File logFile;
	public FileWriter logWriter;
	public String CURRENT_NICK;
	public Boolean REQUESTED_QUIT = false;

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
	private boolean SERVER_REGISTERED = false;
	private boolean textFormatting = true;
	private int connectionRetries = 0;

	private HashMap<String, AnnouncerHandler> announcers;

	public boolean isConnected = false;
	public List<OfflineMessage> offlineMessages = new ArrayList<OfflineMessage>();


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
		createConfigDefaults(defaultConfig);

		// Announcers and other threads that should run while the bot is

		announcers = new HashMap<String, AnnouncerHandler>();
		announcers.put("YouTube", new YouTube(this));
		announcers.put("Twitch", new Twitch(this));

		// Now everything should be okay and we can start the bot...

		this.engineThread = new Thread(this, "iUtil 2 Main Thread");
		//this.engine = this;
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
			e.printStackTrace();
			System.exit(1);
		}
		log("Connection rety #"+this.connectionRetries);
		this.SERVER_REGISTERED = false;
		this.engineThread.interrupt();
		this.engineRunning = false;
		this.engineThread = new Thread(this, "iUtil 2 Main Thread");
		start();
	}

	public void log(String line) {
		log(line, "DEBUG");
	}

	public void log(String line, String type) {
		String time = (new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date(System.currentTimeMillis()));
		String out = time+" ["+type+"] "+line;
		if (Boolean.valueOf(config.getProperty("debug")))
			System.out.println(out.replaceAll("(\\r\\n|\\n\\r)", ""));
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
			config.store(new FileOutputStream(configFile), "iUtil 2 Main Config");
			log("Config succesfully saved.");
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
		//log("Creating config entries...");
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
			log("Lost connection to server: "+e.getLocalizedMessage());
			reconnect();
		}
		/*		catch (Exception | Error e) {
			log("FAILED to connect to server, cannot continue!");
			e.printStackTrace();
			System.exit(1);
		}*/
		if (connection.isConnected()) {
			this.connectionRetries = 0;
			log("Connected to server "+config.getProperty("server")+":"+config.getProperty("port")+".");
			log(connection.toString());

			changeNick(config.getProperty("nick", DEFAULT_NICK));
			log("Registered nick with server, waiting for response...");
			this.isConnected = true;

			// On response from the server, go into a loop until we get RAW 251.
			// If the server never sends this RAW, the bot will never exit this loop (possibly dangerous?)

			String line = "";
			try {
				Protocol protocol = new Protocol();
				// Connection data is parsed here, "regular" IRC traffic is parsed by Protocol.java
				while ((line = in.readLine()) != null && this.engineRunning && !this.engineThread.isInterrupted()) {
					log("<- "+line, "IRC");
					if (line.startsWith("PING ")) // in case the server pings while we're connecting
						send("PONG "+line.substring(5), true, true);
					// Handle being disconnected by the server during connect
					else if (line.startsWith("ERROR :")) {
						protocol.handleDisconnect(this, line.substring(7));
					}
					else if (line.indexOf("001") >= 0) // Server address
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
						send(config.getProperty("identificationString").replaceAll("%PASSWORD%", new String(readPassword())), false /* We have to remember not to log this line because passwords. */);
						send("MODE "+CURRENT_NICK+" "+config.getProperty("connectModes"));
					}

					else if (line.indexOf("433") >= 0) {
						String newNick = "";
						try {
							int n = Integer.valueOf(CURRENT_NICK.substring(CURRENT_NICK.length() - 1));
							newNick = CURRENT_NICK.substring(0, CURRENT_NICK.length() - 1)+n++;
						}
						catch (NumberFormatException e) {
							newNick = CURRENT_NICK+"2";
						}
						log("Nick is in use, trying "+newNick);
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

				String[] chans = config.getProperty("autoJoin").split(",");
				for (String a : chans)
					joinChannel(a);

				// After joining channels we can start any announcers we may have.
				for (AnnouncerHandler ah : announcers.values())
					ah.startAll();

				log("Protocol class is now handling incoming traffic.");
				log(protocol.toString());

				// Send any messages that were queued while the bot was diconnected.
				if (!this.offlineMessages.isEmpty())
					sendQueuedMessages();
				while ((line = in.readLine()) != null && this.engineRunning && !this.engineThread.isInterrupted()) {
					protocol.parse(line, this);
				}

			}
			catch (SocketException | SSLException e) { log("Server disconnected unexpectedly, attempting to reconnect."); reconnect(); }
			catch (Exception e) {
				log("Connection error, cannot continue!");
				e.printStackTrace();
				System.exit(1);			
			}
		}

	}


	private void sendQueuedMessages() {
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
		CURRENT_NICK = nick;
		send("NICK "+nick+"\r\n", true, true);
		if (!SERVER_REGISTERED) {
			send("USER "+nick+" ipeer.auron.co.uk "+nick+": iPeer's Java Utility Bot\r\n", true, true);
			SERVER_REGISTERED = true;
		}

	}

	public void joinChannel(String channel) {
		if (CHANNEL_LIST == null)
			CHANNEL_LIST = new HashMap<String, Channel>();
		send("JOIN "+channel);
		CHANNEL_LIST.put(channel.toLowerCase(), new Channel(channel.toLowerCase()));
		log("Now in "+CHANNEL_LIST.size()+" channels.");
		send("WHO "+channel);
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
		try {
			if (!this.connection.isConnected()) // Even if we want to force send a message, it shouldn't be sent if we're absolutely not connected.
				return;
			if (!this.isConnected && !sendIfNotConnected) {
				log("Tried to send message while offline, queuing for sending when (if) we reconnect.");
				this.offlineMessages.add(new OfflineMessage(data, log, sendIfNotConnected));
				return;
			}

			if (!(data.endsWith("\r\n") || data.endsWith("\n\r")))
				data = data+"\r\n";
			if (this.textFormatting)
				data = data
				.replaceAll("%C1%", Main.COLOUR+(config.getProperty("colour1")))
				.replaceAll("%C2%", Main.COLOUR+(config.getProperty("colour2")))
				.replaceAll("%C%", String.valueOf(Main.COLOUR))
				.replaceAll("%B%", String.valueOf(Main.BOLD))
				.replaceAll("%I%", String.valueOf(Main.ITALICS))
				.replaceAll("%U%", String.valueOf(Main.UNDERLINE))
				.replaceAll("%[HR]%", String.valueOf(Main.HIGHLIGHT))
				.replaceAll("%E%", String.valueOf(Main.ENDALL))
				.replaceAll("%DASH%", String.valueOf(Main.DASH));
			out.write(data);
			out.flush();
			if (log)
				log("-> "+data.replaceAll("\\[rn]", ""), "IRC");

		} catch (IOException e) {
			log("Couldn't send data to socket!");
			e.printStackTrace();
		}
	}

	public HashMap<String, Channel> getChannelList() {
		return CHANNEL_LIST;
	}

	public void quit(String quitMessage) {
		quit(quitMessage, false);
	}

	public void quit(String quitMessage, boolean reconnect) {
		if (!reconnect)
			REQUESTED_QUIT = true;
		send("QUIT :"+quitMessage);
		this.isConnected = false;
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
			e.printStackTrace();
			return new char[0];
		} 
	}

	public void partChannel(String channel, String message) {
		if (!message.equals(""))
			send("PART "+channel+" :"+message);
		else
			send("PART "+channel);
		CHANNEL_LIST.remove(channel.toLowerCase());
		log("Now in "+CHANNEL_LIST.size()+" channels.");
	}

	public void amsg(String msg) {
		for (Channel c : CHANNEL_LIST.values())
			if (!config.getProperty("noAMSG").toLowerCase().contains(c.getName()))
				send("PRIVMSG "+c.getName()+" :"+msg);

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


}
