package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.io.File;
import java.io.FileWriter;

/**
 *
 * @author iPeer
 */
public class CommandDebugDump extends Command {
    
    public CommandDebugDump() {
	registerAlias("dumpdebug");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	sendReply(sender, sendPrefix, "Creating debug dump...");
	try {
	    File a = new File((engine == null ? "logs/" : engine.logDir)+"/debug-"+System.currentTimeMillis()+".txt");
	    engine.getDebugger().writeDebug(new FileWriter(a));
	    sendReply(sender, sendPrefix, "Debug dump written to "+a.getAbsolutePath());
	}
	catch (Throwable e) {
	    engine.logError(e, "Debugger");
	    engine.log("Couldn't write debug dump.", "Debugger");
	    sendReply(sender, sendPrefix, "Couldn't create debug dump. See error log for details.");
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }
    
    @Override
    public String getHelpText() {
	return "Creates a text files containing a whole bunch of debug information.";
	
    }
    
}
