package com.simple.ipeer.iutil2.twitter;

import com.simple.ipeer.iutil2.engine.LogLevel;
import com.simple.ipeer.iutil2.engine.Main;
import com.simple.ipeer.iutil2.util.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author iPeer
 */
public class Twitter {
    
    Main main;
    
    public Twitter(Main main) {
	this.main = main;
	HashMap<String, String> settings = new HashMap<String, String>();
	settings.put("tMaxReplies", "2");
	settings.put("tReplaceLinks", "true");
	settings.put("tUseRealNames", "true");
	if (main != null)
	    main.createConfigDefaults(settings);
    }
    
//    public static void main(String[] args) throws IOException, ParseException {
//
//    }
    
    @Deprecated
    public void writeAPIPrivate(String key) throws IOException {
	writeAPIPrivate(key, new File("./twitter/apiprivate"));
    }
    
    public void writeAPIPrivate(String key, File file) throws IOException {
	new File("./twitter/").mkdirs();
	Util.writeEncrypted(key, file);
    }
    
    public String readAPIPrivate(File file) {
	return Util.readEncrypted(file);
    }
    
    public void authorize() throws UnsupportedEncodingException, MalformedURLException, IOException {
	log("Reading API and Private API keys from file");
	String pk = URLEncoder.encode(Util.readEncrypted(new File("./twitter/apiprivate")), "UTF-8");
	String apik = URLEncoder.encode(Util.readEncrypted(new File("./twitter/apikey")), "UTF-8");
	log("Converting keys to Base64.");
	String base64 = new String(Base64.encodeBase64(new String(apik+":"+pk).getBytes()));
	log("Creating a connection to the Twitter API.");
	
	URL url = new URL("https://api.twitter.com/oauth2/token");
	HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
	connection.setDoInput(true);
	connection.setDoOutput(true);
	connection.setRequestMethod("POST");
	connection.setRequestProperty("Host", "api.twitter.com");
	connection.setRequestProperty("User-Agent", "iUtil");
	connection.setRequestProperty("Authorization", "Basic " + base64);
	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	connection.setRequestProperty("Content-Length", "29");
	connection.setUseCaches(false);
	
	log("Attempting to authorize with Twitter.");
	
	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
	out.write("grant_type=client_credentials");
	out.flush();
	out.close();
	
	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	String authCode = in.readLine();
	
	JSONObject json = (JSONObject)JSONValue.parse(authCode);
	String token = (String)json.get("access_token");
	
	log("Token = "+token, LogLevel.DEBUG_ONLY);
	
	log("Successfully authorized with Twitter. Storing bearer key for later use.");
	Util.writeEncrypted(token, new File("./twitter/bearer"));
	
	in.close();
	connection.disconnect();
	
    }
    
    public void deauthorise() throws IOException { // Hopefully this never needs to be used
	log("Deauthorizing Twitter bearer key. Any future twitter API requests will require reauthorization before they are completed.");
	log("Reading API and Private API keys from file");
	String pk = URLEncoder.encode(Util.readEncrypted(new File("./twitter/apiprivate")), "UTF-8");
	String apik = URLEncoder.encode(Util.readEncrypted(new File("./twitter/apikey")), "UTF-8");
	log("Converting keys to Base64.");
	String base64 = new String(Base64.encodeBase64(new String(apik+":"+pk).getBytes()));
	log("Creating a connection to the Twitter API.");
	
	URL url = new URL("https://api.twitter.com/oauth2/invalidate_token");
	HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
	connection.setDoInput(true);
	connection.setDoOutput(true);
	connection.setRequestMethod("POST");
	connection.setRequestProperty("Host", "api.twitter.com");
	connection.setRequestProperty("User-Agent", "iUtil");
	connection.setRequestProperty("Authorization", "Basic " + base64);
	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	connection.setRequestProperty("Content-Length", "119");
	connection.setUseCaches(false);
	if (!new File("./twitter/bearer").delete()) {
	    log("[WARN] Couldn't delete bearer key file. Well try again when the bot terminates.");
	    new File("./twitter/bearer").deleteOnExit();
	}
	log("Successfully deauthorized.");
    }
    
    public String getTweetData(String tweetID) throws IOException, ParseException {
	if (!new File("./twitter/bearer").exists())
	    authorize();
	URL url = new URL("https://api.twitter.com/1.1/statuses/show.json?id="+tweetID);
	HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
	connection.setDoInput(true);
	connection.setDoOutput(true);
	connection.setRequestMethod("GET");
	connection.setRequestProperty("Host", "api.twitter.com");
	connection.setRequestProperty("User-Agent", "iUtil");
	connection.setRequestProperty("Authorization", "Bearer " + Util.readEncrypted(new File("./twitter/bearer")));
	connection.setUseCaches(false);
	
	BufferedReader in = null;
	if (connection.getResponseCode() != 200) {
	    in = new BufferedReader (new InputStreamReader(connection.getErrorStream()));
	    String a = "";
	    while ((a = in.readLine()) != null) {
		System.err.println(a);
	    }
	}
	
	in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	String data = "";
	StringBuilder str = new StringBuilder();
	while ((data = in.readLine()) != null) {
	    str.append(data);
	    str.append("\n");
	}
	JSONObject json = (JSONObject)JSONValue.parse(str.toString());
	String tweet = json.get("text").toString().replaceAll("\\n", " ");
	String name = "";
	boolean verified = ((JSONObject)json.get("user")).get("verified").toString().equals("true");
	if (main != null && main.config.getProperty("tUseRealNames", "true").equals("true"))
	    name = ((JSONObject)json.get("user")).get("name").toString();
	else
	    name = "@"+((JSONObject)json.get("user")).get("screen_name").toString();
	JSONArray urls = (JSONArray)((JSONObject)(json.get("entities"))).get("urls");
	if (main == null || main.config.getProperty("tReplaceLinks", "true").equals("true"))
	    for (Object o : urls.toArray()) {
		JSONObject ob = (JSONObject)JSONValue.parse(o.toString());
		String turl = ob.get("url").toString();
		String fullURL = ob.get("expanded_url").toString();
		tweet = tweet.replaceAll(turl, fullURL);
	    }
	return Main.BOLD+(verified ? Main.COLOUR+"03" : Main.COLOUR+"13")+name+Main.ENDALL+Main.COLOUR+"14: "+tweet;
    }
    
    public void getAPIUsage() { 
	
    }
    
    public void log(String line) {
	log(line, LogLevel.LOG_DEBUG_AND_CHANNEL);
    }
    
    public void log(String line, LogLevel l) {
	if (this.main == null)
	    System.err.println(line);
	else
	    this.main.log(line, "Twitter", l);
    }
    
}
