package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;
import com.simple.ipeer.iutil2.engine.Main;

/**
 *
 * @author iPeer
 */
public class CommandRegisterCommand extends Command {
    
    public CommandRegisterCommand() {
	registerAliases("addcommand", "registercommand");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("Not enough arguments were supplied. Use "+(sender instanceof Console ? "" : ".")+"help "+getCommandName()+" for a list of required arguments.");
	try {
	    ClassLoader cl = getClass().getClassLoader();
	    Class clazz = cl.loadClass(additionalData);
	    ICommand command = (ICommand)clazz.newInstance();
	    if (engine.getCommandManager().isValidCommand(command.getCommandName()))
		throw new CommandException("Command loading failed: A command with that name is already loaded.");
	    Main.getEngine().getCommandManager().registerCommand(command, false);
	    String commandName = additionalData.split("\\.")[additionalData.split("\\.").length - 1];
	    String out = "Command '"+commandName+"' succesfully registered. Use %s "+command.getCommandName()+" to see usage information.";
	    sendHelpReply(sender, sendPrefix, out);
	} catch (ClassNotFoundException ex) {
	    throw new CommandException("Class with the name '"+additionalData+"' was not found.");
	} catch (InstantiationException | IllegalAccessException ex) {
	    throw new CommandException("Command loading failed: "+ex.toString()+" at "+ex.getStackTrace()[0]);
	}
    }

    @Override
    public String getCommandUsage() {
	return getCommandName()+" <path.to.class>";
    }

    @Override
    public String getHelpText() {
	return "Registers a command that is not loaded by default.";
    }

}
