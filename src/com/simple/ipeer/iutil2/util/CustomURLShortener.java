package com.simple.ipeer.iutil2.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
    
    public static void main(String[] args) throws IOException {
	System.err.println(shorten("http://mc.auron.co.uk"));
    }
    
    public static String shorten(String URL) throws MalformedURLException, IOException {
	
	String postURL = "http://ipeer.auron.co.uk/links/create";
	String urlParams = "sessionID=s9A62CXMAv78rRFc1G77PO930&user=iUtil2&type=url&url="+URLEncoder.encode(URL, "UTF-8"); //TODO: Move the SID into a file at some point
	//System.out.println("Shortening link '"+URL+"'. urlParams: "+urlParams);
	byte[] urlBytes = urlParams.getBytes(Charset.forName("UTF-8"));
	URL request = new URL(postURL);
	HttpURLConnection connection = (HttpURLConnection)request.openConnection();
	connection.setRequestMethod("POST");
	connection.setDoInput(true);
	connection.setDoOutput(true);
	//connection.setInstanceFollowRedirects(false);
	//connection.setReadTimeout(5000);
	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	connection.setRequestProperty("charset", "utf-8");
	connection.setRequestProperty("Content-Length", Integer.toString(urlBytes.length));
	connection.setUseCaches(false);
	connection.getOutputStream().write(urlBytes);
	connection.getOutputStream().flush();
	//DataOutputStream out = new DataOutputStream(connection.getOutputStream());
	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	//out.write(urlBytes);
	//out.flush();
	//out.close();
	String response = in.readLine();
	//System.out.println("Server response: "+response);
	//while ((response = in.readLine()) != null)
	//    System.err.println(response);
	//out.close();
	in.close();
	connection.disconnect();
	return response.split("\\|")[0];
    }

}
