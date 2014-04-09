package com.simple.ipeer.iutil2.commands;

/* THIS COMMAND IS NEVER USED */

import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import java.sql.Connection;
import java.util.Arrays;

/**
 *
 * @author iPeer
 */
public class CommandSQL extends Command {
    
    public CommandSQL() {
	registerAliases("sql", "mysql", "postgresql", "postgre", "postgres", "psql");
    }
    
    @Override
    public int getRequiredUserLevel() {
	return 5;
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	if (Arrays.asList("INSERT, DELETE, UPDATE, DROP, ALTER").contains(additionalData.split(" ")[0])) { throw new CommandException("Those SQL commands cannot be executed remotely"); }
	if (additionalData.split(" ")[0].equals("-r")) { 
	    engine.createSQLConnection();
	    Connection c = engine.getSQLConnection();
	    if (c == null) { sendReply(sender, sendPrefix, "Unbale to recreate SQL connection. See error log for details."); return; }
	    sendReply(sender, sendPrefix, "SQL Connection has been created.");
	}
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }

    @Override
    public String getHelpText() {
	return "Allows remote execution of SQL commands.";

    }
    
}
