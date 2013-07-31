package com.simple.ipeer.iutil2.irc.protocol;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.User;

public class Protocol {

	public Protocol () {	}

	public void handleDisconnect(Main engine, String message)  {
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

	public static void main(String[] args) {
		//String t = ":pandora.mo.ca.SwiftIRC.net NOTICE iUtil2 :*** Disabling usermode 'x' will reveal your IP address to anyone. If you did not intend this, use '/mode iUtil2 +x'";
		//String t2 = ":iPeer!iPeer@13.33.33.37 NOTICE iUtil2 :Hi";
		String t3 = ":zpoon!i@kicked.fd.and.burned.a.pancake QUIT :Ping timeout";
		Protocol a = new Protocol();
		//a.parse(t, null);
		//a.parse(t2, null);
		a.parse(t3, null);
	}

}
