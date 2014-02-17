package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandDelYouTube extends Command {
    
    public CommandDelYouTube() {
	registerAliases("delyoutubechannel", "delyoutubeuser", "delyoutube", "delyt");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("You must give me the person's channel name/ID so I can stop stalking them.");
	String user = additionalData;
	if (engine.getAnnouncers().get("YouTube").removeUser(user))
	    sendReply(sender, sendPrefix, "No longer watching "+user+" for YouTube uploads.");
	else
	    sendReply(sender, sendPrefix, user+" isn't being watched for uploads.");
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <user>";
    }
    
    @Override
    public String getHelpText() {
	return "Removes the specified user to the YouTube upload watch list.";
	
    }
    
}
