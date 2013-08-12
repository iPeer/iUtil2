package com.simple.ipeer.iutil2.irc.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.User;
import com.simple.ipeer.iutil2.youtube.YouTube;

public class Protocol {

	public Protocol () {	}

	public void handleDisconnect(Main engine, String message)  {
		engine.isConnected = false;
		engine.log("Disconnected! "+message);
		if (engine.REQUESTED_QUIT)
			System.exit(0);
		else
			engine.reconnect();
	}

	public static void main(String[] args) {
		Protocol p = new Protocol();
		p.parse(":iPeer!iPeer@13.33.33.37 PRIVMSG #Peer.Dev :http://youtube.com/watch?v=&v=&v=&v=", null);
	}

	public void parse(String line, Main engine) {
		// We log the whole line for debug purposes
		if (engine != null)
			engine.log("<- "+line, "IRC");


		// When the server PINGs us, we need to make sure we reply.
		if (line.startsWith("PING "))
			engine.send("PONG "+line.substring(5));

		// Handle being disconnected by the server
		else if (line.startsWith("ERROR :")) {
			handleDisconnect(engine, line.substring(7));
		}

		//Handle invites
		else if (line.split(" ")[1].equals("INVITE")) {
			String inviteChannel = line.split(" ")[3].substring(1);
			engine.joinChannel(inviteChannel);
		}

		// Handle actual chat messages
		else if (Arrays.asList("PRIVMSG", "NOTICE").contains(line.split(" ")[1])) {
			String nick = "";
			String address = nick;
			String channel = nick;
			String[] data = line.split(" ");
			if (data[0].contains("!")) {
				nick = data[0].split("!")[0].substring(1);
				address = data[0].split("!")[1].split(" ")[0];
			}
			else {
				nick = data[0].substring(1);
				address = nick;
			}
			channel = data[2];
			if (engine != null && channel.equals(engine.CURRENT_NICK))
				channel = nick;
			String[] messageData = line.split(":");
			String message = "";
			for (int x = 2; x < messageData.length; x++) {
				message = message+":"+messageData[x];
			}
			message = message.substring(1);	

			// CTCPs

			if (message.startsWith("")) {
				String ctcpType = message.split(" ")[0].substring(1).replaceAll("", "");
				if (ctcpType.equals("PING"))
					engine.send("NOTICE "+nick+" :PING "+message.substring(6));
				if (ctcpType.equals("TIME"))
					engine.send("NOTICE "+nick+" :TIME "+new SimpleDateFormat("dd/MM/yy HH:mm:ss Z").format(System.currentTimeMillis()));
				if (ctcpType.equals("VERSION"))
					engine.send("NOTICE "+nick+" :VERSION iUtil 2 version "+engine.BOT_VERSION+" Java: "+System.getProperty("sun.arch.data.model")+"-bit "+System.getProperty("java.version")+" ("+System.getProperty("os.name")+")");
			}

			// YouTube Links

			if (message.matches((engine == null ? ".*https?://(www.)?youtu(be.com/watch.*(?=(\\?v=|&v=))|.be/.*).*" : engine.config.getProperty("youtubeLinkRegex"))) && !nick.startsWith("iUtil")) {
				int maxVids = (engine == null ? 2 : Integer.valueOf(engine.config.getProperty("youtubeMaxProcessedLinks")));
				int curVid = 1;
				String[] vids = message.split("(.be/|v=)");
				for (int vn = 1; vn < vids.length && curVid++ <= maxVids; vn++) {
					String videoid = "";
					try {
						videoid = vids[vn].split("[& ]")[0];
					} catch (ArrayIndexOutOfBoundsException e) { continue; }
					if (engine == null)
						System.err.println(videoid);
					HashMap<String, String> ytdata = new HashMap<String, String>();
					try {
						ytdata = (engine == null ? new YouTube(null) : ((YouTube)engine.getAnnouncers().get("YouTube"))).getVideoInfo(videoid);
						String out = (engine == null ? "%C1%[%C2%%USER%%C1%] %C2%%VIDEOTITLE% %C1%[%C2%%VIDEOLENGTH%%C1%] (%C2%%VIEWS%%C1% views, %C2%%COMMENTS%%C1% comments, %C2%%LIKES%%C1% likes, %C2%%DISLIKES%%C1% dislikes) %DASH% %VIDEOURL%" : engine.config.getProperty("youtubeInfoFormat"))
								.replaceAll("%USER%", ytdata.get("author"))
								.replaceAll("%(VIDEO)?TITLE%", ytdata.get("title"))
								.replaceAll("%(VIDEO)?LENGTH%", ytdata.get("duration"))
								.replaceAll("%VIEWS%", ytdata.get("views"))
								.replaceAll("%COMMENTS%", ytdata.get("comments"))
								.replaceAll("%LIKES%", ytdata.get("likes"))
								.replaceAll("%DISLIKES%", ytdata.get("dislikes"))
								.replaceAll("%(VIDEO)?URL%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix"))+videoid);
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
							String description = (engine == null ? "%C1%Description: %C2%%DESCRIPTION%" : engine.config.getProperty("youtubeInfoFormatDescription")).replaceAll("%DESCRIPTION%", desc+(desc.length() < ytdata.get("description").length() ? "..." : ""));
							if (engine == null)
								System.err.println(description.replaceAll("%C([0-9]+)?%", ""));
							else
								engine.send("PRIVMSG "+channel+" :"+description);			
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
			}


			// Commands

			if ((engine == null ? "@#.!" : engine.config.getProperty("commandCharacters")).contains(message.substring(0, 1))) {
				String sendPrefix = ((engine == null ? "#@" : engine.config.getProperty("publicCommandCharacters")).contains(message.substring(0, 1)) ? "PRIVMSG "+channel : "NOTICE "+nick);
				String commandPrefix = message.substring(0, 1);
				boolean isAdmin = (engine == null ? true : engine.getChannelList().get("#peer.dev").getUserList().get(nick).isOp());
				String commandName = message.split(" ")[0].substring(1).toLowerCase();

				if (commandName.equals("quit") && isAdmin) {
					String quitMessage = engine.config.getProperty("quitMessageFormat").replaceAll("%NICK%", nick).replaceAll("%ADDRESS%", address);
					engine.quit(quitMessage);
				}

				else if (commandName.equals("throwexception") && isAdmin) {
					engine.logError(new Exception("Forced debug exception"), "DEBUG");
				}
				
				else if (commandName.equals("reloadconfig") && isAdmin) {
					engine.send(sendPrefix+" :Attempting to reload config...");
					Properties oldConfig = engine.config; // In case it fails.
					try {
						engine.config.clear();
						engine.loadConfig();
						engine.send(sendPrefix+" :Reloaded config succesfully. Some changes may not take effect immediately.");
					} catch (Exception e) { 
						engine.config = oldConfig;
						engine.send(sendPrefix+" :Could reload config: "+e.toString()+" at "+e.getStackTrace()[0]);
						engine.logError(e);
					}
				}

				else if (commandName.equals("part") && isAdmin) {
					try {
						String partChannel = message.split(" ")[1];
						if (engine.getChannelList().containsKey(partChannel))
							engine.partChannel(partChannel, engine.config.getProperty("partMessageFormat").replaceAll("%NICK%", nick).replaceAll("%ADDRESS%", address));
						else
							engine.send(sendPrefix+" :I am not in that channel!");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.partChannel(channel, engine.config.getProperty("partMessageFormat").replaceAll("%NICK%", nick).replaceAll("%ADDRESS%", address));
					}
				}

				else if (commandName.equals("join") && isAdmin) {
					try {
						engine.joinChannel(message.split(" ")[1]);
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.send(sendPrefix+" :Invalid syntax! "+commandPrefix+commandName+" <channel>");
					}

				}

				else if (commandName.matches("(info(mation)?|status)")) {
					long totalMemory = Runtime.getRuntime().totalMemory();
					long freeMemory = Runtime.getRuntime().freeMemory();
					long usedMemory = totalMemory - freeMemory;
					List<String> out = new ArrayList<String>();
					out.add("Memory: "+(usedMemory / 1024L / 1024L)+"MB/"+(totalMemory / 1024L / 1024L)+"MB");
					String threads = "";
					for (String a : engine.getAnnouncers().keySet()) {
						AnnouncerHandler ah = engine.getAnnouncers().get(a);
						int ttu = (int)(ah.timeTilUpdate() / 1000L);
						int seconds = ttu % 60;
						int minutes = ttu / 60;
						int hours = (int)(Math.floor(minutes / 60));
						if (hours > 0)
							minutes -= hours*60;
						String time = (hours > 0 ? String.format("%02d", hours)+":" : "")+String.format("%02d", minutes)+":"+String.format("%02d", seconds);
						threads = threads+(threads.length() > 0 ? ", " : "")+a+": "+time;
					}
					out.add("Announcers (updating in): "+threads);
					out.add("Java: "+System.getProperty("sun.arch.data.model")+"-bit "+System.getProperty("java.version")+", C: "+System.getProperty("java.class.version")+" VM: "+System.getProperty("java.vm.version")+" / "+System.getProperty("java.vm.specification.version"));
					out.add("OS: "+System.getProperty("os.name")+" / "+System.getProperty("os.version"));
					out.add("Connection: "+engine.getConnection().toString());
					engine.send(sendPrefix, out, true, false);				
				}

				else if (commandName.equals("reconnect") && isAdmin) {
					engine.quit("RECONNECT requested by "+nick, true);
				}

				else if (commandName.equals("send") && isAdmin) {
					String toSend = message.substring(6);
					if (toSend.substring(0,  4).toLowerCase().equals("nick"))
						engine.changeNick(toSend.substring(5));
					else
						engine.send(message.substring(6));
				}

				else if (commandName.equals("config") && isAdmin) {
					String d[] = message.split(" ");
					if (d.length == 1) {
						engine.send(sendPrefix+" :Invalid syntax, must provide at least a config entry.");
						engine.send(sendPrefix+" :"+commandPrefix+commandName+" <entry> [value]");
					}
					else {
						String entry = d[1];
						if (engine.config.containsKey(entry)) {
							engine.disableFormatProcessing();
							if (d.length == 3) {
								String oldValue = engine.config.getProperty(entry);
								engine.config.put(entry, d[2]);
								engine.send(sendPrefix+" :Config entry "+entry+" has been changed from "+oldValue+" to "+d[2]+".");
								engine.saveConfig();
							}
							else {
								engine.send(sendPrefix+" :Config entry "+entry+" is current set as "+engine.config.getProperty(entry));
							}
							engine.enableFormatProcessing();
						}
						else {
							engine.send(sendPrefix+" :Config entry "+entry+" doesn't exist!");
						}
					}
				}

				else if (commandName.matches("addy(ou)?t(ube)?(user(name)?)?") && isAdmin) {
					try {
						String user = message.split(" ")[1];
						if (engine.getAnnouncers().get("YouTube").addUser(user))
							engine.send(sendPrefix+" :Now watching "+user+" for YouTube uploads.");
						else
							engine.send(sendPrefix+" :"+user+" is already being watched for uploads!");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.send(sendPrefix+" :Incorrect syntax, must supply a channel to add.");
						engine.send(sendPrefix+" :"+commandPrefix+commandName+" <channel>");
					}
				}
				else if (commandName.matches("(rem(ove)?|del(ete)?)y(ou)?t(ube)?(user(name)?)?") && isAdmin) {
					try {
						String user = message.split(" ")[1];
						if (engine.getAnnouncers().get("YouTube").removeUser(user))
							engine.send(sendPrefix+" :No longer watching "+user+" for YouTube uploads.");
						else
							engine.send(sendPrefix+" :"+user+" isn't being watched for uploads.");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.send(sendPrefix+" :Incorrect syntax, must supply a channel to remove.");
						engine.send(sendPrefix+" :"+commandPrefix+commandName+" <channel>");
					}
				}

				else if (commandName.matches("addtwitch(user(name)?)?") && isAdmin) {
					try {
						String user = message.split(" ")[1];
						if (engine.getAnnouncers().get("Twitch").addUser(user))
							engine.send(sendPrefix+" :Now watching "+user+" for streams.");
						else
							engine.send(sendPrefix+" :"+user+" is already being watched for streams!");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.send(sendPrefix+" :Incorrect syntax, must supply a channel to add.");
						engine.send(sendPrefix+" :"+commandPrefix+commandName+" <channel>");
					}
				}
				else if (commandName.matches("(rem(ove)?|del(ete)?)twitch(user(name)?)?") && isAdmin) {
					try {
						String user = message.split(" ")[1];
						if (engine.getAnnouncers().get("Twitch").removeUser(user))
							engine.send(sendPrefix+" :No longer watching "+user+" for streams.");
						else
							engine.send(sendPrefix+" :"+user+" isn't being watched for streams.");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						engine.send(sendPrefix+" :Incorrect syntax, must supply a channel to remove.");
						engine.send(sendPrefix+" :"+commandPrefix+commandName+" <channel>");
					}
				}


			}

		}

		else if (line.split(" ")[1].equals("NICK")) {
			String nick = line.split("!")[0].substring(1);
			String newNick = line.split(":")[2];
			for (Channel c : engine.getChannelList().values()) {
				if (c.getUserList().containsKey(nick)) {
					User a = c.getUserList().get(nick);
					User b = new User(a.identd, a.address, a.server, newNick, a.modes, a.realname);
					c.getUserList().remove(nick);
					c.getUserList().put(newNick, b);
				}
			}
		}

		else if (Arrays.asList("JOIN", "PART", "QUIT", "KICK").contains(line.split(" ")[1])) {
			String type = line.split(" ")[1];
			String nick = line.split("!")[0].substring(1);
			String channel = line.split(" ")[2].toLowerCase();
			if (channel.startsWith(":"))
				channel = channel.substring(1);
			if (type.equals("JOIN") && !nick.equals(engine.CURRENT_NICK)) { // Why are channels in joins prefixed with colons but parts aren't?
				engine.send("WHO +cn "+channel+" "+nick);
			}
			else if (type.equals("QUIT")) {
				for (Channel c : engine.getChannelList().values()) {
					if (c.getUserList().containsKey(nick)) {
						c.getUserList().remove(nick);
					}
				}
			}
			else {
				engine.getChannelList().get(channel).getUserList().remove(nick);
			}
		}

		else if (line.split(" ")[1].equals("352")) {
			String[] a = line.split(" ");
			String channel = a[3].toLowerCase();
			String realName = line.split(":")[2];
			if (!engine.getChannelList().containsKey(channel)) {
				engine.log("Ignoring WHO data from "+channel+" because we're not in it.");
				return;
			}
			User b = new User(a[4], a[5], a[6], a[7], a[8], realName);
			engine.getChannelList().get(channel).getUserList().put(a[7], b);
		}

		else if (line.split(" ")[1].equals("474")) {
			String channel = line.split(" ")[3].toLowerCase();
			engine.log("Unable to join channel "+channel+" (banned)");
			if (engine.getChannelList().containsKey(channel))
				engine.getChannelList().remove(channel);
		}

	}

}
