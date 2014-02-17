package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.engine.Main;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class CommandHelp extends Command {
    
    public CommandHelp() {
	registerAliases("help", "halp");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0 || getAliases().contains(additionalData)) {
	    
	    String out = "";
	    if (additionalData.length() > 0 && getAliases().contains(additionalData)) {
		engine.send(sendPrefix+" :You seem to have grasped how to use that command already!");
		return;
	    }
	    List<ICommand> commandList = engine.getCommandManager().getCommands();
	    int x = 0;
	    for (ICommand c : commandList) {
		x++;
		out = out+(x == commandList.size() ? " and " : (out.length() > 0 ? ", " : ""))+"%B%"+c.getCommandName()+"%B%";
	    }
	    sendHelpReply(sender, sendPrefix, "Here is a list of valid commands. For help on a specific one, type %s <command>");
	    sendReply(sender, sendPrefix, out);
	    
	}
	else {
	    
	    ICommand c = engine.getCommandManager().getCommandForName(additionalData);
	    if (c == null || c.getCommandUsage().equals("") || c.getHelpText().equals(""))
		sendReply(sender, sendPrefix, "There is no help available for that command."+(c == null ? " Probably because it doesn't exist ;)" : ""));
	    else
		sendReply(sender, sendPrefix, "%B%"+c.getCommandUsage()+"%B%: "+c.getHelpText());
	    
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" [command]";
    }

    @Override
    public String getHelpText() {
	return "Shows this list of commands.";
    }
    
}
