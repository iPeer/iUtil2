package com.simple.ipeer.iutil2.irc.protocol;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.CommandNotFoundException;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.ial.User;
import com.simple.ipeer.iutil2.twitter.Twitter;
import com.simple.ipeer.iutil2.youtube.YouTube;
import java.io.FileWriter;
import java.util.regex.Matcher;

public class Protocol {
    
    public Protocol () {	}
    
    public void handleDisconnect(Main engine, String message)  {
	engine.log("Disconnected! "+message);
	if (engine.REQUESTED_QUIT)
	    engine.terminate();
	else
	    engine.reconnect();
    }
    
    public static void main(String[] args) {
	Protocol p = new Protocol();
	p.parse(":iPeer!iPeer@13.33.33.37 PRIVMSG #Peer.Dev :https://www.youtube.com/watch?v=hDwk6tC7BPU", null);
    }
    
    public void parse(String line, final Main engine) {
	// We log the whole line for debug purposes
	if (engine != null && (!line.startsWith("PING ")/* && engine.config.getProperty("debug").equals("true")*/))
	    engine.log("<- "+line, "IRC");
	
	
	// When the server PINGs us, we need to make sure we reply.
	if (line.startsWith("PING "))
	    engine.send("PONG "+line.substring(5), engine.config.getProperty("debug").equals("true"));
	
	// Handle being disconnected by the server
	else if (line.startsWith("ERROR :")) {
	    handleDisconnect(engine, line.substring(7));
	}
	
	/* CONNECTION RELATED STUFF */
	
	else if (line.split(" ")[1].equals("001")) // Server address
	    engine.CURRENT_SERVER = line.split(" ")[0].substring(1);
	
	else if (line.split(" ")[1].equals("010")) { // "please use this server instead"
	    engine.log("The server is asking us to use a differet address and/or port.", "Protocol");
	    String[] a = line.split(" ");
	    String s = a[3];
	    String p = a[4];
	    engine.log(String.format("Switching to %s:%s on request of the server.", s, p), "Protocol");
	    engine.config.put("server", s);
	    engine.config.put("port", p);
	    engine.log("SSL has been disabled as it is not known if the port specified maps to SSL.", "Protocol");
	    engine.config.put("ssl", "false");
//	    engine.REQUESTED_QUIT = false;
//	    handleDisconnect(engine, "Using alternate server/port combo as per server's request.");
	}
	
	else if (line.split(" ")[1].equals("005")) {	// Network settings
	    if (engine.NETWORK_SETTINGS == null)
		engine.NETWORK_SETTINGS = new HashMap<String, String>();
	    String[] a = line.split(" ");
	    for (int x = 3; x < a.length; x++) {
		if (a[x].contains("=")) {
		    String[] b = a[x].split("=");
		    engine.NETWORK_SETTINGS.put(b[0], b[1]);
		}
	    }
	}
	
	else if (line.split(" ")[1].equals("004")) {
	    try {
		engine.send(engine.config.getProperty("identificationString").replaceAll("%PASSWORD%", new String(engine.readPassword())), false /* We have to remember not to log this line because passwords. */);
	    }
	    catch (RuntimeException e) {
		engine.log("Cannot authenticate with server: "+e.getMessage());
	    }
	    engine.send("MODE "+engine.getIAL().getCurrentNick()+" "+engine.config.getProperty("connectModes"));
	}
	
	else if (line.split(" ")[1].equals("433")) {
	    String newNick = "";
	    try {
		int n = Integer.valueOf(engine.getIAL().getCurrentNick().substring(engine.getIAL().getCurrentNick().length() - 1));
		newNick = engine.getIAL().getCurrentNick().substring(0, engine.getIAL().getCurrentNick().length() - 1)+n++;
	    }
	    catch (NumberFormatException e) {
		newNick = engine.getIAL().getCurrentNick()+"2";
	    }
	    engine.log("Nick is in use, trying "+newNick);
	    engine.changeNick(newNick);
	}
	
	else if (line.split(" ")[1].equals("251")) {
	    engine.CURRENT_NETWORK = engine.NETWORK_SETTINGS.get("NETWORK");
	    if (engine.CURRENT_NETWORK == null || engine.CURRENT_NETWORK.equals(""))
		engine.CURRENT_NETWORK = "UNKNOWN";
	    engine.log("Connected to the "+engine.CURRENT_NETWORK+" network on server "+engine.CURRENT_SERVER);
	    String[] chans = engine.config.getProperty("autoJoin").split(",");
	    for (String a : chans)
		engine.joinChannel(a);
	    
	    // After joining channels we can start any announcers we may have.
	    for (AnnouncerHandler ah : engine.announcers.values())
		ah.startAll();
	    
	    
	    // Send any messages that were queued while the bot was disconnected.
	    if (!engine.offlineMessages.isEmpty())
		engine.sendQueuedMessages();
	}
	
	/* END CONNECTION RELATED STUFF */
	
	//Handle invites
	else if (line.split(" ")[1].equals("INVITE")) {
	    engine.joinChannel(line.split(" ")[3].substring(1));
	}
	
	// Handle actual chat messages
	else if (Arrays.asList("PRIVMSG", "NOTICE").contains(line.split(" ")[1])) {
	    if (engine != null)
		engine.getProfiler().start("Chat");
	    String nick = "";
	    String address = nick;
	    String channel = nick;
	    String[] data = line.split(" ");
	    channel = data[2];
	    if (data[0].contains("!")) {
		nick = data[0].split("!")[0].substring(1);
		address = data[0].split("!")[1].split(" ")[0];
		engine.getIAL().updateAddressIfNeeded(channel, nick, address);
	    }
	    else {
		nick = data[0].substring(1);
		address = nick;
	    }
	    if (engine != null && channel.equals(engine.getIAL().getCurrentNick()))
		channel = nick;
	    
	    String message = Matcher.quoteReplacement(line.substring((line.indexOf(" :") + 2), line.length()));
	    
	    // CTCPs
	    
	    if (message.startsWith("")) {
		engine.getProfiler().start("CTCP");
		String ctcpType = message.split(" ")[0].substring(1).replaceAll("", "");
		if (ctcpType.equals("PING"))
		    engine.send("NOTICE "+nick+" :PING "+message.substring(6));
		if (ctcpType.equals("TIME"))
		    engine.send("NOTICE "+nick+" :TIME "+new SimpleDateFormat("dd/MM/yy HH:mm:ss Z").format(System.currentTimeMillis()));
		if (ctcpType.equals("VERSION"))
		    engine.send("NOTICE "+nick+" :VERSION iUtil 2 version "+engine.BOT_VERSION+" Java: "+System.getProperty("sun.arch.data.model")+"-bit "+System.getProperty("java.version")+" ("+System.getProperty("os.name")+")");
		engine.getProfiler().end();
	    }
	    
	    // Commands
	    
	    if ((engine == null ? "@#.!" : engine.config.getProperty("commandCharacters")).contains(message.substring(0, 1))) {
		engine.getProfiler().start("Commands");
		String sendPrefix = ((engine == null ? "#@" : engine.config.getProperty("publicCommandCharacters")).contains(message.substring(0, 1)) ? "PRIVMSG "+channel : "NOTICE "+nick);
//		String commandPrefix = message.substring(0, 1);
//		boolean isAdmin = engine == null || engine.getIAL().userHasModes(nick, engine.config.getProperty("debugChannel"), "o");
		String commandName = message.split(" ")[0].substring(1).toLowerCase();
		
		try {
		    User userObject = engine.getIAL().getFirstValidOrNewUserObject(nick);
		    if (message.split(commandName+" ").length > 1)
			engine.getCommandManager().executeCommand(commandName, userObject, line, sendPrefix, message.split(commandName+" ")[1]);
		    else
			engine.getCommandManager().executeCommand(commandName, userObject, line, sendPrefix);
		
		} catch (CommandException e) {
		    engine.sendCommandHelpReply(engine.getIAL().getUser(channel.toLowerCase(), nick), sendPrefix, e.getMessage());
		}
		catch (InsufficientPermissionsException e) {
		    engine.send(sendPrefix+" :You do not have the correct permissions to use this command.");
		}
		catch (CommandNotFoundException e) {
		    // Silence this so we don't spam users with error messages should their messages start with any of the command characters.
		}
		
		engine.getProfiler().end();
	    }
	    
	    // YouTube Playlists 
	    
	    /*else if (message.matches(".*https?://(www.)?youtube.com/playlist\\?(p=||list=).*")) {
		String playlistID = message.split("playlist\\?(p|list)=")[1].split("[\\.,!\"£\\$%\\^'@~/\\\\\\+\\*& ]")[0];
		HashMap<String, String> plData = new HashMap<String, String>();
		
		if (!playlistID.equals("") && !(plData = (engine == null ? new YouTube(null) : ((YouTube)engine.getAnnouncers().get("YouTube"))).getPlaylistInfo(playlistID.split("[&\\?#]")[0])).isEmpty()) {
		    String plOut = "";
		    if (plData.containsKey("error"))
			plOut = plData.get("error");
		    else
			plOut = "%C1%[%C2%"+plData.get("playlistAuthor")+"%C1%]%C2% "+plData.get("playlistName")+" %C1%(%C2%"+plData.get("videoCount")+" videos%C1%) %DASH%%C2% https://www.youtube.com/playlist?p="+plData.get("playlistID");
		    engine.send("PRIVMSG "+channel+" :"+plOut);
		}
		
	    }*/
	    
	    // YouTube Links
	    
	    else if (message.matches((engine == null ? ".*https?://(www.)?youtu(be.com/watch.*(?=(\\?v=|&v=))|.be/.*).*" : engine.config.getProperty("youtubeLinkRegex"))) && !nick.startsWith("iUtil")) {
		engine.getProfiler().start("YouTubeLinks");
		int maxVids = (engine == null ? 2 : Integer.valueOf(engine.config.getProperty("youtubeMaxProcessedLinks")));
		int curVid = 1;
		String[] vids = message.split("(.be/|v=)");
		String[] playlists = message.split("[\\?&]list=");
		for (int vn = 1; vn < vids.length && curVid++ <= maxVids; vn++) {
		    String videoid = "";
		    String playlistID = "";
		    try {
			playlistID = playlists[vn].split("[\\.,!\"£\\$%\\^'@~/\\\\\\+\\*& ]")[0];
		    } catch (ArrayIndexOutOfBoundsException e) { }
		    try {
			videoid = vids[vn].split("[\\.,!\"£\\$%\\^'@~/\\\\\\+\\*& ]")[0];
		    } catch (ArrayIndexOutOfBoundsException e) { continue; }
		    if (engine == null)
			System.err.println(videoid);
		    HashMap<String, String> ytdata = new HashMap<String, String>();
		    //HashMap<String, String> plData = new HashMap<String, String>();
		    try {
			ytdata = (engine == null ? new YouTube(null) : ((YouTube)engine.getAnnouncers().get("YouTube"))).getVideoInfo(videoid.split("[&\\?#]")[0]);
			//String plOut = "";
			String out = (engine == null ? "%C1%[%C2%%USER%%C1%] %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] (%C2%%VIEWS%%C1% views, %C2%%COMMENTS%%C1% comments, %C2%%LIKES%%C1% likes, %C2%%DISLIKES%%C1% dislikes) %DASH% %VIDEOURL%" : engine.config.getProperty("youtubeInfoFormat"))
				.replaceAll("%USER%", ytdata.get("author"))
				.replaceAll("%(VIDEO)?TITLE%", Matcher.quoteReplacement(ytdata.get("title")))
				.replaceAll("%(VIDEO)?LENGTH%", ytdata.get("duration"))
				.replaceAll("%VIEWS%", ytdata.get("views"))
				.replaceAll("%COMMENTS%", ytdata.get("comments"))
				.replaceAll("%LIKES%", ytdata.get("likes"))
				.replaceAll("%DISLIKES%", ytdata.get("dislikes"))
				.replaceAll("%(VIDEO)?URL%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix"))+videoid);
			/*if (!playlistID.equals("") && !playlistID.startsWith("WL")) {
			    plData = (engine == null ? new YouTube(null) : ((YouTube)engine.getAnnouncers().get("YouTube"))).getPlaylistInfo(playlistID.split("[&\\?#]")[0]);
			    if (plData.containsKey("error"))
				plOut = plData.get("error");
			    else
				plOut = "%C1%Playlist: %C2%"+plData.get("playlistName")+" %C1%by %C2%"+plData.get("playlistAuthor")+" %C1%(%C2%"+plData.get("videoCount")+" videos%C1%) %DASH%%C2% https://www.youtube.com/playlist?p="+plData.get("playlistID");
			}*/
			if (engine != null)
			    engine.send("PRIVMSG "+channel+" :"+out);
			else
			    System.err.println(out.replaceAll("%C([0-9]+)?%", ""));
			if (ytdata.containsKey("description")) {
			    String desc = "";
			    if ((engine == null ? "word" : engine.config.getProperty("youtubeDescriptionClippingMode")).equals("word")) {
				String[] descData = ytdata.get("description").split(" ");
				for (int x = 0; x < descData.length && desc.length() < Integer.valueOf((engine == null ? "140" : engine.config.getProperty("youtubeDescriptionLengthLimit"))); x++) {
				    desc = desc+(desc.length() > 0 ? " " : "")+descData[x];
				}
			    }
			    else
				desc = ytdata.get("description").substring(0, Integer.valueOf((engine == null ? "140" : engine.config.getProperty("youtubeDescriptionLengthLimit"))));
			    String description = (engine == null ? "%C1%Description: %C2%%DESCRIPTION%" : engine.config.getProperty("youtubeInfoFormatDescription")).replaceAll("%DESCRIPTION%", Matcher.quoteReplacement(desc)+(desc.length() < ytdata.get("description").length() ? "..." : ""));
			    if (engine == null)
				System.err.println(description.replaceAll("%C([0-9]+)?%", ""));
			    else
				engine.send("PRIVMSG "+channel+" :"+description);
			    /*if (!(plOut == null || plOut.equals("")))
							    if (engine == null)
				System.err.println(plOut.replaceAll("%C([0-9]+)?%", ""));
			    else
				engine.send("PRIVMSG "+channel+" :"+plOut);*/
			}
		    } catch (IOException e) {
			if (engine != null)
			    engine.send("PRIVMSG "+channel+" :Video not found: "+videoid);
			else
			    System.err.println("Video not found: "+videoid);
			engine.logError(e);
		    }
		    catch (Exception e) {
			if (engine != null)
			    engine.send("PRIVMSG "+channel+" :An error occured while attempting to retrieve info for video ID '"+videoid+"' ("+e.toString()+" at "+e.getStackTrace()[0]+")");
			else
			    System.err.println("An error occured while attempting to retrieve info for video ID '"+videoid+"' ("+e.toString()+" at "+e.getStackTrace()[0]+")");
			engine.logError(e);
		    }
		}
		engine.getProfiler().end();
	    }
	    
	    else if (message.matches(".*https?://(www.)?twitter.com/.*/status(es)?/.*")) { // Tweet links
		String[] tweetIDs = message.split("/status(es)?/");
		int maxTweets = 3;
		Twitter t = engine.getTwitter();
		for (int x = 1; (x + 1) <= tweetIDs.length && x <= maxTweets; x++) {
		    try {
			String id = tweetIDs[x].split("(/photo| )")[0];
			String tweet = t.getTweetData(id);
			engine.send("PRIVMSG "+channel+" :"+tweet);
		    }
		    catch (Throwable e) {
			engine.logError(e);
			engine.send("PRIVMSG "+channel+" :Couldn't get tweet data: "+e.toString());
		    }
		}
	    }
	    
	}
	
	
	else if (line.split(" ")[1].equals("NICK")) {
	    engine.getProfiler().start("Nicks");
	    String nick = line.split("!")[0].substring(1);
	    String newNick = line.split(" :")[1];
	    engine.getIAL().processNickChange(nick, newNick);
	    engine.getTell().sendMessages(newNick);
	    engine.getProfiler().end();
	}
	
	else if (line.split(" ")[1].equals("MODE")) {
	    engine.getProfiler().start("Modes");
	    String[] data = line.split(" ");
	    if (data.length > 3 && engine.NETWORK_SETTINGS.get("CHANTYPES").contains(data[2].substring(0, 1)) && data[0].contains("!"))
		try {
		    engine.getIAL().updateAddressIfNeeded(data[2], data[0].substring(1).split("!")[0], data[0].substring(1).split("!")[1]);
		} catch (ArrayIndexOutOfBoundsException e) {
		    engine.log("Could not update IAL entry!", "IAL");
		    String[] data2 = new String[data.length + 1];
		    data2[0] = line;
		    int x = 1;
		    for (String a1 : data)
			data2[x++] = a1;
		    engine.logError(e, "IAL", data2);
		}
	    if (data.length < 5) {
		engine.getProfiler().end();
		return;
	    }
	    engine.getIAL().parseModes(data[2], data[3], line.split(data[3].replaceAll("\\+", "\\\\\\+")+" ")[1]);
	    engine.getProfiler().end();
	    
	}
	
	
	else if (Arrays.asList("JOIN", "PART", "QUIT", "KICK").contains(line.split(" ")[1])) {
	    engine.getProfiler().start("Events");
	    String type = line.split(" ")[1];
	    String nick = line.split("!")[0].substring(1);
	    String channel = line.split(" ")[2].toLowerCase();
	    if (channel.startsWith(":"))
		channel = channel.substring(1);
	    if (type.equals("JOIN")) { // Why are channels in joins prefixed with colons but parts aren't?
		if (nick.equals(engine.getIAL().getCurrentNick()))
		    engine.getIAL().registerChannel(channel);
		else
		    engine.getTell().sendMessages(nick);
		User user = new User(nick);
		user.setAddressFromFull(line.split(" ")[0].substring(1));
		engine.getIAL().registerNick(channel, user);
	    }
	    else if (type.equals("QUIT")) {
		engine.getIAL().unregisterQuittingNick(nick);
	    }
	    else {
		if (!nick.equals(engine.getIAL().getCurrentNick()))
		    engine.getIAL().unregisterNick(channel, nick);
		else {
		    engine.getIAL().unregisterChannel(channel);
		}
	    }
	    engine.getProfiler().end();
	}
	
	//		else if (line.split(" ")[1].equals("352")) { // WHO
	//			engine.getProfiler().start("WHO");
	//			String[] a = line.split(" ");
	//			String channel = a[3].toLowerCase();
	//			String realName = line.split(":")[2];
	//			if (!engine.getChannelList().containsKey(channel)) {
	//				engine.log("Ignoring WHO data from "+channel+" because we're not in it.");
	//				return;
	//			}
	//			User b = new User(a[4], a[5], a[6], a[7], a[8], realName);
	//			b.setUpdateTime(System.currentTimeMillis());
	//			engine.getChannelList().get(channel).getUserList().put(a[7], b);
	//			//engine.getTell().sendMessages(a[7]);
	//			engine.getProfiler().end();
	//		}
	
	else if (line.split(" ")[1].equals("353")) { // NAMES
	    String[] data = line.split(" :")[1].split(" ");
	    String channel = line.split(" ")[4];
	    String[] modeMap = engine.NETWORK_SETTINGS.get("PREFIX").substring(1).replaceAll("\\+", "\\\\\\+").split("\\)");
	    for (String a : data) {
		String modes = "";
		String[] nickData = a.split("!");
		String nick = nickData[0].replaceAll("\\+", "\\\\\\+");
		for (int x = 0; modeMap[1].contains(String.valueOf(nick.charAt(x))); x++) {
		    modes += modeMap[0].charAt(modeMap[1].indexOf(nick.charAt(x)));
		    nick = nick.substring(1);
		}
		User user = new User(nick);
		if (!modes.equals(""))
		    user.setModes(modes);
		if (nickData.length > 1) {
		    user.setAddress(nickData[1].split("@")[1]);
		    user.setIdent(nickData[1].split("@")[0]);
		}
		engine.getIAL().registerNick(channel, user);
		engine.getTell().sendMessages(nick);
	    }
	}
	
	if (engine != null)
	    engine.getProfiler().end();
	
    }
    
    private void listThreads(ThreadGroup parent, String i, FileWriter fw) throws IOException {
	try {
	    fw.write(i + "Group[" + parent.getName() + ":" + parent.getClass()+"]\n");
	    int a = parent.activeCount();
	    Thread[] b = new Thread[a*2 + 10];
	    a = parent.enumerate(b, false);
	    
	    for (int x = 0; x < a; x++) {
		Thread t = b[x];
		fw.write(i+" Thread["+t.getName()+" : "+t.getClass()+"]\n");
	    }
	    
	    int c = parent.activeGroupCount();
	    ThreadGroup[] g = new ThreadGroup[c*2 + 10];
	    c = parent.enumerate(g, false);
	    
	    for (int x = 0; x < 5; x++) {
		listThreads(g[x], i+" ", fw);
	    }
	}
	catch (NullPointerException e) { }
	
    }
    
}
