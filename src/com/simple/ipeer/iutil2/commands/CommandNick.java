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
public class CommandNick extends Command {
    
    public CommandNick() {
	registerAliases("nick", "name");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("Must supply a nick for the bot to change to!");
	engine.changeNick(additionalData);
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <newNick>";
    }

    @Override
    public String getHelpText() {
	return "Changes the bot's current nick to <newNick>";

    }
    
}
