package com.simple.ipeer.iutil2.youtube;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Debuggable;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.util.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author iPeer
 */
public class YouTube implements AnnouncerHandler, Debuggable, DebuggableSub {

    protected YouTube instance;
    private Main engine;
    private List<Announcer> announcerList = new ArrayList<Announcer>();
    private HashMap<String, YouTubeChannel> CHANNEL_LIST = new HashMap<String, YouTubeChannel>();
    private File channelFileDir;
    private long lastUpdateTime = 0L;
    private long lastExceptionTime = 0L;
    private Throwable lastException;
    private LinkedList<YouTubeChannel> waitingToSync = new LinkedList<YouTubeChannel>();
    private boolean isSyncing = false;

    public YouTube(Main main) {

	this.engine = main;
	this.instance = this;

	if (this.engine != null) {
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
	    settings.put("youtubeGetIDRegex", "(?<=https?://(www.)?youtu(be.com|.be)/(watch\\?v=)?)[^=]*?( |$)"); // Deprecated
	    settings.put("youtubeDescriptionLengthLimit", "140");

	    // Search settings
	    settings.put("youtubeSearchDescriptions", "false"); // Should descriptions be shown when a user searches for videos?
	    settings.put("youtubeMaxSearchResults", "1");
	    settings.put("youtubeSearchFormat", "%C1%[%C2%%AUTHOR%%C1%] %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] %DASH% %C2%%VIDEOURL%");
	    settings.put("youtubeCountryCode", "GB");

	    // Other settings
	    settings.put("youtubeRestartDeadThreads", "true");
	    settings.put("youtubeChannelUpdateTimeout", "5000");

	    engine.createConfigDefaults(settings);
	}

	File youtubeDir = new File((this.engine == null ? "./YouTube/" : engine.config.getProperty("youtubeDir")));
	File configDir = new File(youtubeDir, "/config/");
	File usrnFile = new File(configDir, "usernames.cfg");
	channelFileDir = new File(youtubeDir, "/Channels/");
	channelFileDir.mkdirs();

	// Does the usernames.cfg file exist? If so, we should load legacy channels into the new system (expensive on API calls though D:)
	if (usrnFile.exists()) {
	    this.loadLegacyChannels(usrnFile);
	} else {
	    this.loadChannels();
	}

    }

    public static void main(String[] args) {
	YouTube yt = new YouTube(null);

    }

    public final void loadLegacyChannels(File file) {
	Properties channels = new Properties();
	try {
	    channels.load(new FileInputStream(file));
	    String[] channelList = channels.getProperty("users").split(",");
	    for (String c : channelList) {
		YouTubeChannel ytc = new YouTubeChannel(c, this.engine, this, true); // Always assume this is a username
		ytc.setSyncing(true);
		//ytc.startIfNotRunning();
		announcerList.add(ytc);
		this.CHANNEL_LIST.put(ytc.getName(), ytc);
	    }
	    if (!file.delete()) {
		file.deleteOnExit();
	    }
	    if (this.engine == null) {
		System.err.println("Loaded legacy usernames!");
	    } else {
		engine.log("Loaded legacy usernames!", "YouTube");
	    }
	} catch (Exception e) {
	    this.lastException = e;
	    this.lastExceptionTime = System.currentTimeMillis();
	    if (this.engine == null) {
		System.err.println("Couldn't load legacy usernames!");
		e.printStackTrace();
	    } else {
		engine.log("Couldn't load legacy usernames!", "YouTube");
		engine.logError(e, "YouTube");
	    }
	}
    }

    public final void loadChannels() {
	try {
	    if (this.engine == null) {
		System.err.println("Loading channels...");
	    } else {
		engine.log("Loading channels...", "YouTube");
	    }
	    File[] files = this.channelFileDir.listFiles();
	    for (File f : files) {
		if (f.getName().equals(".") || f.getName().equals("..")) {
		    continue;
		} // NetBeans says this is unneeded, but testing suggests otherwise.
		engine.log("Loading channel data from '" + f + "'", "YouTube");
		JSONObject json = (JSONObject) JSONValue.parse(new FileReader(f));
		String channelID = json.get("channelID").toString();
		YouTubeChannel ytc = new YouTubeChannel(channelID, this.engine, this, false); // Should NEVER be a username!
		ytc.setSyncing(true);
		//ytc.startIfNotRunning();
		this.announcerList.add(ytc);
		this.CHANNEL_LIST.put(channelID, ytc);

	    }
	    if (this.engine == null) {
		System.err.println(this.announcerList.size() + " channel(s) loaded!");
	    } else {
		engine.log(this.announcerList.size() + " channel(s) loaded!", "YouTube");
	    }
	} catch (Throwable e) {
	    this.lastException = e;
	    this.lastExceptionTime = System.currentTimeMillis();
	    if (this.engine == null) {
		System.err.println("Couldn't load usernames!");
		e.printStackTrace();
	    } else {
		engine.log("Couldn't load usernames!", "YouTube");
		engine.logError(e, "YouTube");
	    }
	}
    }
    
    public void addToSync(YouTubeChannel channel) {
	if (!waitingToSync.contains((channel)))
	    waitingToSync.add(channel);
    }

    public YouTube getInstance() {
	return this.instance;
    }

    public String formatTime(String time) throws DatatypeConfigurationException {
	
	Duration dur = DatatypeFactory.newInstance().newDuration(time);
	
	String hours = pad(dur.getHours());
	String minutes = pad(dur.getMinutes());
	String secs = pad(dur.getSeconds());
	
	
	return (!hours.equals("00") ? hours+":" : "")+minutes+":"+secs;
	
	
	/*String hours = "";
	String min = "";
	if (tsd.length == 3) {
	    hours = tsd[0];
	    min = tsd[1];
	} else {
	    min = tsd[0];
	}
	
	String sec = tsd[tsd.length - 1];
	
	sec = pad(sec);
	min = pad(min);
	hours = pad(hours);
	
	String format = min + ":" + sec;
	
	if (!hours.equals("00") && !hours.equals("0")) {
	    format = hours + ":" + format;
	}
	
	return format;*/
	
    }

    private String pad(int what) {
	return String.format("%02d", what);
    }

    /*public String formatTime(String seconds) {
     return formatTime(Integer.valueOf(seconds));
     }

     public String formatTime(int len) {
     int seconds = len % 60;
     int minutes = len / 60;
     int hours = (int) Math.floor(minutes / 60);
     if (hours > 0) {
     minutes -= hours * 60;
     }
     return (hours > 0 ? String.format("%02d", hours) + ":" : "") + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
     }*/
    public void syncChannelsIfNotSyncing() {
	if (!waitingToSync.isEmpty() && !isSyncing) {
	    isSyncing = true;
	    Iterator<YouTubeChannel> it = waitingToSync.iterator();
	    while (it.hasNext()) {
		(it.next()).startIfNotRunning();
		it.remove();
	    }
	    isSyncing = false;
	}
    }

    @Override
    public boolean addYTUser(String channel, boolean isUsername) {
	try {
	    YouTubeChannel newChannel = new YouTubeChannel(channel, engine, this, isUsername);
	    this.CHANNEL_LIST.put(newChannel.getName(), newChannel);
	    this.waitingToSync.add(newChannel);
	    newChannel.setSyncing(true);
	    newChannel.update();
	    return true;
	} catch (Throwable e) {
	    engine.logError(e, "YouTube [ADD]", channel);
	    return false;
	}
    }

    @Override
    public boolean addUser(String name) {
	return addYTUser(name, true);
    }

    @Override
    public boolean removeUser(String name) {
	if (!this.CHANNEL_LIST.containsKey(name)) {
	    return false;
	}
	YouTubeChannel ytc = this.CHANNEL_LIST.get(name);
	ytc.stop();
	this.CHANNEL_LIST.remove(name);
	return true;
    }

    @Override
    public void updateAll() {
	for (YouTubeChannel ytc : this.CHANNEL_LIST.values()) {
	    try {
		ytc.update();
	    } catch (Throwable e) {
	    }
	}
    }

    @Override
    public void startAll() {
	for (YouTubeChannel ytc : this.CHANNEL_LIST.values()) {
	    ytc.startIfNotRunning();
	}
    }

    @Override
    public void stopAll() {
	for (YouTubeChannel ytc : this.CHANNEL_LIST.values()) {
	    ytc.stop();
	}
    }

    @Override
    public void update(String name) {
    }

    @Override
    public long timeTilUpdate() {
	return this.announcerList.iterator().next().timeTilUpdate();
    }

    @Override
    public long getUpdateDelay() {
	return (engine == null ? 600000 : Long.valueOf(engine.config.getProperty("youtubeUpdateDelay")));
    }

    @Override
    public void scheduleThreadRestart(Object channel) {
    }

    @Override
    public int getDeadThreads() {
	int x = 0;
	for (YouTubeChannel a : this.CHANNEL_LIST.values()) {
	    if (a.isDead()) {
		x++;
	    }
	}
	return x;
    }

    @Override
    public int getTotalThreads() {
	return this.announcerList.size();
    }

    @Override
    public List<Announcer> getAnnouncerList() {
	return this.announcerList;
    }

    @Override
    public void writeDebug(FileWriter fw) {
    }

    @Override
    public Throwable getLastExeption() {
	return this.lastException;
    }

    @Override
    public long getLastExceptionTime() {
	return this.lastExceptionTime;
    }

    @Override
    public long getLastUpdateTime() {
	return this.lastUpdateTime;
    }
    
    public HashMap<String,String> getVideoInfo(String id) throws IOException, DatatypeConfigurationException {
	String apiKey = Util.readEncrypted(new File("./YouTube/YouTubeAPIKey.iuc"));
	HttpsURLConnection _urlC = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/videos?part=snippet%2CcontentDetails%2Cstatistics&id=" + id + "&key=" + apiKey).openConnection();
	JSONObject _json = (JSONObject) JSONValue.parse(new InputStreamReader(_urlC.getInputStream()));
	JSONArray _jsonArray = (JSONArray) _json.get("items");

	HashMap<String, String> data = new HashMap<String, String>();

	for (int _x = 0; _x < _jsonArray.size(); _x++) {

	    JSONObject _jo = (JSONObject) _jsonArray.get(_x);

	    //System.out.println(_jo);
	    String videoID = _jo.get("id").toString();
	    JSONObject __jo = (JSONObject) _jo.get("snippet");
	    String channelName = __jo.get("channelTitle").toString();
	    String videoTitle = __jo.get("title").toString();
	    String videoDesc = __jo.get("description").toString();
	    String liveTags = __jo.get("liveBroadcastContent").toString();
	    __jo = (JSONObject) _jo.get("contentDetails");
	    String videoLength = __jo.get("duration").toString();
	    __jo = (JSONObject) _jo.get("statistics");
	    String likes = new DecimalFormat("#,###").format(Integer.valueOf(__jo.get("likeCount").toString()));
	    String dislikes = new DecimalFormat("#,###").format(Integer.valueOf(__jo.get("dislikeCount").toString()));
	    String views = new DecimalFormat("#,###").format(Integer.valueOf(__jo.get("viewCount").toString()));
	    String comments = "0";
	    try {
		comments = new DecimalFormat("#,###").format(Integer.valueOf(__jo.get("commentCount").toString()));
	    }
	    catch (Throwable _e) {}
	    data.put("author", channelName);
	    data.put("title", videoTitle);
	    data.put("duration", this.formatTime(videoLength));
	    if (videoDesc.length() > 0) 
		data.put("description", videoDesc);
	    data.put("likes", likes);
	    data.put("dislikes", dislikes);
	    data.put("views", views);
	    data.put("comments", comments);
	    data.put("liveData", liveTags);
	}
	return data;
    }

}
