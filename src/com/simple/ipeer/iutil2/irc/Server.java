package com.simple.ipeer.iutil2.irc;

import java.net.InetSocketAddress;

public class Server {
	
	private String SERVER_ADDRESS = "";
	private boolean USE_SSL = false;
	private int PORT = 6667;

	public Server(String addr, boolean SSL, int port) {
		this.SERVER_ADDRESS = addr;
		this.USE_SSL = SSL;
		this.PORT = port;
	}
	
	public int getPort() {
		return this.PORT;
	}
	
	public boolean isSSL() {
		return this.USE_SSL;
	}
	
	public String getAddress() {
		return this.SERVER_ADDRESS;
	}
	
	public InetSocketAddress getAddressAsInet() {
		return new InetSocketAddress(this.SERVER_ADDRESS, this.PORT);
	}
	
}
