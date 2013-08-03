package com.simple.ipeer.iutil2.irc.protocol;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.User;

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
			if (channel.equals(engine.CURRENT_NICK))
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


			// Commands

			if (engine.config.getProperty("commandCharacters").contains(message.substring(0, 1))) {
				String sendPrefix = (engine.config.getProperty("publicCommandCharacters").contains(message.substring(0, 1)) ? "PRIVMSG "+channel : "NOTICE "+nick);
				String commandPrefix = message.substring(0, 1);
				boolean isAdmin = engine.getChannelList().get("#peer.dev").getUserList().get(nick).isOp();
				String commandName = message.split(" ")[0].substring(1).toLowerCase();

				if (commandName.equals("quit") && isAdmin) {
					String quitMessage = engine.config.getProperty("quitMessageFormat").replaceAll("%NICK%", nick).replaceAll("%ADDRESS%", address);
					engine.quit(quitMessage);
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
