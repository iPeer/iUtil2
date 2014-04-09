package com.simple.ipeer.iutil2.commands.base;

import com.simple.ipeer.iutil2.commands.CommandAddTwitch;
import com.simple.ipeer.iutil2.commands.CommandAddYouTube;
import com.simple.ipeer.iutil2.commands.CommandConfig;
import com.simple.ipeer.iutil2.commands.CommandDebugDump;
import com.simple.ipeer.iutil2.commands.CommandDelTell;
import com.simple.ipeer.iutil2.commands.CommandDelTwitch;
import com.simple.ipeer.iutil2.commands.CommandDelYouTube;
import com.simple.ipeer.iutil2.commands.CommandExit;
import com.simple.ipeer.iutil2.commands.CommandHelp;
import com.simple.ipeer.iutil2.commands.CommandInfo;
import com.simple.ipeer.iutil2.commands.CommandJoin;
import com.simple.ipeer.iutil2.commands.CommandMinecraftServiceStatus;
import com.simple.ipeer.iutil2.commands.CommandNick;
import com.simple.ipeer.iutil2.commands.CommandPart;
import com.simple.ipeer.iutil2.commands.CommandQuit;
import com.simple.ipeer.iutil2.commands.CommandReconnect;
import com.simple.ipeer.iutil2.commands.CommandRegisterCommand;
import com.simple.ipeer.iutil2.commands.CommandRegisterExternalCommands;
import com.simple.ipeer.iutil2.commands.CommandSQL;
import com.simple.ipeer.iutil2.commands.CommandSend;
import com.simple.ipeer.iutil2.commands.CommandTell;
import com.simple.ipeer.iutil2.commands.CommandTwitchUpdate;
import com.simple.ipeer.iutil2.commands.CommandUnregisterCommand;
import com.simple.ipeer.iutil2.commands.CommandYouTubeSearch;
import com.simple.ipeer.iutil2.commands.CommandYouTubeUpdate;
import com.simple.ipeer.iutil2.console.Console;
import com.simple.ipeer.iutil2.engine.Main;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class CommandManager {
    
    private Main engine;
    private List<ICommand> commandList;
    private List<String> protectedCommands; // Can't unregister these.
    
    public CommandManager(Main main) {
	this.engine = main;
	commandList = new ArrayList<ICommand>();
	protectedCommands = new ArrayList<String>();
	
	registerCommand(new CommandRegisterCommand(), true);
	registerCommand(new CommandRegisterExternalCommands(), true);
	registerCommand(new CommandUnregisterCommand(), true);
	registerCommand(new CommandHelp(), true);
	registerCommand(new CommandInfo(), true);
	registerCommand(new CommandQuit(), true);
	registerCommand(new CommandReconnect(), true);
	registerCommand(new CommandYouTubeUpdate(), true);
	registerCommand(new CommandTwitchUpdate(), true);
	registerCommand(new CommandJoin(), true);
	registerCommand(new CommandPart(), true);
	registerCommand(new CommandSend(), true);
	registerCommand(new CommandNick(), true);
	registerCommand(new CommandDebugDump(), true);
	registerCommand(new CommandYouTubeSearch(), true);
	registerCommand(new CommandMinecraftServiceStatus(), true);
	registerCommand(new CommandAddYouTube(), true);
	registerCommand(new CommandDelYouTube(), true);
	registerCommand(new CommandAddTwitch(), true);
	registerCommand(new CommandDelTwitch(), true);
	registerCommand(new CommandTell(), true);
	registerCommand(new CommandDelTell(), true);
	registerCommand(new CommandExit(), true);
	registerCommand(new CommandConfig(), true);
	registerCommand(new CommandSQL(), true);
		
    }
    
    public final void registerCommand(ICommand command) {
	registerCommand(command, false);
    }
    
    public final void registerCommand(ICommand command, boolean protect) {
	this.commandList.add(command);
	if (protect)
	    this.protectedCommands.add(command.getCommandName());
	if (engine == null)
	    System.out.print("Registered command '"+command.getCommandName()+"'");
	else
	    engine.log("Registered command '"+command.getCommandName()+"'", "CommandManager");
    }
    
    public final List<ICommand> getCommands() {
	return this.commandList;
    }
    
    public void executeCommand(String commandName, ICommandSender sender, String incomingLine, String sendPrefix) throws CommandNotFoundException, CommandException, InsufficientPermissionsException {
	executeCommand(commandName, sender, incomingLine, sendPrefix, "");
    }
    
    public boolean isValidCommand(String name) {
	for (ICommand c : commandList)
	    for (String a : c.getAliases())
		if (a.equalsIgnoreCase(name))
		    return true;
	return false;
    }
    
    public ICommand getCommandForName(String name) {
	for (ICommand c : commandList)
	    for (String a : c.getAliases())
		if (a.equalsIgnoreCase(name))
		    return c;	
	return null;
    }
    
    public void executeCommand(String commandName, ICommandSender sender, String incomingLine, String sendPrefix, String additionalData) throws CommandNotFoundException, CommandException, InsufficientPermissionsException {
	ICommand command = null;
	command = getCommandForName(commandName);
	if (command == null)
	    throw new CommandNotFoundException();
	
	// TODO: Code actual admin system that is better than checking if a user has op in a channel...	
	int userLevel = (engine.getIAL().userHasModes(sender.getNick(), engine.config.getProperty("debugChannel"), "o") ? 4 : 0);
	if (sender.getNick().equals("iPeer")) // Escalate myself to UL 5.
	    userLevel++;
	if (sender instanceof Console || userLevel >= command.getRequiredUserLevel()) {
	    command.process(sender, incomingLine, sendPrefix, additionalData);
	}
	else {
	    throw new InsufficientPermissionsException();
	}
	
    }

    public void unregisterCommand(ICommand command) {
	if (this.protectedCommands.contains(command.getCommandName()))
	    throw new RuntimeException("That command is protected and cannot be unregistered.");
	getCommands().remove(command);
    }

}
