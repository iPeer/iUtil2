package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.util.Random;

/**
 *
 * @author iPeer
 */
public class CommandRandomNumber extends Command {
    
    public CommandRandomNumber() {
	registerAlias("random");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	sendReply(sender, sendPrefix, Integer.toString(new Random().nextInt(10000)));
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }

    @Override
    public String getHelpText() {
	return "It just gives you a random number!";

    }
    
}
