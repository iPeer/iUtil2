package com.simple.ipeer.iutil2.youtube;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.Main;

public class YouTubeChannel implements Announcer, Runnable {

	private String channelName;
	private String realChannelName;
	private Thread thread;
	private boolean isRunning = false;
	private long lastUpdate = 0L;
	protected Main engine;
	private YouTube youtube;
	private LinkedHashMap<String, Upload> channelUploads;
	private String lastUpload = "";
	private boolean isSyncing = false;
	private File cacheFile;

	public YouTubeChannel (String name, Main engine, YouTube youtube) {
		this.channelName = this.realChannelName = name;
		this.engine = engine;
		this.youtube = youtube;
		this.cacheFile = new File((engine == null ? "./YouTube" : engine.config.getProperty("youtubeDir")), "cache/"+this.channelName+".iuc");
		this.channelUploads = loadCache();
		//this.lastUpload = new Properties();
	}

	public String getName() {
		return this.channelName;
	}

	public String getRealName() {
		return this.realChannelName;
	}


//	public static void main(String[] args) {
//		YouTubeChannel c = new YouTubeChannel("TheiPeer", null, new YouTube(null));
//		c.clearUploads();
//		c.setSyncing(true);
//		c.update();
//	}

	public void clearUploads() {
		this.channelUploads.clear();
	}

	@Override
	public void run() {
		while (this.isRunning && !this.thread.isInterrupted()) {
			if (youtube != null && !youtube.waitingToSync.isEmpty()) {
				Iterator<YouTubeChannel> it = youtube.waitingToSync.iterator();
				while (it.hasNext()) {
					(it.next()).startIfNotRunning();
					it.remove();
				}
			}
			try {
				update();
				lastUpdate = System.currentTimeMillis();
				Thread.sleep(Long.valueOf(engine.config.getProperty("youtubeUpdateDelay")));
			} 
			catch (InterruptedException e) { }
			catch (Exception e) {
				engine.log("["+this.channelName+"] Cannot update!", "YouTube");
				engine.logError(e, "YouTube");
			}
		}
		if (engine != null)
			engine.log("YouTube thread for user "+this.channelName+" ("+this.realChannelName+") is stopping.", "YouTube");
	}

	@Override
	public void update() {
		try {
			LinkedList<Upload> announce = new LinkedList<Upload>();

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			doc = builder.parse("https://gdata.youtube.com/feeds/api/users/"+this.channelName+"/uploads");
			Element element = doc.getDocumentElement();
			element.normalize();

			NodeList uploads = element.getElementsByTagName("entry");
			for (int x = 0; x < uploads.getLength() - 1; x++) {
				NodeList data = uploads.item(x).getChildNodes();
				String videoID = data.item(0).getChildNodes().item(0).getNodeValue().replaceAll("https?://gdata.youtube.com/feeds/api/videos/", "");
				if (this.channelUploads.containsKey(videoID) || videoID.equals(this.lastUpload) || x > Integer.valueOf((this.isSyncing ? "0" : engine.config.getProperty("youtubeMaxUploads"))))
					break;
				try {
					String author = ((Element)data).getElementsByTagName("author").item(0).getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
					if (!this.realChannelName.equals(author))
						this.realChannelName = author;
				}
				catch (NullPointerException e) { }
				String title = ((Element)data).getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
				int duration = Integer.valueOf(((Element)data).getElementsByTagName("yt:duration").item(0).getAttributes().getNamedItem("seconds").getNodeValue());
				if (!channelUploads.containsKey(videoID)) {
					Upload u = new Upload(title, duration, videoID);
					channelUploads.put(videoID, u);
					while (channelUploads.size() > (engine == null ? 10 : Integer.valueOf(engine.config.getProperty("youtubeMaxHistory"))))
						channelUploads.remove(channelUploads.keySet().iterator().next());
					announce.add(u);
				}
			}
			if (this.isSyncing)
				this.isSyncing = false;
			if (!announce.isEmpty()) {
				lastUpload = announce.get(0).getID();
				saveCache();
				announce(announce);
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			engine.log("Couldn't update YouTube uploads for user "+this.channelName);
			engine.logError(e, "YouTube");
		}
		
	}

	public void saveCache() throws FileNotFoundException, IOException {
		if (this.cacheFile.exists())
			this.cacheFile.delete();
		BufferedWriter c = new BufferedWriter(new FileWriter(this.cacheFile));
		c.write(lastUpload+"\n");
		if (!channelUploads.isEmpty())
		{
			for (String d : channelUploads.keySet()) {
				Upload e = channelUploads.get(d);
				c.write(e.getID()+"\01"+e.getLength()+"\01"+e.getTitle()+"\n");
			}
			c.close();

		}
	}

	public LinkedHashMap<String, Upload> loadCache() {
		try {
			BufferedReader b = new BufferedReader(new FileReader(this.cacheFile));
			String line = null;
			LinkedHashMap<String, Upload> c = new LinkedHashMap<String, Upload>();
			while ((line = b.readLine()) != null) {

				if (this.lastUpload.equals("") || this.lastUpload == null)
					this.lastUpload = line.trim();
				else {
					String[] data = line.split("\01");
					c.put(data[0], new Upload(data[2], data[1], data[0]));
				}	

			}
			b.close();
			//a.delete();
			return c;
		}
		catch (IOException e) {
			return new LinkedHashMap<String, Upload>();
		}
	}

	@Override
	public long timeTilUpdate() {
		return (lastUpdate + Long.valueOf(engine.config.getProperty("youtubeUpdateDelay"))) - System.currentTimeMillis();
	}

	public void announce(LinkedList<Upload> uploads) {
		Iterator<Upload> it = uploads.iterator();
		while (it.hasNext()) {
			Upload u = it.next();
			String title = u.getTitle();
			String vid = u.getID();

			int len = u.getLength();

			String time = youtube.formatTime(len);

			String out = (engine == null ? "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1] %DASH% %C2%%VIDEOLINK%" : engine.config.getProperty("youtubeAnnounceFormat"))
					.replaceAll("%(VIDEO)?TITLE%", Matcher.quoteReplacement(title)) // Fix for "Illegal group reference" when title contains regex characters such as $.
					.replaceAll("(%(VIDEO)?(LENGTH|DURATION)%)", time)
					.replaceAll("%USER%", this.realChannelName)
					.replaceAll("%VIDEOLINK%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix")+vid));
			if (engine == null)	
				System.err.println(out);
			else
				engine.amsg(out);
		}
	}

	@Override
	public void stop() {
		this.isRunning = false;
		(this.thread).interrupt();
	}

	@Override
	public void start() {
		if (engine != null)
			engine.log("YouTube Announcer Thread "+this.channelName+" is starting", "YouTube");
		this.thread = new Thread(this, "YouTube Announcer Thread ("+this.channelName+")");
		this.isRunning = true;
		(this.thread).start();
	}
	
	public void stopIfRunning() {
		if (this.isRunning)
			stop();
	}

	public void startIfNotRunning() {
		if (!this.isRunning) {
			start();
		}
	}
	
	public void setSyncing(boolean b) {
		this.isSyncing = b;
	}
	
	public void removeCache() {
		if (this.cacheFile.exists()) {
			if (!this.cacheFile.delete())
				this.cacheFile.deleteOnExit();
		}
	}

}
