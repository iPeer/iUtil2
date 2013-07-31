package com.simple.ipeer.iutil2.twitch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;

public class Twitch implements AnnouncerHandler {
	
	private Main engine;
	private HashMap<String, TwitchChannel> users;
	public List<TwitchChannel> waitingToSync = new ArrayList<TwitchChannel>();
	
	public Twitch (Main engine) {
		engine.log("Twitch announcer is starting up.", "Twitch");
		this.engine = engine;
		HashMap<String, String> settings = new HashMap<String, String>();
		settings.put("twitchUpdateDelay", "600000");
		settings.put("twitchAnnounceFormat", "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%(%C2%%STREAMDESC%%C1%) [%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
		settings.put("twitchAnnounceFormatNoGame", "%C2%%USER% %C1%is streaming %C2%%STREAMDESC% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
		settings.put("twitchAnnounceFormatNoDesc", "%C2%%USER% %C1%is streaming %C2%%GAMENAME% %C1%[%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
		settings.put("twitchAnnounceFormatNoGameOrDesc", "%C2%%USER% %C1%is streaming [%C2%%STREAMQUALITY%%C1%] %DASH% %C2%%URL%");
		settings.put("twitchAnnounceFormatNotStreaming", "%C2%%USER% %C1%is no longer streaming.");
		settings.put("twitchURLPrefix", "http://twitch.tv/");
		settings.put("twitchDir", "./Twitch");
		settings.put("twitchDefaultChannels", "BdoubleO100,GenerikB,Kurtjmac,Harumei,Nebris,TotalBiscuit");
		engine.createConfigDefaults(settings);
		File a = new File(engine.config.getProperty("twitchDir"), "cache");
		if (!a.exists())
			a.mkdirs();
		a = new File(engine.config.getProperty("twitchDir"), "config");
		if (!a.exists())
			a.mkdirs();
		this.users = loadChannels();
		if (engine != null)
			engine.log("Twitch announcer started up succesfully.", "Twitch");
	}
	
	public void saveChannels() {
		if (engine != null)
			engine.log("Attempting to save Twitch usernames...", "Twitch");
		File config = new File((engine == null ? "./Twitch" : engine.config.getProperty("twitchDir"))+"/config/usernames.cfg");
		try {
			FileWriter out = new FileWriter(config);
			for (TwitchChannel c : this.users.values())
				out.write(c.getName()+"\n");
			out.flush();
			out.close();
			if (engine != null)
				engine.log("Twitch usernames saved succesfully.", "Twitch");
		} catch (IOException e) {
			if (engine != null)
				engine.log("Couldn't save Twitch channels!", "Twitch");
			e.printStackTrace();
		}
	}
	
	public HashMap<String, TwitchChannel> loadChannels() {
		if (engine != null)
			engine.log("Attempting to load Twitch usernames from file", "Twitch");
		File config = new File((engine == null ? "./Twitch" : engine.config.getProperty("twitchDir"))+"/config/usernames.cfg");
		HashMap<String, TwitchChannel> ret = new HashMap<String, TwitchChannel>();
		try {
			Scanner s = new Scanner(new FileInputStream(config), "UTF-8");
			while (s.hasNextLine()) {
				String user = s.nextLine();
				ret.put(user.toLowerCase(), new TwitchChannel(user, engine, this));
			}
			s.close();
			if (engine != null)
				engine.log("Succesfully loaded usernames from file", "Twitch");
			return ret;
		} catch (FileNotFoundException e) {
			if (engine != null)
				engine.log("Username file not found, loading default usernames.", "Twitch");
			for (String u : engine.config.getProperty("twitchDefaultChannels").split(","))
				ret.put(u.toLowerCase(), new TwitchChannel(u, engine, this));
			return ret;
		}
	}

	@Override
	public boolean addUser(String name) {
		if (engine != null)
			engine.log("Attempting to add user "+name+" to Twitch user list...", "Twitch");
		if (this.users.containsKey(name.toLowerCase())) {
			if (engine != null)
				engine.log("User "+name+" already appears in the list.", "Twitch");
			return false;
		}
		TwitchChannel a = new TwitchChannel(name, engine, this);
		a.update();
		this.users.put(name.toLowerCase(), a);
		this.waitingToSync.add(a);
		if (engine != null)
			engine.log("User "+name+" succesfully added to users list and is waiting to sync.", "Twitch");
		saveChannels();
		return true;
	}

	@Override
	public boolean removeUser(String name) {
		if (this.users.containsKey(name.toLowerCase())) {
			if (!this.waitingToSync.isEmpty()) {
				Iterator<TwitchChannel> it = this.waitingToSync.iterator();
				while (it.hasNext())
					if (it.next().getName().toLowerCase().equals(name.toLowerCase())) {
						it.remove();
						if (engine != null)
							engine.log(name+" was waiting to sync, but its removal is being requested.", "Twitch");
					}
			}
			TwitchChannel a = this.users.get(name.toLowerCase());
			a.stop();
			a.removeCache();
			this.users.remove(name.toLowerCase());
			saveChannels();
			return true;
		}
		return false;
	}

	@Override
	public void updateAll() {
		if (engine != null)
			engine.log("Updating all watched channels.", "Twitch");
		for (TwitchChannel c : this.users.values())
			c.update();
	}

	@Override
	public void startAll() {
		for (TwitchChannel c : this.users.values())
			c.startIfNotRunning();
	}

	@Override
	public void stopAll() {
		for (TwitchChannel c : this.users.values()) {
			if (engine != null)
				engine.log("Stopping TwitchChannel thread for user "+c.getName(), "Twitch");
			c.stop();
		}
	}
	
	@Override
	public void update(String name) {
		if (this.users.containsKey(name.toLowerCase())) {
			if (engine != null)
				engine.log("Updating "+name, "Twitch");
			this.users.get(name.toLowerCase()).update();
		}
	}

	@Override
	public long timeTilUpdate() {
		return this.users.values().iterator().next().timeTilUpdate();
	}

}
