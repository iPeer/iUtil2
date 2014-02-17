package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.util.CustomClassLoader;
import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class CommandRegisterExternalCommands extends Command {
    
    public CommandRegisterExternalCommands() {
	registerAliases("loadexternalcommands", "registerexternalcommands");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	//	if (additionalData.length() == 0)
	//	    throw new CommandException("Not enough arguments were supplied. Use "+(sender instanceof Console ? "" : ".")+"help "+getCommandName()+" for a list of required arguments.");
	File classDir = new File("./addons/com/simple/ipeer/iutil2/commands/");
	for (File f : classDir.listFiles()) {
	    try {
		if (!f.getAbsolutePath().endsWith(".class"))
		    continue;
		String path = f.getAbsolutePath();
		String className = "com.simple.ipeer.iutil2.commands."+path.substring(path.lastIndexOf("\\") + 1).replace(".class", "");
		Class cls = Class.forName(className, true, new CustomClassLoader());
		ICommand com = (ICommand)cls.newInstance();
		engine.getCommandManager().registerCommand(com, false); // Manually loaded classes, ESPECIALLY external ones should never be protected.
	    } catch (ClassNotFoundException ex) {
		ex.printStackTrace();
		throw new CommandException(ex.toString());
	    } catch (InstantiationException | IllegalAccessException ex) {
		throw new CommandException("Command loading failed: "+ex.toString()+" at "+ex.getStackTrace()[0]);
	    }
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
