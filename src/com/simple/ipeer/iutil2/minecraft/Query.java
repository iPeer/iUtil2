/*
 * This class is mostly untouched from the iUtil1 version as nothing in this file really needs to be changed.
 */

package com.simple.ipeer.iutil2.minecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Query {
    
    private InetSocketAddress address;
    private Map<String, String> values;
    private String[] users;
    
    public static void main(String[] args) throws IOException {
	Query q = new Query("79.143.191.217", 35565);
	q.sendQuery();
	Map<String, String> data = q.getData();
	String[] players = q.getPlayers();
	for (String key : data.keySet())
	    System.out.println(key+": "+data.get(key));
	if (players.length == 0)
	    return;
	System.out.println("\n=== PLAYERS ===\n");
	for (String p : players)
	    System.out.println(p);
	
    }
    
    public Query(String address, int port) {
	this(new InetSocketAddress(address, port));
    }
    
    public Query(InetSocketAddress inetSocketAddress) {
	this.address =  inetSocketAddress;
    }
    
    public void sendQuery() throws IOException {
	sendQueryRequest();
    }
    
    public Map<String, String> getData() {
	return this.values;
    }
    
    public String[] getPlayers() {
	return this.users;
    }
    
    public String getAddress() {
	return this.address.getHostString()+":"+this.address.getPort();
    }
    
    public void sendQueryRequest() throws IOException {
	
	DatagramSocket socket = new DatagramSocket();
	try {
	    byte[] recv = new byte[10240];
	    socket.setSoTimeout(5000);
	    long now = System.nanoTime();
	    sendPacket(socket, address, 0xFE, 0xFD, 0x09, 0x01, 0x01, 0x01, 0x01);
	    int challenge;
	    {
		receivePacket(socket, recv);
		byte b = -1;
		int i = 0;
		byte[] buffer = new byte[8];
		for (int c = 5; (b = recv[c++]) != 0;)
		    buffer[i++] = b;
		challenge = Integer.parseInt(new String(buffer).trim());
	    }
	    
	    sendPacket(socket, address, 0xFE, 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01, challenge >> 24, challenge >> 16, challenge >> 8, challenge, 0x00, 0x00, 0x00, 0x00);
	    int length = receivePacket(socket, recv).getLength();
	    long ping = (System.nanoTime() - now) / 0xf4240L;
	    values = new HashMap<String, String>();
	    values.put("ping", ping+"ms");
	    AtomicInteger cursor = new AtomicInteger(5);
	    while (cursor.get() < length) {
		String s = readString(recv, cursor);
		if (s.length() == 0) // null terminator
		    break;
		String value = readString(recv, cursor);
		values.put(s, value);
	    }
	    readString(recv, cursor);
	    Set<String> players = new HashSet<String>();
	    while (cursor.get() < length) {
		String player = readString(recv, cursor);
		if (player.length() > 0)
		    players.add(player.trim());
	    }
	    users = players.toArray(new String[players.size()]);
	}
	finally {
	    socket.close();
	}
	
    }
    
    private final static void sendPacket(DatagramSocket socket, InetSocketAddress address, byte... data) throws IOException {
	DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
	socket.send(packet);
    }
    
    // Helper
    private final static void sendPacket(DatagramSocket socket, InetSocketAddress address, int... data) throws IOException {
	byte[] d = new byte[data.length];
	int x = 0;
	for (int i : data)
	    d[x++] = (byte)(i & 0xff);
	sendPacket(socket, address, d);
    }
    
    private final static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	socket.receive(packet);
	return packet;
    }
    
    public final static String readString(byte[] array, AtomicInteger cursor) {
	int start = cursor.incrementAndGet();
	for (; cursor.get() < array.length && array[cursor.get()] != 0; cursor.incrementAndGet()) { }
	return new String(Arrays.copyOfRange(array, start, cursor.get()));
    }
    
    public InetSocketAddress getInetAddress() {
	return this.address;
    }
    
    
}
