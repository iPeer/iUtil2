package com.simple.ipeer.iutil2.minecraft.servicestatus;

import com.simple.ipeer.iutil2.engine.Announcer;
import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import com.simple.ipeer.iutil2.util.InterruptThread;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 *
 * @author iPeer
 */
public class MinecraftService implements IMinecraftService, Announcer, DebuggableSub {
    
    protected String url;
    protected HashMap<String, String> data;
    protected boolean useHTTPS;
    protected String reqMethod = "GET";
    protected Main engine;
    private long startupTime = 0L;
    public long lastUpdate = 0L;
    private long lastExceptionTime = 0L;
    private Throwable lastException;
    
    public MinecraftService(String url) {
	this.url = url;
	this.data = new HashMap<String, String>();
	this.engine = Main.getEngine();
    }
    
    @Override
    public HashMap<String, String> getData() {
	return this.data;
    }
    
    public void setMethod(String method) {
	this.reqMethod = method;
    }
    
    public void setHTTPS(boolean b) {
	if (this.url.toLowerCase().startsWith("https") && !b)
	    throw new IllegalStateException("URL Protocol is HTTPS, this connection must use HTTPS.");
	this.useHTTPS = b;
    }
    
    @Override
    public void update() {
	Long pingStart = 0L;
	data.put("status", "2");
	data.put("errorMessage", "Timed Out.");
	try {
	    HttpURLConnection con = null;
	    if (useHTTPS) {
		try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, SSLUtils.trustAll, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (GeneralSecurityException ex) {
		    lastException = ex;
		    lastExceptionTime = System.currentTimeMillis();
		    if (engine != null) {
			engine.log("Couldn't create SSL instance!", "MinecraftService");
			engine.logError(ex, "MinecraftService");
		    }
		    else {
			ex.printStackTrace();
		    }
		}
		con = (HttpsURLConnection)new URL(this.url).openConnection();
	    }
	    else {
		con = (HttpURLConnection)new URL(this.url).openConnection();
	    }
	    pingStart = System.currentTimeMillis();
	    con.setRequestMethod(reqMethod);
	    Thread t = new Thread(new InterruptThread(con, 5000));
	    t.start();
	    con.setConnectTimeout(5000);
	    con.setReadTimeout(5000);
	    con.connect();
	    data.put("status", Integer.toString(con.getResponseCode()));
	    data.remove("errorMessage");
	    if (t.isAlive())
		t.interrupt();
	} catch (UnknownHostException e) {
	    data.put("errorMessage", "Unknown Host");
	}
	catch (SocketTimeoutException e) {
	    data.put("errorMessage", "Timed out");
	}
	catch (ConnectException e) {
	    data.put("errorMessage", e.toString());
	}
	catch (ArrayIndexOutOfBoundsException e) {
	    data.put("errorMessage", e.toString());
	}
	catch (SocketException e) {
	    data.put("errorMessage", e.toString());
	}
	catch (Exception e) {
	    data.put("errorMessage", e.toString());
	}
	data.put("ping", Long.toString(System.currentTimeMillis() - pingStart));
	this.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public String getAddress() {
	return this.url.split("/")[2];
    }    

    @Override
    public long timeTilUpdate() {
	return (this.lastUpdate + engine.getAnnouncers().get("Minecraft Service Status").getUpdateDelay()) - System.currentTimeMillis();
    }

    @Override
    public void stop() {
    }

    @Override
    public void start() {
    }

    @Override
    public void startIfNotRunning() {
    }

    @Override
    public void removeCache() {
    }

    @Override
    public void stopIfRunning() {
    }

    @Override
    public void shouldUpdate(boolean b) {
    }

    @Override
    public boolean isDead() {
	return false;
    }

    @Override
    public String getThreadName() {
	return "Minecraft Service Status: "+this.url;
    }

    @Override
    public long getStartupTime() {
	return ((MinecraftServiceStatus)engine.getAnnouncers().get("Minecraft Service Status")).getStartupTime();
    }

    @Override
    public Throwable getLastExeption() {
	return this.lastException;
    }

    @Override
    public long getLastExceptionTime() {
	return this.lastExceptionTime;
    }

    @Override
    public long getLastUpdateTime() {
	return this.lastUpdate;
    }

    @Override
    public boolean addYTUser(String name, boolean isChannel) {
	return true;
    }
    
}
