package com.simple.ipeer.iutil2.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;

public class YouTube implements AnnouncerHandler {

	public HashMap<String, Channel> CHANNEL_LIST;
	protected Main engine;
	public List<Channel> waitingToSync = new ArrayList<Channel>();
	private boolean hasStarted = false;

	public YouTube(Main engine) {
		if (engine != null)
			engine.log("YouTube announcer is starting up.", "YouTube");
		this.engine = engine;
		HashMap<String, String> settings = new HashMap<String, String>();
		settings.put("youtubeUpdateDelay", "600000");
		settings.put("youtubeAnnounceFormat", "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] %DASH% %C2%%VIDEOLINK%");
		settings.put("youtubeTitleBlacklist", "(live)?stream(ing)?");
		settings.put("youtubeDir", "./YouTube");
		settings.put("youtubeURLPrefix", "https://youtu.be/");
		settings.put("youtubeMaxUploads", "5");
		settings.put("youtubeMaxHistory", "5");
		engine.createConfigDefaults(settings);

		File youtubeDir = new File(engine.config.getProperty("youtubeDir"));
		if (!youtubeDir.exists()) {
			youtubeDir.mkdirs();
			new File(youtubeDir, "cache").mkdirs();
			new File(youtubeDir, "config").mkdirs();
		}
		loadChannels();
		if (engine != null)
			engine.log("YouTube announcer started up succesfully.", "YouTube");
	}

	public void loadChannels() {
		CHANNEL_LIST = new HashMap<String, Channel>();
		File a = new File(new File(engine.config.getProperty("youtubeDir"), "config"), "usernames.cfg");
		String[] channels = "TheiPeer,GuudeBoulderfist,EthosLab,Docm77,BdoubleO100,VintageBeef,GenerikB".split(",");

		if (a.exists()) {
			try {
				Properties b = new Properties();
				b.load(new FileInputStream(a));
				channels = b.getProperty("users").split(",");
			}
			catch (Exception e) {
				engine.log("Couldn't load username list!", "YouTube");
				e.printStackTrace();
			}
		}
		for (String c : channels) {
			Channel d = new Channel(c, engine, this);
			//d.startIfNotRunning();
			//waitingToSync.add(d);
			CHANNEL_LIST.put(c.toLowerCase(), d);
		}

	}

	public void saveChannels() {
		try {
			File a = new File(new File(engine.config.getProperty("youtubeDir"), "config"), "usernames.cfg");
			String users = "";
			for (Channel c : this.CHANNEL_LIST.values())
				users = users+c.getName()+",";
			Properties b = new Properties();
			b.setProperty("users", users);
			b.store(new FileOutputStream(a), "YouTube Username Cache File");
		}
		catch (Exception e) {
			if (engine != null)
				engine.log("Couldn't save youtube username list!", "YouTube");
			e.printStackTrace();
		}
	}

	public void stopAll() {
		for (Channel c : CHANNEL_LIST.values()) {
			if (engine != null)
				engine.log("Stopping YouTube thread for user "+c.getName(), "YouTube");
			c.stop();
		}
	}

	public void startAll() {
		for (Channel c : CHANNEL_LIST.values()) {
			c.startIfNotRunning();
		}
	}

	public void startIfNotRunning() {
		if (!this.hasStarted) {
			engine.log("Upload announcer is starting.", "YouTube");
			loadChannels();
		}
	}

	@Override
	public boolean addUser(String name) {
		if (this.CHANNEL_LIST.containsKey(name.toLowerCase()))
			return false;
		Channel a = new Channel(name, engine, this);
		a.setSyncing(true);
		a.update();
		this.CHANNEL_LIST.put(name.toLowerCase(), a);
		this.waitingToSync.add(a);
		saveChannels();
		if (engine != null)
			engine.log("User "+name+" succesfully added to users list and is waiting to sync.", "YouTube");
		return true;
	}

	@Override
	public boolean removeUser(String name) {
		if (this.CHANNEL_LIST.containsKey(name.toLowerCase())) {
			if (!this.waitingToSync.isEmpty()) {
				Iterator<Channel> it = this.waitingToSync.iterator();
				while (it.hasNext())
					if (it.next().getName().toLowerCase().equals(name.toLowerCase())) {
						it.remove();
						if (engine != null)
							engine.log(name+" was waiting to sync, but its removal is being requested.", "YouTube");
					}
			}
			Channel c = this.CHANNEL_LIST.get(name.toLowerCase());
			c.removeCache();
			c.stopIfRunning();
			this.CHANNEL_LIST.remove(name.toLowerCase());
			saveChannels();
			return true;
		}
		return false;
	}

	@Override
	public void updateAll() {
		for (Channel c : this.CHANNEL_LIST.values())
			c.update();
	}

	@Override
	public void update(String name) {
		if (this.CHANNEL_LIST.containsKey(name.toLowerCase()))
			this.CHANNEL_LIST.get(name.toLowerCase()).update();
	}

	@Override
	public long timeTilUpdate() {
		return this.CHANNEL_LIST.values().iterator().next().timeTilUpdate();
	}


}