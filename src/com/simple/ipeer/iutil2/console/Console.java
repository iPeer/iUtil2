package com.simple.ipeer.iutil2.console;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.CommandNotFoundException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.simple.ipeer.iutil2.engine.Main;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Console implements Runnable, ICommandSender {
    
    private Main engine;
    private boolean isRunning = false;
    private Thread thread;
    
    public Console(Main engine) {
	this.engine = engine;
	if (engine != null)
	    engine.log("Waiting for connect to start console instance...", "Console");
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
		String additionalData = "";
		for (int x = 1; x < line.split(" ").length; x++)
		    additionalData += (additionalData.length() > 0 ? " " : "")+line.split(" ")[x];
		
		try {
		    engine.getCommandManager().executeCommand(command, (ICommandSender)this, line, "Console", additionalData);
		} catch (CommandNotFoundException ex) {
		    System.out.println("Command not found: "+command);
		} catch (InsufficientPermissionsException ex) {
		    // Never thrown for the console
		} catch (CommandException ex) {
		    System.out.println("Command error: "+ex.getMessage());
		}
		
	    }
	}
	catch (IOException e) {
	    engine.log("Console instance encountered an error.", "Console");
	    engine.logError(e, "Console");
	}
    }
    
    @Override
    public String getNick() {
	return "Console";
    }
    
    @Override
    public String getIdentd() {
	return "Console";
    }
    
    @Override
    public String getAddress() {
	return "127.0.0.1";
    }
    
    
    
}
