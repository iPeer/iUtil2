package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandUnregisterCommand extends Command {
    
    public CommandUnregisterCommand() {
	registerAliases("delcommand", "unregistercommand");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("You need to specify a command to unregister!");
	ICommand command = engine.getCommandManager().getCommandForName(additionalData);
	if (command == null)
	    throw new CommandException("That isn't a valid command!");
	try {
	    engine.getCommandManager().unregisterCommand(command);
	    sendReply(sender, sendPrefix, "The command '"+command.getCommandName()+"' has been unregistered.");
	}
	catch (RuntimeException e) {
	    sendReply(sender, sendPrefix, e.getMessage());
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }
    
    @Override
    public String getHelpText() {
	return "";
	
    }
    
}
