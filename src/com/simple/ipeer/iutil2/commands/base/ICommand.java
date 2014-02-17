package com.simple.ipeer.iutil2.commands.base;

import com.simple.ipeer.iutil2.commands.Command;
import java.util.List;

/**
 *
 * @author iPeer
 */
public interface ICommand {

    public List<String> getAliases();
    public int getRequiredUserLevel();
    public abstract void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException;
    public void notifyDebugChannel(ICommandSender sender, Command com, String additionalData); // Probably won't be used. Ever. Here just incase we need a command to notify the bot's debug channel.
    public abstract String getCommandUsage();
    public abstract String getHelpText();
    public String getCommandName(); // Normally the first entry in the aliases list, but in some cases we may need to modify that.
    public void registerAlias(String alias);
    public void registerAliases(String... aliases);
    public void registerAliases(List<String> aliases);
    public boolean protectOnLoad();
    
}
