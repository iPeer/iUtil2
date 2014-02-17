package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;

/**
 *
 * @author iPeer
 */
public class CommandDelTell extends Command {
    
    public CommandDelTell() {
	registerAliases("deltell", "delmemo", "canceltell", "cancelmemo");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (sender instanceof Console)
	    throw new CommandException("You can't use this command from the console!");
	String nick = sender.getNick();
	if (additionalData.length() == 0)
	    throw new CommandException("You need to provide a nick of whom you wish to cancel your last message for.");
	else {
	    String n = additionalData.split(" ")[0];
	    try {
		engine.getTell().cancelMessage(nick, n);
		sendReply(sender, sendPrefix, "You last message to "+n+" has been cancelled.");
	    }
	    catch (RuntimeException e) {
		sendReply(sender, sendPrefix, e.getMessage());
	    }
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <nick>";
    }
    
    @Override
    public String getHelpText() {
	return "Revokes your most recent memo to a user.";
	
    }
    
}
