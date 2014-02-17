package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;
import com.simple.ipeer.iutil2.engine.Main;

/**
 *
 * @author iPeer
 */
public class CommandTestExternal extends Command {

    public CommandTestExternal() {
	registerAliases("teste", "teste1", "teste2");
    }
    
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionData) throws CommandException, InsufficientPermissionsException {
	sendReply(sender, sendPrefix, "This be a external class");
		    
    }

    @Override
    public String getCommandUsage() {
	return getCommandName();
    }

    @Override
    public String getHelpText() {
	return "It's yet another amazing test command... From the outside!";
    }
    
}
