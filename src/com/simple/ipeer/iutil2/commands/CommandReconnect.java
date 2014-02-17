package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandReconnect extends Command {
    
    public CommandReconnect() {
	registerAliases("reconnect", "jump");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (engine.isConnected()) 
	    engine.quit("RECONNECT from "+sender.getNick(), true);
	else
	    engine.getProtocol().handleDisconnect(engine, "Forced Reconnect");
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }

    @Override
    public String getHelpText() {
	return "Reconnects to bot the the network.";

    }
    
}
