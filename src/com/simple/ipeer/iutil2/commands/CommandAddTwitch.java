package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;

/**
 *
 * @author iPeer
 */
public class CommandAddTwitch extends Command {
    
    public CommandAddTwitch() {
	registerAliases("addtwitchchannel", "addtwitchuser", "addtwitch");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 4;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (additionalData.length() == 0)
	    throw new CommandException("You must give me the person's channel name so I can stalk them for you.");
	String user = additionalData;
	if (engine.getAnnouncers().get("Twitch").addUser(user))
	    engine.send(sendPrefix+" :Now watching "+user+" for streams.");
	else
	    engine.send(sendPrefix+" :"+user+" is already being watched for streams!");
	
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <user>";
    }
    
    @Override
    public String getHelpText() {
	return "Adds the specified user to the Twitch stream watch list.";
	
    }
    
}
