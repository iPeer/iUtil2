package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandConfig extends Command {
    
    public CommandConfig() {
	registerAliases("config", "configuration");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	String[] d = additionalData.split(" ");
	if (d.length == 0)
	    throw new CommandException("Must provide at least a config entry");
	else {
	    String configEntry = d[0];
	    if (engine.config.containsKey(configEntry)) {
		engine.disableFormatProcessing();
		if (d.length >= 2) {
		    String oldEntry = engine.config.getProperty(configEntry);
		    String newEntry = "";
		    for (int x = 1; x < d.length; x++)
			newEntry += (newEntry.length() > 0 ? " " : "")+d[x];
		    engine.config.put(configEntry, newEntry);
		    engine.saveConfig();
		    sendReply(sender, sendPrefix, String.format("Config entry %s has been changed from %s to %s.", configEntry, newEntry, oldEntry));
		}
		else {
		    sendReply(sender, sendPrefix, String.format("Config entry %s is currently set as %s", configEntry, engine.config.getProperty(configEntry)));
		}
		engine.enableFormatProcessing();
	    }
	    else
		sendReply(sender, sendPrefix, String.format("Config entry %s doesn't exist.", configEntry));
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <configEntry> [newValue]";
    }

    @Override
    public String getHelpText() {
	return "Allows viewing or changing of the bot's configuration options.";

    }
    
}
