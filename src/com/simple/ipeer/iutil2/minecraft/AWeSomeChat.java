package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.Main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class AWeSomeChat implements AnnouncerHandler {
    
    private Main engine;
    private List<IAWeSomeChatTailer> tailers;
    
    public AWeSomeChat(Main engine) {
	this.engine = engine;
	HashMap<String, String> settings = new HashMap<String, String>();
	settings.put("ascUpdateDelay", "500");
	settings.put("ascOutputPrefix", "%C1%[%C2%AWeSome %SERVERNAME%%C1%]: ");
	settings.put("ascOutputFormat", "%C2%%USER%%C1%: %MESSAGE%");
	settings.put("ascOutputInOutFormat", "%C2%%USER%%C1% %TYPE% the game.");
	settings.put("ascOutputActionFormat", "%C2%%USER% %ACTION%");
	settings.put("ascOutputDeathFormat", "%C2%%USER%%C1% %DEATHMESSAGE%");
	engine.createConfigDefaults(settings);
	
	tailers = new ArrayList<IAWeSomeChatTailer>();
	
	tailers.add(new AWeSomeChatTailer(engine, "/home/minecraft/servers/survival/server.log", "Survival"));
	tailers.add(new AWeSomeChatTailer(engine, "/home/minecraft/servers/creative/server.log", "Creative"));
	
    }
    
    @Override
    public boolean addUser(String name) {
	return false;
    }
    
    @Override
    public boolean removeUser(String name) {
	return false;
    }
    
    @Override
    public void updateAll() {
    }
    
    @Override
    public void startAll() {
	for (Iterator<IAWeSomeChatTailer> it = this.tailers.iterator(); it.hasNext();) {
	    it.next().startIfNotRunning();
	}
    }
    
    @Override
    public void stopAll() {
	for (Iterator<IAWeSomeChatTailer> it = this.tailers.iterator(); it.hasNext();) {
	    it.next().stop();
	}
    }
    
    @Override
    public void update(String name) {
    }
    
    @Override
    public long timeTilUpdate() {
	return (((AWeSomeChatTailer)this.tailers.iterator().next()).lastUpdate + getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public long getUpdateDelay() {
	return (engine == null ? 500 : Long.valueOf(engine.config.getProperty("ascUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
    }
    
    
}
