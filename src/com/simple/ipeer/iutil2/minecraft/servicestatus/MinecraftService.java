package com.simple.ipeer.iutil2.minecraft.servicestatus;

import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 *
 * @author iPeer
 */
public class MinecraftService implements IMinecraftService {
    
    protected String url;
    protected HashMap<String, String> data;
    protected boolean useHTTPS;
    protected String reqMethod = "GET";
    protected Main engine;
    
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
	try {
	    HttpURLConnection con = null;
	    if (useHTTPS) {
		try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, SSLUtils.trustAll, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (GeneralSecurityException ex) {
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
	    con.setConnectTimeout(5000);
	    con.setReadTimeout(5000);
	    data.put("status", Integer.toString(con.getResponseCode()));
	} catch (UnknownHostException e) {
	    data.put("errorMessage", "unknown Host");
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
    }

    @Override
    public String getAddress() {
	System.err.println(this.url);
	return this.url.split("/")[2];
    }
    
    
}