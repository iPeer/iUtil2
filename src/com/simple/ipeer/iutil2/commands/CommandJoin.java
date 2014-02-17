package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandJoin extends Command {
    
    public CommandJoin() {
	registerAlias("join");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("Error: You must specify a channel to join!");
//	else if (engine.getIAL().getChannelList().containsKey(additionalData[0].toLowerCase()))
//	    throw new CommandException("I cannot join that channel because I am already in it!");
	else
	    engine.joinChannel(additionalData);
	sendReply(sender, sendPrefix, "Joined "+additionalData);
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <channel[,channel2,...]>";
    }

    @Override
    public String getHelpText() {
	return "Makes the bot join the specified channel(s).";

    }
    
}
