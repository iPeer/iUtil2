package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandPart extends Command {
    
    public CommandPart() {
	registerAlias("part");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("Error: You must specify a channel to part!");
	String channel = additionalData.split(" ")[0];
	if (!engine.getIAL().getChannelList().containsKey(channel.toLowerCase()))
	    throw new CommandException("I cannot part that channel because I am not in it!");

	String msg = additionalData.substring(channel.length() + 1);
	
	engine.partChannel(channel, "PART from "+sender.getNick()+(msg.length() > 0 ? " ("+msg+")" : ""));
	sendReply(sender, sendPrefix, "Parted "+channel);
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <channel[,channel2,...]> [message]";
    }

    @Override
    public String getHelpText() {
	return "Makes the bot part the specified channel(s).";

    }
    
}
