package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;

/**
 *
 * @author iPeer
 */
public class CommandTell extends Command {
    
    public CommandTell() {
	registerAliases("tell", "memo");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (sender instanceof Console)
	    throw new CommandException("You can't use this command from the console!");
	String channel = chatLine.split(" ")[2];
	String nick = sender.getNick();
	if (additionalData.split(" ").length < 2)
	    throw new CommandException("You must specify both a nick and at least one word to send to them!");
	
	String n = additionalData.split(" ")[0];
	String msg = additionalData.split(n+" ")[1];
	if (!n.equals(nick)) {
	    for (String c : engine.getIAL().getChannelList().keySet()) {
		if (engine.getIAL().isOnChannel(n, c) && engine.getIAL().isOnChannel(nick, c)) {
		    sendReply(sender, sendPrefix, String.format("Why not tell them yourself? They're on %s right now!", c));
		    return;
		}
	    }
	}
	try {
	    engine.getTell().addMessage(nick, n, msg);
	    sendReply(sender, sendPrefix, (n.equals(nick) ? "Trying to send a message to your future self eh, "+nick+"? Very well, it is done." : "I'll be sure to tell "+n+" that next time I see them, "+nick+"."));
	}
	catch (RuntimeException e) {
	    sendReply(sender, sendPrefix, e.getMessage());
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName()+" <nick> <message>";
    }
    
    @Override
    public String getHelpText() {
	return "Allows you to send a message to a user. THe message will be delivered to them the next time the bot seems them.";
	
    }
    
}
