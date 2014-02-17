package com.simple.ipeer.iutil2.irc.ial;

import java.util.HashMap;

import com.simple.ipeer.iutil2.engine.Main;

public class IAL {

	private Main engine;
	private HashMap<String, HashMap<String, User>> CHANNEL_LIST;
	private String CURRENT_NICK = "";

	public IAL(Main engine) {
		engine.log("IAL is starting up...", "IAL");
		this.engine = engine;
		CHANNEL_LIST = new HashMap<String, HashMap<String, User>>();
		engine.log("IAL started.", "IAL");
	}

	public static void main(String[] args) {
		IAL a = new IAL(null);
		a.parseModes("#Peer.Dev", "+vmif-oq+ao", "SomeTestUser [1t]:5 iPeer iPeer iPeer iPeer");
	}

	public void clearIAL() {
		if (!this.CHANNEL_LIST.isEmpty()) {
			this.CHANNEL_LIST.clear();
			engine.log("The IAL has been cleared.", "IAL");
		}
	}

	public void registerChannel(String channel) {
		this.CHANNEL_LIST.put(channel.toLowerCase(), new HashMap<String, User>());
		engine.log("Registered '"+channel+"'", "IAL");
	}

	public void unregisterChannel(String channel) {
		this.CHANNEL_LIST.remove(channel.toLowerCase());
		engine.log("Unregistered '"+channel+"'", "IAL");
	}

	public void registerNick(String channel, String nick) {
		registerNick(channel, new User(nick));
	}

	public void registerNick(String channel, User nick) {
		if (!isOnChannel(nick.getNick(), channel)) {
			this.CHANNEL_LIST.get(channel.toLowerCase()).put(nick.getNick(), nick);
			engine.log("Registered '"+nick.getNick()+"' under '"+channel+"'", "IAL");
		}
		else {
			User user = getUser(channel, nick.getNick());
			updateAddressIfNeeded(channel, nick.getNick(), user.getFullAddress().split("!")[1]);
		}
	}

	public void unregisterNick(String channel, User nick) {
		unregisterNick(channel, nick.getNick());
	}

	public void unregisterNick(String channel, String nick) {
		this.CHANNEL_LIST.get(channel.toLowerCase()).remove(nick);
		engine.log("Unregistered '"+nick+"'", "IAL");
	}

	public void unregisterQuittingNick(String nick) {
		for (String a : this.CHANNEL_LIST.keySet()) {
			if (this.CHANNEL_LIST.get(a).containsKey(nick))
				this.CHANNEL_LIST.get(a).remove(nick);
		}
	}


	public void updateAddressIfNeeded(String channel, String nick, String address) {
		if (isOnChannel(nick, channel) && !this.CHANNEL_LIST.get(channel.toLowerCase()).get(nick).getFullAddress().equals(nick+"!"+address)) {
			User u = this.CHANNEL_LIST.get(channel.toLowerCase()).get(nick);
			if (address.contains("!"))
				u.setAddressFromFull(address);
			else
				u.setAddressFromFull(nick+"!"+address);
			engine.log("Updated IAL entry for '"+nick+"'", "IAL");
		}
		
/*		for (String a : this.CHANNEL_LIST.keySet()) {
			if (!getUser(a, nick).getFullAddress().equals(nick+"!"+address)) {
				User u = getUser(a, nick);
				u.setAddressFromFull(nick+"!"+address);
			}
			engine.log("Updated IAL entry for '"+nick+"'", "IAL");
		}*/
		
	}

	public void processNickChange(String nick, String newNick) {
		for (String a : this.CHANNEL_LIST.keySet()) {
			if (this.CHANNEL_LIST.get(a).containsKey(nick)) {
				User b = this.CHANNEL_LIST.get(a).get(nick);
				b.changeNick(newNick);
				this.CHANNEL_LIST.get(a).put(newNick, b);
				this.CHANNEL_LIST.get(a).remove(nick);
				engine.log("'"+nick+"' has been updated to be known as '"+newNick+"' in the IAL.", "IAL");
			}
		}
	}

	public User getUser(String channel, String user)/* throws GeneralException*/ {
		//		if (!CHANNEL_LIST.containsKey(channel.toLowerCase()) || !CHANNEL_LIST.get(channel.toLowerCase()).containsKey(user))
		//			throw new GeneralException("No such user or channel.");
		return CHANNEL_LIST.get(channel.toLowerCase()).get(user);
	}

	public boolean amIOnChannel(String channel) {
		return CHANNEL_LIST.containsKey(channel.toLowerCase());	
	}

	public boolean isOnChannel(String user, String channel) {
		try {
			return CHANNEL_LIST.get(channel.toLowerCase()).containsKey(user);
		}
		catch (NullPointerException e) { return false; }
	}

	/*
	 *  THIS CODE IS WORK IN PROGRESS AND IS PROBABLY PRETTY MESSY
	 *  GIVEN TIME, IT WILL PROBABLY GET CLEANER AND MORE EFFICIENT.
	 */

	public void parseModes(String channel, String modes, String args) {
		// TestIRC: beI,kfL,lj,psmntirRcOAQKVCuzNSMTGZ
		// SwiftIRC: beIqa,kfL,lj,psmntirRcOAQKVCuzNSMTGHFEB
		//              ^^
		int arg = 0;
		boolean set = false;
		String[] chanModes = (engine == null ? "beI,kfL,lj,psmntirRcOAQKVCuzNSMTGZ" : engine.NETWORK_SETTINGS.get("CHANMODES")).split(",");
		for (int x = 0; x < modes.length(); x++) {
			String c = String.valueOf(modes.charAt(x)); // Easier to work with Strings
			if ("+-".contains(c)) {
				set = c.equals("+");
				continue;
			}
			else if (chanModes[0].contains(c) || chanModes[1].contains(c) || (chanModes[2].contains(c) && set)) {
				arg++;
				continue;
			}
			else if ((engine == null ? "(qaohv)~&@%+" : engine.NETWORK_SETTINGS.get("PREFIX")).split("\\)")[0].substring(1).contains(c)) {
				this.CHANNEL_LIST.get(channel.toLowerCase()).get(args.split(" ")[arg]).updateModes(c, set);
				arg++;
			}
		}
	}

	public boolean userHasModes(String nick, String channel, String modes) {
		if (!isOnChannel(nick, channel) || !amIOnChannel(channel))
			return false;
		for (int x = 0; x < modes.length(); x++) {
			String c = String.valueOf(modes.charAt(x));
			if (getUser(channel, nick).getModes().contains(c))
				return true;
		}
		return false;
	}

	public String getCurrentNick() {
		return this.CURRENT_NICK;
	}

	public void setCurrentNick(String nick) {
		this.CURRENT_NICK = nick;
	}

	public HashMap<String, HashMap<String, User>> getChannelList() {
		return this.CHANNEL_LIST;
	}
	
	public User getFirstValidOrNewUserObject(String nick) {
	    for (String c : getChannelList().keySet())
		if (isOnChannel(nick, c))
		    return getUser(c, nick);
	    return new User(nick);
	}

}
