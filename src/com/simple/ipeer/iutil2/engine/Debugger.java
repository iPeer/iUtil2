package com.simple.ipeer.iutil2.engine;

import com.simple.ipeer.iutil2.minecraft.servicestatus.IMinecraftService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author iPeer
 */
public class Debugger {
    
    protected Main engine;
    
    public Debugger(Main main) {
	this.engine = main;
    }

    public void writeDebug() throws IOException {
    	File a = new File((engine == null ? "logs/" : engine.logDir)+"/debug-"+System.currentTimeMillis()+".txt");
	writeDebug(new FileWriter(a));
    }
    public void writeDebug(FileWriter fw) throws IOException {
	DateFormat df = DateFormat.getDateTimeInstance();
	df.setTimeZone(TimeZone.getTimeZone("UTC"));
	fw.write("Attempting to dump debug information.");
	fw.write("\nDebug report generated at "+df.format(new Date(System.currentTimeMillis())).toString()+". All times are GMT.\n\n");
	for (String i : engine.generateInfoOutput())
	    fw.write("\t"+i+"\n");
	fw.write("\n");
	for (String b : engine.getAnnouncers().keySet()) {
	    AnnouncerHandler ah = engine.getAnnouncers().get(b);
	    if (b.equals("Minecraft Service Status"))
		continue;
	    fw.write("Dumping debug information for the AnnouncerHandler "+b+"\n\n");
	    fw.write("Threads: "+ah.getTotalThreads()+", dead: "+ah.getDeadThreads()+"\n\n");
	    for (Announcer an : engine.getAnnouncers().get(b).getAnnouncerList()) {
		if (an instanceof IMinecraftService)
		    continue;
		DebuggableSub d = (DebuggableSub)an;
		
		fw.write("\t"+an.getThreadName()+"\n\t\tLast exception: "+(d.getLastExeption() == null ? "none" : d.getLastExeption().toString())+"\n");
		if (d.getLastExeption() != null) {
		    fw.write("\t\tStacktrace: \n");
		    for (StackTraceElement s : d.getLastExeption().getStackTrace())
			fw.write("\t\t\t\t"+s.toString()+"\n");
		}
		fw.write("\t\tException time: "+(d.getLastExceptionTime() == 0L ? "never" : df.format(new Date(d.getLastExceptionTime())))+"\n\n");
	    }
	    fw.write("\tDead threads are marked with an asterisk\n\n");
	    if (engine.getAnnouncers().get(b).getAnnouncerList().isEmpty())
		fw.write("\tNo threads available for this AnnouncerHandler\n");
	    for (Announcer an : engine.getAnnouncers().get(b).getAnnouncerList()) {
		fw.write("\t"+an.getThreadName()+(an.isDead() ? "*" : "")+" (running since "+df.format(new Date(an.getStartupTime()))+", last update at "+(((DebuggableSub)an).getLastUpdateTime() == 0L ? "never" : df.format(new Date(((DebuggableSub)an).getLastUpdateTime())))+")\n");
	    }
	    fw.write("\n\n");
	}
	fw.write("Debug dump complete.");
	fw.close();
    }
    
    

}
