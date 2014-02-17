package com.simple.ipeer.iutil2.reddit;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.AnnouncerHandler;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.util.CustomURLShortener;
import com.simple.ipeer.iutil2.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author iPeer
 */
public class GameDeals implements Announcer, AnnouncerHandler, Runnable, DebuggableSub {
    
    private Main main;
    LinkedList<String> linkCache = new LinkedList<String>();
    public long lastUpdate = 0L;
    private boolean isRunning = false;
    private Thread thread;
    public long startTime = 0L;
    private File filePath;
    private Throwable lastException;
    private long lastExceptionTime = 0L;
    
    public static void main(String[] args) {
	GameDeals g = new GameDeals(null);
	g.startIfNotRunning();
    }
    
    public GameDeals(Main main) {
	
	this.main = main;
	
	if (main != null) {
	    HashMap<String, String> s = new HashMap<String, String>();
	    s.put("rgdEnabled", "true");
	    s.put("rgdCacheDir", "./Reddit");
	    s.put("rgdUpdateDelay", "600000");
	    main.createConfigDefaults(s);
	}
	
	filePath = new File((main == null ? "./Reddit" : main.config.getProperty("rgdCacheDir")));
	if (!filePath.exists())
	    filePath.mkdirs();
	filePath = new File(filePath, "/GameDeals.iuc");
	linkCache = Util.loadLinkedListFromFile(filePath);
	//System.err.println(filePath.getAbsoluteFile());
	
    }
    
    @Override
    public void run() {
	
	while (isRunning && !thread.isInterrupted()) {
	    if (main == null || main.config.getProperty("rgdEnabled").equals("true")) {
		try {
		    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		    String URL = "http://www.reddit.com/r/GameDeals/new/.rss";
		    DocumentBuilder a = f.newDocumentBuilder();
		    Document doc = a.newDocument();
		    doc = a.parse(URL);
		    Element e = doc.getDocumentElement();
		    e.normalize();
		    
		    LinkedList<String> out = new LinkedList<String>();
		    
		    NodeList nodes = ((Element)e).getElementsByTagName("item");
		    
		    for (int x = 0; x < nodes.getLength() && x < 5; x++) {
			
			NodeList node = nodes.item(x).getChildNodes();
			
			String postTitle = ((Element)node).getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
			if (postTitle.length() >= 150)
			    postTitle = postTitle.substring(0, 147)+"...";
			String link = ((Element)node).getElementsByTagName("guid").item(0).getChildNodes().item(0).getNodeValue();
			if (linkCache.contains(link))
			    break;
			String description = ((Element)node).getElementsByTagName("description").item(0).getChildNodes().item(0).getNodeValue();
			String[] linkData = link.split("/");
			String shorterLink = "http://redd.it/"+linkData[linkData.length - 2];
			
			String saleLinkData = description.split("\">\\[link\\]")[0];
			String saleURL = saleLinkData.substring(saleLinkData.lastIndexOf("\"") + 1, saleLinkData.length()).replaceAll("&amp;", "&");
			if (saleURL.length() >= 60 && !link.equals(saleURL)) {
			    try {
				saleURL = CustomURLShortener.shorten(saleURL);
			    }
			    catch (Throwable ex) {
			        saleURL = link;
			    }
			}
			out.add("%C1%[%C2%/r/GameDeals%C1%] %C2%"+postTitle+"%C1% %DASH% %C2%"+shorterLink+(!link.equals(saleURL) ? "%C1% %DASH% %C2%"+saleURL : ""));
			linkCache.add(link);
			if (linkCache.size() > 10)
			    linkCache.remove(0);
			
			
		    }
		    
		    if (!out.isEmpty()) {
			for (Iterator<String> it = out.iterator(); it.hasNext();) {
			    String o = it.next();
			    if (main == null)
				System.err.println(o);
			    else
				main.send("PRIVMSG #QuestHelp :"+o);
			}
			Util.saveListToFile(linkCache, filePath);
		    }
		    
		}
		
		catch (Throwable ex) {
		    lastException = ex;
		    lastExceptionTime = System.currentTimeMillis();
		    if (main != null) {
			main.logError(ex, getThreadName());
			main.log("Couldn't update game deals announcer!", getThreadName());
		    }
		    else {
			ex.printStackTrace();
		    }
		    
		}
	    }
	    try {
		lastUpdate = System.currentTimeMillis();
		Thread.sleep(getUpdateDelay());
	    } catch (InterruptedException e) {
		lastException = e;
		lastExceptionTime = System.currentTimeMillis();
		stop();
		main.logError(e, getThreadName());
		main.log("Stopped due to sleep exception!", getThreadName());
	    }
	}
    }
    
    @Override
    public void update() throws IOException, ParserConfigurationException, SAXException {
    }
    
    @Override
    public long timeTilUpdate() {
	return (lastUpdate + getUpdateDelay()) - System.currentTimeMillis();
    }
    
    @Override
    public long getUpdateDelay() {
	return (main == null ? 600000L : Long.parseLong(main.config.getProperty("rgdUpdateDelay")));
    }
    
    @Override
    public void stop() {
	isRunning = false;
	thread.interrupt();
    }
    
    @Override
    public void start() {
	startTime = System.currentTimeMillis();
	isRunning = true;
	(thread = new Thread(this, getThreadName())).start();
    }
    
    @Override
    public void startIfNotRunning() {
	if (!isRunning)
	    start();
    }
    
    @Override
    public void removeCache() {
	
    }
    
    @Override
    public void stopIfRunning() {
	if (isRunning)
	    stop();
    }
    
    @Override
    public void shouldUpdate(boolean b) {
    }
    
    @Override
    public boolean isDead() {
	return timeTilUpdate() > 30L;
    }
    
    @Override
    public String getThreadName() {
	return "/r/GameDeals Announcer";
    }
    
    @Override
    public long getStartupTime() {
	return startTime;
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
	startIfNotRunning();
    }
    
    @Override
    public void stopAll() {
	stopIfRunning();
    }
    
    @Override
    public void update(String name) {
    }
    
    @Override
    public void scheduleThreadRestart(Object channel) {
    }
    
    @Override
    public int getDeadThreads() {
	return (isDead() ? 1 : 0);
    }
    
    @Override
    public int getTotalThreads() {
	return 1;
    }
    
    @Override
    public List<Announcer> getAnnouncerList() {
	List<Announcer> a = new ArrayList<Announcer>();
	a.add(this);
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
