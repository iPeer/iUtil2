package com.simple.ipeer.iutil2.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;

public class YouTube implements AnnouncerHandler {

	public HashMap<String, YouTubeChannel> CHANNEL_LIST;
	protected Main engine;
	public List<YouTubeChannel> waitingToSync = new ArrayList<YouTubeChannel>();
	private boolean hasStarted = false;
	private static YouTube youtubeInstance;

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
			settings.put("youtubeLinkRegex", ".*https?://(www.)?youtu(be.com|.be)/(watch\\?v=)?.*");
			settings.put("youtubeGetIDRegex", "(?<=https?://(www.)?youtu(be.com|.be)/(watch\\?v=)?)[^=]*?( |$)");
			settings.put("youtubeDescriptionLengthLimit", "140");
			
			// Search settings
			
			settings.put("youtubeSearchDescriptions", "false"); // Should descriptions be shown when a user searches for videos?
			settings.put("youtubeMaxSearchResults",  "1");
			settings.put("youtubeSearchFormat", "%C1%[%C2%%AUTHOR%%C1%] %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] %DASH% %C2%%VIDEOURL%");
			settings.put("youtubeCountryCode", "GB");
			
			
			engine.createConfigDefaults(settings);

			File youtubeDir = new File(engine.config.getProperty("youtubeDir"));
			if (!youtubeDir.exists()) {
				youtubeDir.mkdirs();
				new File(youtubeDir, "cache").mkdirs();
				new File(youtubeDir, "config").mkdirs();
			}
			loadChannels();
			engine.log("YouTube announcer started up succesfully.", "YouTube");
			youtubeInstance = this;
		}
	}

	public void loadChannels() {
		CHANNEL_LIST = new HashMap<String, YouTubeChannel>();
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
				engine.logError(e, "YouTube");
			}
		}
		for (String c : channels) {
			YouTubeChannel d = new YouTubeChannel(c, engine, this);
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
			for (YouTubeChannel c : this.CHANNEL_LIST.values())
				users = users+c.getName()+",";
			Properties b = new Properties();
			b.setProperty("users", users);
			b.store(new FileOutputStream(a), "YouTube Username Cache File");
		}
		catch (Exception e) {
			if (engine != null)
				engine.log("Couldn't save youtube username list!", "YouTube");
			engine.logError(e, "YouTube");
		}
	}

	public void stopAll() {
		for (YouTubeChannel c : CHANNEL_LIST.values()) {
			if (engine != null)
				engine.log("Stopping YouTube thread for user "+c.getName(), "YouTube");
			c.stop();
		}
	}

	public void startAll() {
		for (YouTubeChannel c : CHANNEL_LIST.values()) {
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
		YouTubeChannel a = new YouTubeChannel(name, engine, this);
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
				Iterator<YouTubeChannel> it = this.waitingToSync.iterator();
				while (it.hasNext())
					if (it.next().getName().toLowerCase().equals(name.toLowerCase())) {
						it.remove();
						if (engine != null)
							engine.log(name+" was waiting to sync, but its removal is being requested.", "YouTube");
					}
			}
			YouTubeChannel c = this.CHANNEL_LIST.get(name.toLowerCase());
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
		for (YouTubeChannel c : this.CHANNEL_LIST.values())
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
		String views = "0";
		try {
			views = NumberFormat.getInstance().format(Integer.valueOf(e.getElementsByTagName("yt:statistics").item(0).getAttributes().getNamedItem("viewCount").toString().replaceAll("viewCount=|\"", "")));
		} catch (NullPointerException e1) { }
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
	
	public LinkedList<YouTubeSearchResult> getSearchResults(String query) throws SAXException, IOException, ParserConfigurationException {
		return getSearchResults(query, 0);
	}

	public LinkedList<YouTubeSearchResult> getSearchResults(String query, int retries) throws SAXException, IOException, ParserConfigurationException {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		HttpURLConnection conn = (HttpURLConnection)(new URL("https://gdata.youtube.com/feeds/api/videos?q="+URLEncoder.encode(query, "UTF-8" /* ISO-8859-1 */)+"&v=2&safeSearch=none&max-results="+(engine == null ? "1" : Integer.valueOf(engine.config.getProperty("youtubeMaxSearchResults")))+"&restriction="+(engine == null ? "GB" : engine.config.getProperty("youtubeCountryCode"))).openConnection());
		conn.setReadTimeout(2500);
		try {
			doc = builder.parse(conn.getInputStream());
		}
		catch (SocketTimeoutException e) {
			if (retries < 5) { // Infinite loop prevention
				System.err.println("Retry #"+(retries + 1));
				return getSearchResults(query, retries++);
			}
			else
				throw new RuntimeException("The server did not respond within 5 tries.");
		}
		Element element = doc.getDocumentElement();
		element.normalize();
		NodeList entries;
		if ((entries = element.getElementsByTagName("entry")).getLength() < 1)
			throw new RuntimeException("No videos found for '"+query+"'");
		LinkedList<YouTubeSearchResult> data = new LinkedList<YouTubeSearchResult>();
		for (int x = 0; x < entries.getLength(); x++) {
			NodeList d = entries.item(x).getChildNodes();
			int length, views, likes, dislikes, comments;
			length = likes = dislikes = length = comments = views = 0;
			String title, author, description, videoID;
			title = author = description = videoID = "";

			// Don't need to check these for null as they should never be empty

			author = ((Element)d).getElementsByTagName("author").item(0).getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
			title = ((Element)d).getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
			videoID = ((Element)d).getElementsByTagName("yt:videoid").item(0).getChildNodes().item(0).getNodeValue();

			length = Integer.valueOf(((Element)d).getElementsByTagName("media:content").item(0).getAttributes().getNamedItem("duration").getNodeValue());
			comments = Integer.valueOf(((Element)d).getElementsByTagName("gd:feedLink").item(0).getAttributes().getNamedItem("countHint").getNodeValue());

			
			// Needs to be checked for null values

			try {
				views = Integer.valueOf(((Element)d).getElementsByTagName("yt:statistics").item(0).getAttributes().item(1).getNodeValue());
			} catch (NullPointerException e1) { }

			try {
				NamedNodeMap ratings = ((Element)d).getElementsByTagName("yt:rating").item(0).getAttributes();
				dislikes = Integer.valueOf(ratings.item(0).getNodeValue());
				likes = Integer.valueOf(ratings.item(1).getNodeValue());
			} catch (NullPointerException e1) { }

			try {
				description = ((Element)d).getElementsByTagName("media:description").item(0).getFirstChild().getNodeValue().replaceAll("\\[rn]+", " ");
			} catch (NullPointerException e1) { }

			data.add(new YouTubeSearchResult(videoID, author, Matcher.quoteReplacement(title), Matcher.quoteReplacement(description), formatTime(length), length, views, likes, dislikes, comments));

		}
		return data;
	}

	public static YouTube getInstance() {
		return youtubeInstance;
	}

	public static void main(String[] args) {

		try {
			YouTube y = new YouTube(null);
			LinkedList<YouTubeSearchResult> a = y.getSearchResults("ellie goulding burn");
			for (YouTubeSearchResult b : a) {
				System.err.println(b.toString());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

}
