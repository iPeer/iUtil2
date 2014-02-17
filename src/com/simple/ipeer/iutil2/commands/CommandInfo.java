package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class CommandInfo extends Command {
    
    public CommandInfo() {
	registerAliases("info", "information", "debuginfo", "debuginformation");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }

    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	List<String> out = engine.generateInfoOutput();
	for (String c : out)
	    sendReply(sender, sendPrefix, c);
    }

    @Override
    public String getCommandUsage() {
	return getCommandName();
    }

    @Override
    public String getHelpText() {
	return "Displays some debug information about the bot.";
    }

}
