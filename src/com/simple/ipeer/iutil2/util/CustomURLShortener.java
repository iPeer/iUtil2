package com.simple.ipeer.iutil2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 *
 * @author iPeer
 */
public class CustomURLShortener {
    
    public CustomURLShortener() throws IOException {
	
    }
    
    public static String shorten(String URL, String cURL) throws MalformedURLException, IOException {
	
	String postURL = "http://ipeer.auron.co.uk/links/create";
	String urlParams = "sessionID="+Util.readEncrypted(new File("./urlshortener/apikey"))+"&user=iUtil2&type=url&url="+URLEncoder.encode(URL, "UTF-8")+(cURL.length() > 1 ? "&customurl="+cURL : "");
	byte[] urlBytes = urlParams.getBytes(Charset.forName("UTF-8"));
	URL request = new URL(postURL);
	HttpURLConnection connection = (HttpURLConnection)request.openConnection();
	connection.setRequestMethod("POST");
	connection.setDoInput(true);
	connection.setDoOutput(true);
	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	connection.setRequestProperty("charset", "utf-8");
	connection.setRequestProperty("Content-Length", Integer.toString(urlBytes.length));
	connection.setUseCaches(false);
	connection.getOutputStream().write(urlBytes);
	connection.getOutputStream().flush();
	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	String response = in.readLine();
	in.close();
	connection.disconnect();
	return response.split("\\|")[0];
    }

}
