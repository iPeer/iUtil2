package com.simple.ipeer.iutil2.commands;


import com.simple.ipeer.iutil2.commands.base.CommandException;
import com.simple.ipeer.iutil2.commands.base.ICommandSender;
import com.simple.ipeer.iutil2.commands.base.InsufficientPermissionsException;
import com.simple.ipeer.iutil2.minecraft.servicestatus.MinecraftServiceStatus;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author iPeer
 */
public class CommandMinecraftServiceStatus extends Command {
    
    public CommandMinecraftServiceStatus() {
	registerAliases("minecraftstatus", "minecraftservicestatus", "mcstatus", "mcservicestatus");
    }
    
    @Override
    public void process(ICommandSender sender, String chatLine, String sendPrefix, String additionalData) throws CommandException, InsufficientPermissionsException {
	MinecraftServiceStatus mcss = (MinecraftServiceStatus)engine.getAnnouncers().get("Minecraft Service Status");
	String out = "";
	HashMap<String, HashMap<String, String>> statusData = mcss.getStatusData();
	for (Iterator<String> it = statusData.keySet().iterator(); it.hasNext();) {
	    String key = it.next();
	    String ping = statusData.get(key).get("ping");
	    String status = statusData.get(key).get("status");
	    String errorMessage = "";
	    if (statusData.get(key).containsKey("errorMessage"))
		errorMessage = statusData.get(key).get("errorMessage");
	    String colourPrefix = (!errorMessage.equals("") || !status.equals("200") ? "%K04%" : (Integer.valueOf(ping) > 1500 ? "%K08%" : "%K03%"));
	    out += (out.length() > 0 ? "%C1%, " : "")+colourPrefix+key+" ("+(errorMessage.equals("") ? ping+"ms" : errorMessage)+")";
	}
	sendReply(sender, sendPrefix, out);
    }
    
    @Override
    public String getCommandUsage() {
	return getCommandName();
    }
    
    @Override
    public String getHelpText() {
	return "Displays information on the status of various Minecraft services. Accurate to within 60 seconds.";
	
    }
    
}
