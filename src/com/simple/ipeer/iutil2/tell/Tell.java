package com.simple.ipeer.iutil2.tell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;

import com.simple.ipeer.iutil2.engine.Main;


public class Tell {

	protected Main engine;
	private HashMap<String, LinkedList<TellMessage>> messages;
	private HashMap<String, Long> timers;
	private File tellDir;

	public Tell(Main engine) {
		engine.log("Tell system is starting up...", "Tell");
		this.engine = engine;
		this.messages = new HashMap<String, LinkedList<TellMessage>>();
		this.timers = new HashMap<String, Long>();
		HashMap<String, String> dSettings = new HashMap<String, String>();
		dSettings.put("tellMaxMessages", "5");
		dSettings.put("tellDir", "./Tell");
		engine.createConfigDefaults(dSettings);
		tellDir = new File((engine == null ? "./Tell" : engine.config.getProperty("tellDir")));
		if (!tellDir.exists())
			tellDir.mkdirs();
		loadMessages();
		engine.log("Tell system started successfully.", "Tell");
	}

	private void saveMessages(String user) {
		engine.log("Tell system is attempting to save user data for '"+user+"'", "Tell");
		File uFile = new File(tellDir, user+".iuc");
			try {
				FileWriter fw = new FileWriter(uFile);
				for (TellMessage tm : messages.get(user))
					fw.write(tm.getSender()+"\01"+tm.getTime()+"\01"+tm.getMessage()+"\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		engine.log("Tell system saved user data for '"+user+"' successfully", "Tell");
	}

	private void removeCache(String user) {
		File uFile = new File(tellDir, user+".iuc");
		if (uFile.exists())
			if (!uFile.delete()) {
				uFile.deleteOnExit();
				engine.log("Couldn't delete Tell cache for '"+user+"', scheduling it to be deleted the nex ttime the bot exists.", "Tell");
			}
	}

	private void loadMessages() {
		for (File a : tellDir.listFiles()) {
			LinkedList<TellMessage> mData = new LinkedList<TellMessage>();
			try {
				Scanner s = new Scanner(new FileInputStream(a), "UTF-8");
				while (s.hasNextLine()) {
					String[] data = Matcher.quoteReplacement(s.nextLine()).split("\01");
					mData.add(new TellMessage(data[0], data[1], data[2]));
				}
				s.close();
			} catch (FileNotFoundException e) {
				continue;
			}
			messages.put(a.getName().split("\\.")[0], mData);
		}

	}

	public void addMessage(String sender, String recipient, String message) {
		if (!canSend(sender))
			throw new RuntimeException("You cannot send another message yet. Please wait "+(int)Math.floor((60000 - (System.currentTimeMillis() - timers.get(sender))) / 1000L)+" seconds before sending another message.");
		if (messages.containsKey(recipient) && messages.get(recipient).size() >= Integer.valueOf(engine.config.getProperty("tellMaxMessages")))
			throw new RuntimeException(recipient+" has too many pending messages and cannot recieve any more!");
		TellMessage tm = new TellMessage(sender, System.currentTimeMillis(), message);
		if (messages.containsKey(recipient))
			messages.get(recipient).add(tm);
		else {
			LinkedList<TellMessage> ll = new LinkedList<TellMessage>();
			ll.add(tm);
			messages.put(recipient, ll);
		}
		timers.put(sender, System.currentTimeMillis());
		saveMessages(recipient);
	}

	public void cancelMessage(String sender, String recipient) {
		if (messages.get(recipient).isEmpty() || !hasMessageFrom(sender, recipient))
			throw new RuntimeException(recipient+" doesn't have any messages from you.");
		for (int x = messages.get(recipient).size() - 1; x >= 0; x--) {
			if (messages.get(recipient).get(x).getSender().equals(sender)) {
				messages.get(recipient).remove(x);
				break;
			}
		}
		if (messages.get(recipient).isEmpty()) {
			removeCache(recipient);
			messages.remove(recipient);
		}
	}

	private boolean hasMessageFrom(String sender, String recipient) {
		for (TellMessage tm : messages.get(recipient))
			if (tm.getSender().equals(sender))
				return true;
		return false;
	}

	private boolean canSend(String sender) {
		return !timers.containsKey(sender) || (System.currentTimeMillis() - timers.get(sender)) > 60000;
	}

	public boolean hasMessages(String recipeint) {
		return messages.containsKey(recipeint);
	}

	public void sendMessages(String recipient) {
		if (!hasMessages(recipient))
			return;
		for (TellMessage tm : messages.get(recipient)) {
			engine.send("PRIVMSG "+recipient+" :Message from "+tm.getSender()+", received at "+new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Date(tm.getTime()))+":");
			engine.send("PRIVMSG "+recipient+" :"+tm.getMessage());
			if (messages.get(recipient).size() > 1)
				engine.send("PRIVMSG "+recipient+":-------");
		}
		messages.remove(recipient);
		removeCache(recipient);
	}

}
