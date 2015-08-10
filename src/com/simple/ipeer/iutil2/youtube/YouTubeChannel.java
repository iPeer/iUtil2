package com.simple.ipeer.iutil2.youtube;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.util.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.SAXException;

/**
 *
 * @author iPeer
 */
public class YouTubeChannel implements Announcer, Runnable, DebuggableSub {

    private String CHANNEL_UPLOADS_PLAYLIST = "";
    private String CHANNEL_REAL_NAME = "";
    private String CHANNEL_ID = "";
    private long lastUpdate = 0L;
    private long startedAt = 0L;
    private long lastExceptionTime = 0L;
    private Throwable lastException = null;
    private boolean isRunning = false;
    private LinkedList<String> channelUploads;
    private boolean isSyncing = false;
    private Thread thread;

    protected Main engine;
    protected YouTube youtube;

    public YouTubeChannel(String c, Main engine, YouTube youtube) {
	this(c, engine, youtube, false);
    }

    public YouTubeChannel(String c, Main engine, YouTube youtube, boolean isUsername) {
	this.engine = engine;
	this.youtube = youtube;
	this.channelUploads = new LinkedList<String>();
	if (!new File("./YouTube/Channels/", c + ".ytui").exists()) {
	    this.createChannelInfo(c, isUsername);
	} else {
	    this.loadChannelDataFromFile(c);
	}

    }

    public void setChannelName(String cn) {
	this.CHANNEL_REAL_NAME = cn;
    }

    public void setChannelID(String cid) {
	this.CHANNEL_ID = cid;
    }

    public void setChannelPlaylist(String plID) {
	this.CHANNEL_UPLOADS_PLAYLIST = plID;
    }

    public static void main(String[] args) throws IOException {
	/*File f = new File("./YouTube/");
	 f.mkdirs();
	 Util.writeEncrypted("[redacted]", new File(f, "YouTubeAPIKey.uic"));*/
	YouTubeChannel yt = new YouTubeChannel("TheiPeer", null, null, true);
	yt.update();
	//yt.createChannelInfo("TheiPeer", true);
    }

    private void saveChannelDataToFile() throws IOException {
	File cConfig = new File("./YouTube/Channels/", this.CHANNEL_ID + ".ytui");
	JSONObject json = new JSONObject();
	json.put("channelName", this.CHANNEL_REAL_NAME);
	json.put("channelID", this.CHANNEL_ID);
	json.put("uploadsPlaylist", this.CHANNEL_UPLOADS_PLAYLIST);
	if (channelUploads.size() > 0) {
	    JSONArray ja = new JSONArray();
	    ja.addAll(channelUploads);
	    json.put("recentUploads", ja);
	}
	FileWriter fw;
	json.writeJSONString((fw = new FileWriter(cConfig)));
	fw.flush();
	fw.close();

    }

    private void loadChannelDataFromFile(String cID) {
	File cConfig = new File("./YouTube/Channels/", cID + ".ytui");
	try {
	    JSONObject json = (JSONObject) JSONValue.parse(new FileReader(cConfig));
	    this.CHANNEL_REAL_NAME = json.get("channelName").toString();
	    this.CHANNEL_ID = json.get("channelID").toString();
	    this.CHANNEL_UPLOADS_PLAYLIST = json.get("uploadsPlaylist").toString();
	    if (json.containsKey("recentUploads")) {
		JSONArray _json = (JSONArray) json.get("recentUploads");
		for (Object o : _json.toArray()) {
		    channelUploads.add(o.toString());
		}
	    }
	} catch (FileNotFoundException ex) {
	    // Should never be fired because we check it bfore this code is even tried :/
	}

    }

    public final void createChannelInfo(String id, boolean isUsername) {
	try {
	    String apiKey = Util.readEncrypted(new File("./YouTube/YouTubeAPIKey.iuc"));
	    HttpsURLConnection urlC = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/channels?part=snippet%2CcontentDetails&" + (!isUsername ? "id" : "forUsername") + "=" + id + "&key=" + apiKey).openConnection();
	    JSONObject json = (JSONObject) JSONValue.parse(new InputStreamReader(urlC.getInputStream()));
	    System.err.println((json));
	    JSONArray _json = (JSONArray) json.get("items");
	    String cName = ((JSONObject) ((JSONObject) _json.get(0)).get("snippet")).get("title").toString();
	    String cID = ((JSONObject) _json.get(0)).get("id").toString();
	    String cUploads = ((JSONObject) ((JSONObject) ((JSONObject) _json.get(0)).get("contentDetails")).get("relatedPlaylists")).get("uploads").toString();
	    //System.out.println(cName + " / " + cID + " / " + cUploads);
	    File cConfig = new File("./YouTube/Channels/");
	    cConfig.mkdirs();
	    cConfig = new File(cConfig, cID + ".ytui");
	    // Because JSON is awesome and totally not annoying in Java...
	    JSONObject jo = new JSONObject();
	    jo.put("channelName", cName);
	    jo.put("channelID", cID);
	    jo.put("uploadsPlaylist", cUploads);
	    this.CHANNEL_ID = cID;
	    this.CHANNEL_REAL_NAME = cName;
	    this.CHANNEL_UPLOADS_PLAYLIST = cUploads;
	    BufferedWriter bw = new BufferedWriter(new FileWriter(cConfig));
	    jo.writeJSONString(bw);
	    bw.flush();
	    bw.close();
	} catch (Throwable ex) {
	    Logger.getLogger(YouTubeChannel.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    @Override
    public void run() {
	while (this.isRunning && !this.thread.isInterrupted()) {

	    try {

		youtube.syncChannelsIfNotSyncing();

		update();

		this.lastUpdate = System.currentTimeMillis();

	    } catch (IOException ex) {
		engine.log(("Couldn't update YouTube thread for channel ID " + this.CHANNEL_ID));
		engine.logError(ex, "YouTube", this.CHANNEL_ID, this.CHANNEL_REAL_NAME, this.CHANNEL_UPLOADS_PLAYLIST);
		this.lastException = ex;
		this.lastExceptionTime = System.currentTimeMillis();
	    }
	    try {
		Thread.sleep((engine == null ? 600000 : Long.valueOf(engine.config.getProperty("youtubeUpdateDelay"))));
	    } catch (InterruptedException ex) {
		this.isRunning = false;
		youtube.addToSync(this);
		engine.log(("Couldn't sleep YouTube thread for channel ID " + this.CHANNEL_ID));
		engine.logError(ex, "YouTube", this.CHANNEL_ID, this.CHANNEL_REAL_NAME, this.CHANNEL_UPLOADS_PLAYLIST);
		this.lastException = ex;
		this.lastExceptionTime = System.currentTimeMillis();
	    }
	}
    }

    @Override
    public void update() throws IOException {

	String apiKey = Util.readEncrypted(new File("./YouTube/YouTubeAPIKey.iuc"));

	try {
	    // Behold, the amazingness of the YouTube v3 API that forces you to make AT LEAST 2 calls to get any decent video information

	    // Get a list of video IDs we need to look up
	    HttpsURLConnection urlC = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/playlistItems?part=contentDetails&playlistId=" + this.CHANNEL_UPLOADS_PLAYLIST + "&key=" + apiKey).openConnection();
	    JSONObject json = (JSONObject) JSONValue.parse(new InputStreamReader(urlC.getInputStream()));
	    int num = 1;
	    LinkedList<String> idsToAnnounce = new LinkedList<String>();
	    JSONArray jsonArray = (JSONArray) json.get("items");
	    System.err.println(jsonArray.size() + " objects.");
	    for (int x = 0; x < jsonArray.size(); x++) {

		JSONObject _json = (JSONObject) (jsonArray.get(x));

		String videoID = ((JSONObject) _json.get("contentDetails")).get("videoId").toString();
		System.err.println(channelUploads.contains(videoID));
		if (channelUploads.contains(videoID) || num > (this.isSyncing ? 1 : (this.engine == null ? 5 : Integer.parseInt(engine.config.getProperty("youtubeMaxUploads"))))) {
		    break;
		}

		idsToAnnounce.add(videoID);

		num++;

	    }

	    //System.err.println(idsToAnnounce.size() + " uploads to announce");
	    // Now we have a list of IDs, we need to announce them.
	    // Prepare a list of IDs so we can request them all at once.
	    // I separated these 2 loops for easy maintenance.
	    if (idsToAnnounce.size() > 0) {

		String videoIDs = "";

		for (Iterator<String> it = idsToAnnounce.iterator(); it.hasNext();) {

		    String videoID = it.next();
		    channelUploads.add(videoID);
		    while (channelUploads.size() > (engine == null ? 10 : Integer.valueOf(engine.config.getProperty("youtubeMaxHistory")))) {
			channelUploads.remove(channelUploads.iterator().next());
		    }

		    videoIDs = videoIDs + (videoIDs.length() > 0 ? "," : "") + videoID;
		    it.remove();

		}

		//System.out.println(videoIDs);
		// Request the data and announce the uploads!
		HttpsURLConnection _urlC = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails&id=" + videoIDs + "&key=" + apiKey).openConnection();
		JSONObject _json = (JSONObject) JSONValue.parse(new InputStreamReader(_urlC.getInputStream()));
		JSONArray _jsonArray = (JSONArray) _json.get("items");

		LinkedList<Upload> uploads = new LinkedList<Upload>();

		for (int _x = 0; _x < _jsonArray.size(); _x++) {

		    JSONObject _jo = (JSONObject) _jsonArray.get(_x);

		    //System.out.println(_jo);
		    String videoID = _jo.get("id").toString();
		    JSONObject __jo = (JSONObject) _jo.get("snippet");
		    String channelName = __jo.get("channelTitle").toString();
		    String videoTitle = __jo.get("title").toString();
		    __jo = (JSONObject) _jo.get("contentDetails");
		    String videoLength = __jo.get("duration").toString();
		    //if (_x < Integer.valueOf((this.isSyncing ? "0" : engine.config.getProperty("youtubeMaxUploads")))) {
		    uploads.add(new Upload(videoTitle, videoLength, videoID));
			//}

		    //System.out.println(channelName + " just uploaded a video: " + videoTitle + " [" + videoLength + "] -- https://youtu.be/" + videoID);
		}

		announce(uploads);

		saveChannelDataToFile();

		if (this.isSyncing) {
		    this.isSyncing = false;
		}

	    }

	} catch (MalformedURLException ex) {
	    this.lastException = ex;
	    this.lastExceptionTime = System.currentTimeMillis();
	    Logger.getLogger(YouTubeChannel.class.getName()).log(Level.SEVERE, null, ex);
	}

    }

    public void announce(LinkedList<Upload> uploads) {
	Iterator<Upload> it = uploads.iterator();
	while (it.hasNext()) {
	    Upload u = it.next();
	    String title = u.getTitle();
	    String vid = u.getID();

	    //int len = u.getLength();
	    String time = u.getLength().replace("PT", "");
	    try {
		time = this.youtube.formatTime(u.getLength());
	    } catch (DatatypeConfigurationException ex) {
		Logger.getLogger(YouTubeChannel.class.getName()).log(Level.SEVERE, null, ex);
	    }

	    String out = (engine == null ? "%C2%%USER% %C1%uploaded a video: %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1] %DASH% %C2%%VIDEOLINK%" : engine.config.getProperty("youtubeAnnounceFormat"))
		    .replaceAll("%(VIDEO)?TITLE%", Matcher.quoteReplacement(title)) // Fix for "Illegal group reference" when title contains regex characters such as $.
		    .replaceAll("(%(VIDEO)?(LENGTH|DURATION)%)", time)
		    .replaceAll("%USER%", this.CHANNEL_REAL_NAME)
		    .replaceAll("%VIDEOLINK%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix") + vid));
	    if (engine == null) {
		System.err.println(out);
	    } else {
		engine.amsg(out);
	    }
	}
    }

    public void setSyncing(boolean v) {
	this.isSyncing = v;
    }

    public String getName() {
	return this.CHANNEL_ID;
    }

    @Override
    public long timeTilUpdate() {
	return (lastUpdate + Long.valueOf(engine.config.getProperty("youtubeUpdateDelay"))) - System.currentTimeMillis();
    }

    @Override
    public void stop() {
	this.isRunning = false;
    }

    @Override
    public void start() {
	if (this.isRunning) {
	    return;
	}
	this.isRunning = true;
	this.startedAt = System.currentTimeMillis();
	if (engine != null) {
	    engine.log("YouTube Announcer Thread " + this.CHANNEL_ID + " (" + this.CHANNEL_REAL_NAME + ") is starting", "YouTube");
	}
	this.thread = new Thread(this, "YouTube Announcer Thread (" + this.CHANNEL_ID + " [" + this.CHANNEL_REAL_NAME + "])");
	this.thread.start();
    }

    @Override
    public void startIfNotRunning() {
	if (!this.isRunning) {
	    this.start();
	}
    }

    @Override
    public void removeCache() {
    }

    @Override
    public void stopIfRunning() {
	if (this.isRunning) {
	    this.stop();
	}
    }

    @Override
    public void shouldUpdate(boolean b) {
    }

    @Override
    public boolean isDead() {
	return false;
    }

    @Override
    public String getThreadName() {
	return "YouTube_Update-" + this.CHANNEL_ID;
    }

    @Override
    public long getStartupTime() {
	return startedAt;
    }

    @Override
    public Throwable getLastExeption() {
	return lastException;
    }

    @Override
    public long getLastExceptionTime() {
	return lastExceptionTime;
    }

    @Override
    public long getLastUpdateTime() {
	return lastUpdate;
    }

    @Override
    public boolean addYTUser(String s, boolean b) {
	return true;
    }

}
