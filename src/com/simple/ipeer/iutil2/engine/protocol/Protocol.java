package com.simple.ipeer.iutil2.engine.protocol;

import java.util.Arrays;

import com.simple.ipeer.iutil2.engine.Main;

public class Protocol {

	public Protocol () { }

	public void parse(String line, Main engine) {
		// We log the whole line for debug purposes
		engine.log(line);
		
		
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

	}

}
