package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandSend extends Command {
    
    public CommandSend() {
	registerAlias("send");	
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() < 0)
	    throw new CommandException("Command error: Insufficient arguments. See %s "+getCommandName()+" for help.");
	else if (additionalData.startsWith("NICK"))
	    throw new CommandException("Do not use this command to change the bot's nick. For that use the 'nick' command instead.");
	else
	    engine.send(additionalData);
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <TYPE> [<WHERE>] :<what...>";
    }

    @Override
    public String getHelpText() {
	return "Sends the supplies data to the IRC server. Must be in IRC protocol format. See http://tools.ietf.org/html/rfc2812#section-2.3 for details.";

    }
    
}
