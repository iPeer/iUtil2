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
public class CommandExit extends Command {
    
    public CommandExit() {
	registerAliases("terminate", "exit");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (engine.isConnected())
	    engine.quit("TERMINATE from "+sender.getNick()+(additionalData.length() > 0 ? " ("+additionalData+")" : ""));
	else
	    engine.terminate();
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" [message]";
    }

    @Override
    public String getHelpText() {
	return "Terminates the bot's process, quitting from IRC first if neccessary.";

    }
    
}
