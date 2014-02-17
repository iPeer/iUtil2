package com.simple.ipeer.iutil2.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.simple.ipeer.iutil2.engine.Main;


public class Profiler {
    
    private String sectionName = "";
    private List<String> sectionNames = new ArrayList<String>();
    private List<Long> sectionTimestamps = new ArrayList<Long>();
    private HashMap<String, Long> profilerData = new HashMap<String, Long>();
    private File profilerLog;
    private FileWriter profilerWriter;
    private Main engine;
    
    public Profiler(Main engine) {
	this.engine = engine;
	if (engine != null) {
	    this.engine.log("Profiler is attempting to start...", "Profiler");
	    HashMap<String, String> defaultConfig = new HashMap<String, String>();
	    defaultConfig.put("profilerWarningTime", "100000000");
	    defaultConfig.put("profilerLogEnabled", "true");
	    this.engine.createConfigDefaults(defaultConfig);
	}
	if (engine == null || engine.config.getProperty("profilerLogEnabled").equals("true")) {
	    profilerLog = new File((engine == null ? "./logs/profiler.log" : this.engine.logDir+"/profiler.log"));
	    try {
		profilerWriter = new FileWriter(profilerLog, true);
	    } catch (IOException e) {
		if (this.engine != null) {
		    this.engine.log("Couldn't create FileWriter for Profiler log, output will not be logged!", "Profiler");
		    this.engine.logError(e, "Profiler");
		}
		else
		    e.printStackTrace();
	    }
	    logData((profilerLog.length() > 0L ? "\n\n" : "")+"-- Profiler logging session starting at "+(new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date(System.currentTimeMillis()))+" --\n");
	}
	if (engine != null)
	    this.engine.log("Profiler was started succesfully.", "Profiler");
    }
    
    public void startSection(String name) {
	
	if ((this.engine == null ? true : Main.getMain().profilingEnabled())) {
	    
	    this.sectionName = (this.sectionName.length() > 0 ? this.sectionName+"." : "")+name;
	    this.sectionNames.add(this.sectionName);
	    this.sectionTimestamps.add(System.nanoTime());
	    
	}
    }
    
    public void start(String name) {
	startSection(name);
    }
    
    public void endSection() {
	if (this.engine == null || Main.getMain().profilingEnabled()) {
	    Main engine = Main.getMain();
	    long now = System.nanoTime();
	    long then = ((Long)this.sectionTimestamps.remove(this.sectionTimestamps.size() - 1)).longValue();
	    this.sectionNames.remove(this.sectionNames.size() - 1);
	    long difference = now - then;
	    
	    this.profilerData.put(this.sectionName, difference);
	    
	    String out = "'"+this.sectionName+"' complete after "+(difference / 1000000.0D)+"ms.";
	    
	    if (difference > Long.valueOf((engine == null ? "60000000000" : this.engine.config.getProperty("profilerWarningTime", "60000000000")))) {
		out = "Something is running slow! '"+this.sectionName+"' took "+(difference / 1000000.0D)+"ms!";
		if (engine != null && engine.config.getProperty("profilerLogEnabled").equals("true"))
		    this.engine.send("PRIVMSG "+this.engine.config.getProperty("debugChannel")+" :"+out);
		else
		    System.err.println(out);
		
	    }
	    
	    if (engine != null && engine.config.getProperty("debug").equals("true"))
		engine.log(out, "Profiler");
	    
	    if (engine == null || this.engine.config.getProperty("profilerLogEnabled").equals("true"))
		logData(out);
	    this.sectionName = (!this.sectionNames.isEmpty() ? this.sectionNames.get(this.sectionNames.size() - 1) : "");
	    
	}
    }
    
    public void end() {
	endSection();
    }
    
    private void logData(String data) {
	if (engine == null || this.engine.config.getProperty("profilerLogEnabled").equals("true")) {
	    try {
		profilerWriter.write(data+"\r\n");
		profilerWriter.flush();
	    } catch (IOException e) {
		if (this.engine != null) {
		    this.engine.log("Couldn't log profiler data!", "Profiler");
		    this.engine.logError(e, "Profiler");
		}
		else
		    e.printStackTrace();
	    }
	}
    }
    
    public HashMap<String, Long> profileData() {
	return this.profilerData;
    }

}
