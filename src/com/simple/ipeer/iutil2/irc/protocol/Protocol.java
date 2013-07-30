package com.simple.ipeer.iutil2.irc.protocol;

import java.util.Arrays;

import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.Channel;
import com.simple.ipeer.iutil2.irc.User;

public class Protocol {

	public Protocol () { }

	public void parse(String line, Main engine) {
		// We log the whole line for debug purposes
		engine.log("<- "+line, "IRC");
		
		
		// When the server PINGs us, we need to make sure we reply.
		if (line.startsWith("PING "))
			engine.send("PONG "+line.substring(5));
		
		// Handle being disconnected by the server
		else if (line.startsWith("ERROR :")) {
			engine.log("Disconnected by server! "+line.substring(7));
			//TODO: For now, we just terminate. We'll make it try and reconnect later
			System.exit(0);
		}

		// Handle actual chat messages
		else if (Arrays.asList("PRIVMSG", "NOTICE").contains(line.split(" ")[1])) {
			// Reusing old code because it doesn't make sense to rewrite this.
			String nick = line.split("!")[0].substring(1);
			String address = "";
			if (line.split(" ")[0].contains("!"))
				address = line.split("!")[1].split(" ")[0];
			String target = line.split(" ")[2];
			String[] messageData = line.split(":");
			String message = "";
			for (int x = 2; x < messageData.length; x++) {
				message = message+":"+messageData[x];
			}
			message = message.substring(1);	
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
			if (type.equals("JOIN") && !nick.equals(engine.CURRENT_NICK)) // Why are channels in joins prefixed with colons but parts aren't?
				engine.send("WHO +cn "+channel.substring(1)+" "+nick);
			else if (type.equals("QUIT"))
				for (Channel c : engine.getChannelList().values())
					if (c.getUserList().containsKey(nick))
						c.getUserList().remove(nick);
			else
				engine.getChannelList().get(channel).getUserList().remove(nick);
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
