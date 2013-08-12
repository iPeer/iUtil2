package com.simple.ipeer.iutil2.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.simple.ipeer.iutil2.engine.Main;

public class Console implements Runnable {

	private Main engine;
	private boolean isRunning = false;
	private Thread thread;

	public Console(Main engine) {
		this.engine = engine;
		if (engine != null)
			engine.log("Console instance is starting...", "Console");
	}

	public static void main(String[] args) {
		Console a = new Console(null);
		a.start();
	}

	public void startIfNotRunning() {
		if (!this.isRunning)
			start();
	}

	public void start() {
		this.isRunning = true;
		(thread = new Thread(this, "iUtil2 Commandline Listener")).start();
	}

	public void stop() {
		if (this.isRunning) {
			this.isRunning = false;
			thread.interrupt();
		}
	}

	@Override
	public void run() {
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		if (engine != null)
			engine.log("Console has started successfully",  "Console");
		try {
			String line = "";
			while((line = console.readLine()) != null && !thread.isInterrupted()) {
				String command = line.split(" ")[0];
				
				if (command.matches("forcey(ou)?t(ube)?update")) {
					engine.getAnnouncers().get("YouTube").updateAll();
				}
				else if (command.equals("forcetwitchupdate")) {
					engine.getAnnouncers().get("Twitch").updateAll();
				}
				else if (command.matches("stop|quit|die|q{1,3}")) {
					engine.quit("QUIT from console.");
				}
				else if (command.equals("reconnect")) {
					engine.quit("RECONNECT from console.", true);
				}
				
			}
		}
		catch (IOException e) {
			engine.log("Console instance encountered an error.", "Console");
			engine.logError(e, "Console");
		}
	}



}
