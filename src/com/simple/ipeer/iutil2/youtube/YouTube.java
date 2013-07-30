package com.simple.ipeer.iutil2.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.simple.ipeer.iutil2.engine.Main;

public class YouTube {

	public HashMap<String, Channel> CHANNEL_LIST;
	protected Main engine;
	public List<Channel> waitingToSync = new ArrayList<Channel>();
	private boolean hasStarted = false;
	
	public YouTube(Main engine) {
		this.engine = engine;
		HashMap<String, String> settings = new HashMap<String, String>();
		settings.put("youtubeUpdateDelay", "600000");
		settings.put("youtubeAnnounceFormat", "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] %DASH% %C2%%VIDEOLINK%");
		settings.put("youtubeTitleBlacklist", "(live)?stream(ing)?");
		settings.put("youtubeDir", "./YouTube");
		settings.put("youtubeURLPrefix", "https://youtu.be/");
		settings.put("youtubeMaxUploads", "5");
		engine.createConfigDefaults(settings);
		
		File youtubeDir = new File(engine.config.getProperty("youtubeDir"));
		if (!youtubeDir.exists()) {
			youtubeDir.mkdirs();
			new File(youtubeDir, "cache").mkdirs();
			new File(youtubeDir, "config").mkdirs();
		}
		
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
			d.startIfNotRunning();
			//waitingToSync.add(d);
			CHANNEL_LIST.put(c.toLowerCase(), d);
		}
		
	}
	
	public void stopAll() {
		for (Channel c : CHANNEL_LIST.values())
			c.stop();
	}
	
	public void startAll() {
		for (Channel c : CHANNEL_LIST.values())
			c.startIfNotRunning();
	}
	
	public void startIfNotRunning() {
		if (!this.hasStarted) {
			engine.log("Upload announcer is starting.", "YouTube");
			loadChannels();
		}
	}


}
