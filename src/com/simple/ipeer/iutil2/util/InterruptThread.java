package com.simple.ipeer.iutil2.util;

import com.simple.ipeer.iutil2.engine.LogLevel;
import com.simple.ipeer.iutil2.engine.Main;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author iPeer
 */
public class InterruptThread implements Runnable {

    private final URLConnection con;
    private int timeout = 5000;
    private boolean ssl = false;
    
    
    public InterruptThread(HttpsURLConnection con, int timeout) {
	this.con = con;
	this.timeout = timeout;
	this.ssl = true;
    }
    
    public InterruptThread(HttpURLConnection con, int timeout) {
	this.con = con;
	this.timeout = timeout;
	this.ssl = false;
    }
    
    @Override
    public void run() {
	
	try {
	    Thread.sleep(timeout);
	}
	catch (InterruptedException e) { }
	catch (Throwable e) { }
	if (this.ssl)
	    ((HttpsURLConnection)this.con).disconnect();
	else
	    ((HttpURLConnection)this.con).disconnect();
	//Main.getMain().log("Forcing closure of socket as it is taking too long to respond.", "Minecraft Service Status", LogLevel.DEBUG_ONLY);
	
    }

}
