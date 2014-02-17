package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class CommandQuit extends Command {
    
    public CommandQuit() {
	registerAliases("quit", "qqq", "die");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	engine.quit("QUIT from "+sender.getNick()+(additionalData.length() > 0 ? " ("+additionalData+")" : ""));
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" [message]";
    }

    @Override
    public String getHelpText() {
	return "Disconnects the bot from the IRC network, using [message] as the message if it was supplied.";

    }
    
}
