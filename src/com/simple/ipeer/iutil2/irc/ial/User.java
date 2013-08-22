package com.simple.ipeer.iutil2.irc.ial;

public class User {

	private String nick, address, ident, usermodes = "";
	
	public User(String nick, String ident, String address) {
		this.nick = nick;
		this.ident = ident;
		this.address = address;
	}
	
	public User(String nick) {
		this.nick = nick;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public void setAddressFromFull(String fullAddress) {
		this.ident = fullAddress.split("!")[1].split("@")[0];
		this.address = fullAddress.split("@")[1];
	}
	
	public void setIdent(String ident) {
		this.ident = ident;
	}
	
	public String getNick() {
		return this.nick;
	}
	
	public String getIdent() {
		return this.ident;
	}
	
	public String address() {
		return this.address;
	}
	
	public String getFullAddress() {
		return this.nick+"!"+this.ident+"@"+this.address;
	}
	
	public String getUsermodes() {
		return getModes();
	}
	
	public String getModes() {
		return this.usermodes;
	}

	public void changeNick(String newNick) {
		this.nick = newNick;
	}
	
	public void updateModes(String mode, boolean set) {
		if (set && !this.usermodes.contains(mode))
			this.usermodes += mode;
		else if (!set && this.usermodes.contains(mode))
			this.usermodes = this.usermodes.replaceAll(mode, "");
	}

	public void setModes(String modes) {
		this.usermodes = modes;
	}
	
}
