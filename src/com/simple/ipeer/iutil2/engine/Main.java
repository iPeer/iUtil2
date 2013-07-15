package com.simple.ipeer.iutil2.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import com.simple.ipeer.iutil2.irc.Server;

public class Main {
	
	private static final File DEFAULT_CONFIG_FILE = new File("/config/config.cfg");
	private static final Server DEFAULT_SERVER = new Server("irc.swiftirc.net", false, 6667);
	private static final String DEFAULT_NICK = "iUtil";
	
	public Properties config = new Properties();
	public String MY_NICK = DEFAULT_NICK;
	public Server server = DEFAULT_SERVER;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap<String, String> startArgs = new HashMap<String, String>();
		for (String a : args) {
			String[] c = a.split("=");
			startArgs.put(c[0].replace("-", ""), c[1]);
		}	
	}

}
