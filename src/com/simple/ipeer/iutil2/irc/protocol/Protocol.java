	package com.simple.ipeer.iutil2.irc.protocol;
	
	import java.io.IOException;
	import java.text.SimpleDateFormat;
	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Properties;
	
	import javax.xml.parsers.ParserConfigurationException;
	
	import org.xml.sax.SAXException;
	
	import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
	import com.simple.ipeer.iutil2.engine.Main;
	import com.simple.ipeer.iutil2.irc.ial.User;
	import com.simple.ipeer.iutil2.util.Filesize;
	import com.simple.ipeer.iutil2.youtube.YouTube;
	import com.simple.ipeer.iutil2.youtube.YouTubeSearchResult;
	
	public class Protocol {
	
		public Protocol () {	}
	
		public void handleDisconnect(Main engine, String message)  {
			engine.log("Disconnected! "+message);
			if (engine.REQUESTED_QUIT)
				System.exit(0);
			else
				engine.reconnect();
		}
	
		public static void main(String[] args) {
			Protocol p = new Protocol();
			p.parse(":iPeer!iPeer@13.33.33.37 PRIVMSG #Peer.Dev ::D", null);
		}
	
		public void parse(String line, Main engine) {
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
				engine.send(engine.config.getProperty("identificationString").replaceAll("%PASSWORD%", new String(engine.readPassword())), false /* We have to remember not to log this line because passwords. */);
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
	
				String message = line.split(" :")[1];
	
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
					String commandPrefix = message.substring(0, 1);
					boolean isAdmin = engine == null || engine.getIAL().userHasModes(nick, engine.config.getProperty("debugChannel"), "o");
					String commandName = message.split(" ")[0].substring(1).toLowerCase();
	
					if (commandName.matches("(q{3}|quit|die)") && isAdmin) {
						String quitMessage = engine.config.getProperty("quitMessageFormat").replaceAll("%NICK%", nick).replaceAll("%ADDRESS%", address);
						engine.quit(quitMessage);
					}
	
					else if (commandName.equals("throwexception") && isAdmin) {
						engine.logError(new Exception("Forced debug exception"), "DEBUG");
					}
	
					else if (commandName.matches("force(y(ou)?t(ube)?)?update") && isAdmin) {
						long start = System.currentTimeMillis();
						engine.send(sendPrefix+" :Updating all YouTube threads...");
						engine.getAnnouncers().get("YouTube").updateAll();
						engine.send(sendPrefix+" :Finished updating all YouTube threads. Update took "+(System.currentTimeMillis() - start)+"ms.");
					}
					else if (commandName.equals("forcetwitchupdate") && isAdmin) {
						long start = System.currentTimeMillis();
						engine.send(sendPrefix+" :Updating all Twitch threads...");
						engine.getAnnouncers().get("Twitch").updateAll();
						engine.send(sendPrefix+" :Finished updating all Twitch threads. Update took "+(System.currentTimeMillis() - start)+"ms.");
					}
	
					else if (commandName.equals("profiler")) {
						if (message.split(" ").length > 1) {
							String parameter = message.split(" ")[1];
							if (engine.getProfiler().profileData().containsKey(parameter))
								engine.send(sendPrefix+" :Last iteration of '%B%"+parameter+"%B%' took "+engine.getProfiler().profileData().get(parameter) / 1000000.0D+ "ms.");
							else 
								engine.send(sendPrefix+" :'%B%"+parameter+"%B%' has been iterated upon yet.");
						}
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
							if (engine.getIAL().getChannelList().containsKey(partChannel))
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
	
					else if (commandName.matches("(info(r?mation)?|status)") && isAdmin) {
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
						out.add("IRC Traffic (estimate): Sent: "+Filesize.calculate(engine.getBytesSent())+", Received: "+Filesize.calculate(engine.getBytesReceived()));
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
								if (d.length >= 3) {
									String oldValue = engine.config.getProperty(entry);
									String newValue = "";
									for (int x = 2; x < d.length; x++)
										newValue = newValue+(newValue.length() > 0 ? " " : "")+d[x];
									engine.config.put(entry, newValue);
									engine.send(sendPrefix+" :Config entry "+entry+" has been changed from "+oldValue+" to "+newValue+".");
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
	
					else if (commandName.matches("^(memo|tell)")) {
						if (message.split(" ").length < 3) {
							engine.send(sendPrefix+" :You need to provide a nick to send the message to and the message you want to send them.");
							engine.send(sendPrefix+" :"+commandPrefix+commandName+" <nick> <message>");
						}
						else {
							String n = message.split(commandName+" ")[1].split(" ")[0];
							String msg = message.split(n+" ")[1];
							if (engine.getIAL().isOnChannel(n, channel) && !n.equals(nick))
								engine.send(sendPrefix+" :Why not tell them yourself? They're on this very channel right now!");
							else {
								try {
									engine.getTell().addMessage(nick, n, msg);
									engine.send(sendPrefix+" :"+(n.equals(nick) ? "Trying to send a message to your future self eh, "+nick+"? Very well, it is done." : "I'll be sure to tell "+n+" that next time I see them, "+nick+"."));
								}
								catch (RuntimeException e) {
									engine.send(sendPrefix+" :"+e.getMessage());
								}
							}
						}
	
					}
	
					else if (commandName.matches("(cancel|del(ete)?|remove)(memo|tell)")) {
						if (message.split(" ").length < 2) {
							engine.send(sendPrefix+" :You need to provide a nick of whom you wish to cancel your last message for.");
							engine.send(sendPrefix+" :"+commandPrefix+commandName+" <nick>");
						}
						else {
							String n = message.split(commandName+" ")[1].split(" ")[0];
							try {
								engine.getTell().cancelMessage(nick, n);
								engine.send(sendPrefix+" :You last message to "+n+" has been cancelled.");
							}
							catch (RuntimeException e) {
								engine.send(sendPrefix+" :"+e.getMessage());
							}
						}
					}
	
					else if (commandName.matches("y(ou)?t(ube)?(search)?")) {
						engine.getProfiler().start("YTSearch");
						try {
							String query = message.split(commandName+" ")[1];
							List<YouTubeSearchResult> results = (engine == null ? new YouTube(null) : (YouTube)engine.getAnnouncers().get("YouTube")).getSearchResults(query);
							int result = 0;
							for (YouTubeSearchResult r : results) {
								result++;
								String out = engine.config.getProperty("youtubeSearchFormat")
										.replaceAll("%RESULT%", Integer.toString(result))
										/*.replaceAll("%(TOTAL)?RESULTS%", Integer.toString(r.getTotalResults()))*/
										.replaceAll("%(USER|(VIDEO)?AUTHOR)%", r.getAuthor())
										.replaceAll("%(VIDEO)?TITLE%", r.getTitle())
										.replaceAll("%(VIDEO)?LENGTH%", r.getFormattedLength())
										.replaceAll("%VIEWS%", Integer.toString(r.getViews()))
										.replaceAll("%COMMENTS%", Integer.toString(r.getComments()))
										.replaceAll("%LIKES%", Integer.toString(r.getLikes()))
										.replaceAll("%DISLIKES%", Integer.toString(r.getDislikes()))
										.replaceAll("%(VIDEO)?URL%", (engine == null ? "https://youtu.be/" : engine.config.getProperty("youtubeURLPrefix"))+r.getID());
								engine.send(sendPrefix+" :"+out);
								if (engine.config.getProperty("youtubeSearchDescriptions").equals("true") && r.hasDescription())
									engine.send(sendPrefix+" :"+engine.config.getProperty("youtubeInfoFormatDescription").replaceAll("%DESCRIPTION%", r.getDescription()));
							}
						}
						catch (ArrayIndexOutOfBoundsException e) {
							engine.send(sendPrefix+" :You must provide a search query!");
							engine.send(sendPrefix+" :"+commandPrefix+commandName+" <query>");
						}
						catch (RuntimeException e) { 
							engine.send(sendPrefix+" :"+e.getMessage());
						}
	
						catch (SAXException | IOException | ParserConfigurationException e) {
							engine.logError(e);
							engine.send(sendPrefix+" :Couldn't get search results because an error occured. Please inform iPeer of this error:");
							engine.send(sendPrefix+" :"+e.toString()+" at "+e.getStackTrace()[0]);
						}
	
						engine.getProfiler().end();
	
					}
	
					else if (commandName.matches("myinfo|whoami")) {
						engine.send(sendPrefix+" :You are "+engine.getIAL().getUser(channel, nick).getFullAddress()+"."+(engine.getIAL().getUser(channel, nick).getModes().equals("") ? "" : " You have modes +"+engine.getIAL().getUser(channel, nick).getModes()));
					}
	
					else if (commandName.matches("yourinfo|whoareyou") && isAdmin) {
						engine.send(sendPrefix+" :I am "+engine.getIAL().getUser(channel, engine.getIAL().getCurrentNick()).getFullAddress()+"."+(engine.getIAL().getUser(channel, engine.getIAL().getCurrentNick()).getModes().equals("") ? "" : " I have modes +"+engine.getIAL().getUser(channel, engine.getIAL().getCurrentNick()).getModes()));
					}
	
					engine.getProfiler().end();
				}
	
				// YouTube Links
	
				else if (message.matches((engine == null ? ".*https?://(www.)?youtu(be.com/watch.*(?=(\\?v=|&v=))|.be/.*).*" : engine.config.getProperty("youtubeLinkRegex"))) && !nick.startsWith("iUtil")) {
					engine.getProfiler().start("YouTubeLinks");
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
							ytdata = (engine == null ? new YouTube(null) : ((YouTube)engine.getAnnouncers().get("YouTube"))).getVideoInfo(videoid.split("[&\\?#]")[0]);
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
					engine.getProfiler().end();
				}
	
			}
	
			else if (line.split(" ")[1].equals("NICK")) {
				engine.getProfiler().start("Nicks");
				String nick = line.split("!")[0].substring(1);
				String newNick = line.split(":")[2];
				engine.getIAL().processNickChange(nick, newNick);
				engine.getTell().sendMessages(newNick);
				engine.getProfiler().end();
			}
	
			else if (line.split(" ")[1].equals("MODE")) {
				engine.getProfiler().start("Modes");
				String[] data = line.split(" ");
				if (data.length > 3 && engine.NETWORK_SETTINGS.get("CHANTYPES").contains(data[2].substring(0, 1)))
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
	
	}
