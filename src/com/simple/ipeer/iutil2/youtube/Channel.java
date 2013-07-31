package com.simple.ipeer.iutil2.youtube;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.Main;

public class Channel implements Announcer, Runnable {

	private String channelName;
	private String realChannelName;
	private Thread thread;
	private boolean running = false;
	private long lastUpdate = 0L;
	protected Main engine;
	private YouTube youtube;
	private LinkedHashMap<String, Upload> channelUploads;
	private String lastUpload = "";

	public Channel (String name, Main engine, YouTube youtube) {
		this.channelName = this.realChannelName = name;
		this.engine = engine;
		this.youtube = youtube;
		this.channelUploads = loadCache();
		//this.lastUpload = new Properties();
	}

	public String getName() {
		return this.channelName;
	}

	public String getRealName() {
		return this.realChannelName;
	}


	public static void main(String[] args) {
		Channel c = new Channel("TheiPeer", null, null);
		c.lastUpload = "ZeGXGYSTOtc";
		c.update();

	}

	@Override
	public void run() {
		while (this.running && !this.thread.isInterrupted()) {
			if (!youtube.waitingToSync.isEmpty()) {
				Iterator<Channel> it = youtube.waitingToSync.iterator();
				while (it.hasNext()) {
					(it.next()).start();
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
				e.printStackTrace();
			}
		}
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
				if (videoID.equals(this.lastUpload) || x > Integer.valueOf(engine.config.getProperty("youtubeMaxUploads")))
					break;
				try {
					String author = data.item(12).getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
					if (!this.realChannelName.equals(author))
						this.realChannelName = author;
				}
				catch (NullPointerException e) { }
				String title = data.item(5).getChildNodes().item(0).getNodeValue();
				int duration = Integer.valueOf(((Element)data).getElementsByTagName("yt:duration").item(0).getAttributes().getNamedItem("seconds").getNodeValue());
				if (!channelUploads.containsKey(videoID)) {
					Upload u = new Upload(title, duration, videoID);
					channelUploads.put(videoID, u);
					while (channelUploads.size() > Integer.valueOf(engine.config.getProperty("youtubeMaxHistory")))
						channelUploads.remove(channelUploads.keySet().iterator().next());
					announce.add(u);
				}
			}

			if (!announce.isEmpty()) {
				lastUpload = announce.get(0).getID();
				saveCache();
				announce(announce);
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			engine.log("Couldn't update YouTube uploads for user "+this.channelName);
			e.printStackTrace();
		}
	}

	public void saveCache() throws FileNotFoundException, IOException {
		File b = new File((engine == null ? "./YouTube" : engine.config.getProperty("youtubeDir")), "cache/"+this.channelName+".iuc");
		if (b.exists())
			b.delete();
		BufferedWriter c = new BufferedWriter(new FileWriter(b));
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
		File a = new File((engine == null ? "./YouTube" : engine.config.getProperty("youtubeDir")), "cache/"+this.channelName+".iuc");
		try {
			BufferedReader b = new BufferedReader(new FileReader(a));
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

			int seconds = len % 60;
			int minutes = len / 60;
			int hours = (int)Math.floor(minutes / 60);
			if (hours > 0)
				minutes -= hours*60;
			String time = (hours > 0 ? String.format("%02d", hours)+":" : "")+String.format("%02d", minutes)+":"+String.format("%02d", seconds);

			String out = (engine == null ? "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1] %DASH% %C2%%VIDEOLINK%" : engine.config.getProperty("youtubeAnnounceFormat"))
					.replaceAll("%(VIDEO)?TITLE%", title)
					.replaceAll("(%(VIDEO)?(LENGTH|DURATION)%)", time)
					.replaceAll("%USER%", this.realChannelName)
					.replaceAll("%C1%", Main.COLOUR+engine.config.getProperty("colour1"))
					.replaceAll("%C2%", Main.COLOUR+engine.config.getProperty("colour2"))
					.replaceAll("%B%", String.valueOf(Main.BOLD))
					.replaceAll("%I%", String.valueOf(Main.ITALICS))
					.replaceAll("%U%", String.valueOf(Main.UNDERLINE))
					.replaceAll("%[HR]%", String.valueOf(Main.HIGHLIGHT))
					.replaceAll("%E%", String.valueOf(Main.ENDALL))
					.replaceAll("%VIDEOLINK%", engine.config.getProperty("youtubeURLPrefix")+vid)
					.replaceAll("%DASH%", String.valueOf(Main.DASH));
			if (engine == null)	
				System.err.println(out);
			else
				engine.amsg(out);
		}
	}

	@Override
	public void stop() {
		this.running = false;
		(this.thread).interrupt();
	}

	@Override
	public void start() {
		this.thread = new Thread(this, "YouTube Announcer Thread ("+this.channelName+")");
		this.running = true;
		(this.thread).start();
	}

	public void startIfNotRunning() {
		if (!this.running)
			start();
	}

}
