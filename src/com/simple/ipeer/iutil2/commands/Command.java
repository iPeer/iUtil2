package com.simple.ipeer.iutil2.commands;

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.ICommand;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.console.Console;
import com.simple.ipeer.iutil2.engine.Main;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author iPeer
 */
public abstract class Command implements ICommand {
    
    private List<String> aliasList;
    protected Main engine;
    
    public Command() {
	this.aliasList = new ArrayList<String>();
	engine = Main.getEngine();
    }

    @Override
    public List<String> getAliases() {
	return aliasList;
    }

    @Override
    public int getRequiredUserLevel() {
	return 0;
    }

    public abstract void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException;
    public abstract String getCommandUsage();
    public abstract String getHelpText();

    @Override
    public String getCommandName() {
	return getAliases().iterator().next();
    }

    @Override
    public void registerAlias(String alias) {
	aliasList.add(alias);
    }

    @Override
    public void registerAliases(String... aliases) {
	aliasList.addAll(Arrays.asList(aliases));
    }

    @Override
    public void registerAliases(List<String> aliases) {
	aliasList.addAll(aliases);
    }

    @Override
    public void notifyDebugChannel(ICommandSender sender, Command com, String additionalData) {
    }
    
    public void sendHelpReply(ICommandSender sender, String sendPrefix, String line) {
	engine.sendCommandHelpReply(sender, sendPrefix, line);
    }
    
    public void sendReply(ICommandSender sender, String sendPrefix, String line) {
	engine.sendCommandReply(sender, sendPrefix, line);
    }
    
    @Override
    public boolean protectOnLoad() {
	return false;
    }

}
