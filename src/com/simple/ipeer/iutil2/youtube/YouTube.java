package com.simple.ipeer.iutil2.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

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
		if (engine != null) {
			HashMap<String, String> settings = new HashMap<String, String>();

			// Announcer settings

			settings.put("youtubeUpdateDelay", "600000");
			settings.put("youtubeAnnounceFormat", "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] %DASH% %C2%%VIDEOLINK%");
			settings.put("youtubeTitleBlacklist", "(live)?stream(ing)?");
			settings.put("youtubeDir", "./YouTube");
			settings.put("youtubeURLPrefix", "https://youtu.be/");
			settings.put("youtubeMaxUploads", "5");
			settings.put("youtubeMaxHistory", "10");

			// Link info settings

			settings.put("youtubeInfoFormat", "%C1%[%C2%%USER%%C1%] %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] (%C2%%VIEWS%%C1% views, %C2%%COMMENTS%%C1% comments, %C2%%LIKES%%C1% likes, %C2%%DISLIKES%%C1% dislikes) %DASH% %C2%%VIDEOURL%");
			settings.put("youtubeInfoFormatDescription", "%C1%Description: %C2%%DESCRIPTION%");
			settings.put("youtubeDescriptionClippingMode", "word");
			settings.put("youtubeMaxProcessedLinks", "2");
			settings.put("youtubeLinkRegex", ".*https?://(www.)?youtu(be.com|\\.be)/(watch\\?v=)?.*");
			settings.put("youtubeGetIDRegex", "(?<=https?://(www.)?youtu(be.com|\\.be)/(watch\\?v=)?).*?( |$)");
			settings.put("youtubeDescriptionLengthLimit", "140");

			engine.createConfigDefaults(settings);

			File youtubeDir = new File(engine.config.getProperty("youtubeDir"));
			if (!youtubeDir.exists()) {
				youtubeDir.mkdirs();
				new File(youtubeDir, "cache").mkdirs();
				new File(youtubeDir, "config").mkdirs();
			}
			loadChannels();
			engine.log("YouTube announcer started up succesfully.", "YouTube");
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
			d.setSyncing(true); // Stops the bot spamming the hell out of channels after downtime or accidental cache invalidation.
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

	public HashMap<String, String> getVideoInfo(String videoid) throws SAXException, IOException, ParserConfigurationException {
		//https://gdata.youtube.com/feeds/api/videos/ZeGXGYSTOtc
		HashMap<String, String> data = new HashMap<String, String>();
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		String URL = "https://gdata.youtube.com/feeds/api/videos/"+videoid+"?v=2";
		DocumentBuilder a = f.newDocumentBuilder();
		Document doc = a.newDocument();
		doc = a.parse(URL);
		Element e = doc.getDocumentElement();
		e.normalize();

		//NodeList data = e.getElementsByTagName("entry");
		String title = e.getElementsByTagName("title").item(0).getFirstChild().getNodeValue();
		String desc = "";
		try {
			desc = e.getElementsByTagName("media:description").item(0).getFirstChild().getNodeValue().replaceAll("\n+", " ");
		} catch (NullPointerException e1) { }
		String likes = "0", dislikes = "0";
		try {
			NamedNodeMap li = e.getElementsByTagName("yt:rating").item(0).getAttributes();
			dislikes = NumberFormat.getInstance().format(Integer.valueOf(li.getNamedItem("numDislikes").toString().replaceAll("numDislikes=|\"", "")));
			likes = NumberFormat.getInstance().format(Integer.valueOf(li.getNamedItem("numLikes").toString().replaceAll("numLikes=|\"", "")));
		} catch (NullPointerException e1) { }
		String comments = NumberFormat.getInstance().format(Integer.valueOf(e.getElementsByTagName("gd:feedLink").item(0).getAttributes().getNamedItem("countHint").toString().replaceAll("countHint=|\"", "")));
		String views = NumberFormat.getInstance().format(Integer.valueOf(e.getElementsByTagName("yt:statistics").item(0).getAttributes().getNamedItem("viewCount").toString().replaceAll("viewCount=|\"", "")));
		String duration = e.getElementsByTagName("yt:duration").item(0).getAttributes().getNamedItem("seconds").toString().replaceAll("seconds=|\"", "");
		String author = e.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();

		data.put("title", title);
		if (desc.length() > 0)
			data.put("description", desc);
		data.put("dislikes", dislikes);
		data.put("likes", likes);
		data.put("comments",  comments);
		data.put("views", views);
		data.put("duration", (duration.equals("0") ? "LIVE" : formatTime(duration)));
		data.put("durationSeconds", duration);
		data.put("author", author);
		return data;
	}

	//	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
	//
	//		YouTube y = new YouTube(null);
	//		HashMap<String, String> a = y.getVideoInfo("CGyEd0aKWZE");
	//		for (String b : a.keySet())
	//			System.err.println(b+": "+a.get(b));
	//
	//	}

	public String formatTime(String seconds) {
		return formatTime(Integer.valueOf(seconds));
	}

	public String formatTime(int len) {
		int seconds = len % 60;
		int minutes = len / 60;
		int hours = (int)Math.floor(minutes / 60);
		if (hours > 0)
			minutes -= hours*60;
		return (hours > 0 ? String.format("%02d", hours)+":" : "")+String.format("%02d", minutes)+":"+String.format("%02d", seconds);
	}

}
