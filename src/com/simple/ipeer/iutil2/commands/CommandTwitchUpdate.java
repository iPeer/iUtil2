package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandTwitchUpdate extends Command {
    
    public CommandTwitchUpdate() {
	registerAliases("forcetwitchupdate");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(final ICommandSender sender, String chatLine, final String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	new Thread(
		new Runnable() {
		    public void run() {
			sendReply(sender, sendPrefix, "Updating all Twitch threads...");
			long start = System.currentTimeMillis();
			engine.getAnnouncers().get("Twitch").updateAll();
			sendReply(sender, sendPrefix, "Finished updating all Twitch threads. Update took "+(System.currentTimeMillis() - start)+"ms.");
		    }
		}).start();
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }
    
    @Override
    public String getHelpText() {
	return "Forces an update of all running Twitch stream announcers.";
	
    }
    
}
