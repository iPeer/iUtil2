package com.simple.ipeer.iutil2.minecraft;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author iPeer
 */
public class AWeSomeChat implements AnnouncerHandler, DebuggableSub {
    
    private Main engine;
    private List<IAWeSomeChatTailer> tailers;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    private long lastUpdate = 0L;
    
    public AWeSomeChat(Main engine) {
	this.engine = engine;
	HashMap<String, String> settings = new HashMap<String, String>();
	settings.put("ascUpdateDelay", "500");
	settings.put("ascOutputPrefix", "%C1%[%C2%AWeSome %SERVERNAME%%C1%]: ");
	settings.put("ascOutputFormat", "%C2%%USER%%C1%: %MESSAGE%");
	settings.put("ascOutputInOutFormat", "%C2%%USER%%C1% %TYPE% the game.");
	settings.put("ascOutputActionFormat", "%C2%* %USER% %ACTION%");
	settings.put("ascOutputDeathFormat", "%C2%%USER%%C1% %DEATHMESSAGE%");
	settings.put("ascOutputAchievementFormat", "%C2%%USER%%C1% earned the achievement %C2%%ACHIEVEMENTNAME%%C1%!");
	engine.createConfigDefaults(settings);
	
	tailers = new ArrayList<IAWeSomeChatTailer>();
	
	tailers.add(new AWeSomeChatTailer(engine, "/home/minecraft/servers/survival/logs/latest.log", "Survival"));
	tailers.add(new AWeSomeChatTailer(engine, "/home/minecraft/servers/creative/logs/latest.log", "Creative"));
	tailers.add(new AWeSomeChatTailer(engine, "F:\\MC Server\\logs\\latest.log", "Test")); // Test server
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
	return 99999999L;
    }
    
    @Override
    public long getUpdateDelay() {
	return (engine == null ? 500 : Long.valueOf(engine.config.getProperty("ascUpdateDelay")));
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
    }
    
    @Override
    public int getDeadThreads() {
	int x = 0;
	for (IAWeSomeChatTailer a : tailers) {
	    if (a.isDead())
		x++;
	}
	return x;
    }
    
    	@Override
	public int getTotalThreads() {
	    return tailers.size();
	}

    @Override
    public List<Announcer> getAnnouncerList() {
	List<Announcer> a = new ArrayList<Announcer>();
	for (IAWeSomeChatTailer b : tailers)
	    a.add((Announcer)b);
	return a;
    }

    @Override
    public Throwable getLastExeption() {
	return lastException;
    }

    @Override
    public long getLastExceptionTime() {
	return lastExceptionTime;
    }

    @Override
    public long getLastUpdateTime() {
	return lastUpdate;
    }
    
    
}
