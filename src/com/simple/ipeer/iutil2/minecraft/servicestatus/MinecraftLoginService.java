package com.simple.ipeer.iutil2.minecraft.servicestatus;

import com.simple.ipeer.iutil2.engine.DebuggableSub;
import com.simple.ipeer.iutil2.irc.SSLUtils;
import com.simple.ipeer.iutil2.util.InterruptThread;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 *
 * @author iPeer
 */
public class MinecraftLoginService extends MinecraftService implements IMinecraftService, DebuggableSub {
    
    private MinecraftServiceStatus mcss;
    
    public MinecraftLoginService(String url, MinecraftServiceStatus mcss) {
	super(url);
	this.mcss = mcss;
    }
    
    public static void main(String[] args) throws IOException {
	new MinecraftLoginService(null, null).writeLogin("user", "pass");
    }
    
    @Override
    public void update() {
	data.put("status", "2");
	data.put("errorMessage", "Timed Out.");
	long pingStart = 0L;
	String line = "";
	try {
	    String[] login = readLogin();
	    SSLContext sc = SSLContext.getInstance("SSL");
	    sc.init(null, SSLUtils.trustAll, new java.security.SecureRandom());
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	    String params = "user="+login[0]+"&password="+login[1]+"&version=13";
	    pingStart = System.currentTimeMillis();
	    HttpsURLConnection con = (HttpsURLConnection)new URL(url).openConnection();
	    con.setRequestMethod("POST");
	    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	    con.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
	    con.setRequestProperty("Content-Language", "en-US");
	    con.setUseCaches(false);
	    con.setDoInput(true);
	    con.setDoOutput(true);
	    con.setReadTimeout(5000);
	    con.setConnectTimeout(5000);
	    Thread t = new Thread(new InterruptThread(con, 5000));
	    t.start();
	    con.connect();
	    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	    wr.writeBytes(params);
	    wr.flush();
	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    line = in.readLine();
	    //System.err.println(line);
//	    if (line.contains(":"))
//		this.mcss.setSSID(line.split(":")[3]); // Not actually used right now
//	    else
//		data.put("errorMessage", "Server gave an invalid response: "+line);
	    in.close();
	    wr.close();
	    data.put("status", Integer.toString(con.getResponseCode()));
	    data.remove("errorMessage");
	    if (t.isAlive())
		t.interrupt();
	} catch (NoSuchAlgorithmException e) {
	    data.put("errorMessage", "Couldn't read login credentials.");
	}
	catch (UnknownHostException e) {
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
	data.put("response", line);
	data.put("ping", Long.toString(System.currentTimeMillis() - pingStart));
	lastUpdate = System.currentTimeMillis();
    }
    
    private String[] readLogin() {
	Key key;
	String u = "", p = "";
	try {
	    Cipher ci = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    DataInputStream in = new DataInputStream(new FileInputStream(new File("./Minecraft/credentials.iaf")));
	    byte[] ke = new byte[in.readInt()];
	    in.readFully(ke);
	    key = new SecretKeySpec(ke, "AES");
	    ci.init(Cipher.DECRYPT_MODE, key);
	    byte[] pass = new byte[in.readInt()];
	    in.readFully(pass);
	    p = new String(ci.doFinal(pass));
	    byte[] user = new byte[in.readInt()];
	    in.readFully(user);
	    u = new String(ci.doFinal(user));
	    in.close();
	} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException | NoSuchAlgorithmException | NoSuchPaddingException e) {
	    data.put("errorMessage", e.toString());
	}
	String[] s = {u, p};
	return s;
    }
    
    //Taken from iUtil1, hooray.
    private void writeLogin(String u, String p) throws IOException {
	DataOutputStream out = new DataOutputStream(new FileOutputStream(new File("./Minecraft/credentials.iaf")));
	KeyGenerator kg;
	Key key;
	Cipher ci;
	try {
	    kg = KeyGenerator.getInstance("AES");
	    kg.init(128);
	    key = kg.generateKey();
	    out.writeInt(key.getEncoded().length);
	    ci = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    ci.init(Cipher.ENCRYPT_MODE, key);
	    out.write(key.getEncoded());
	    byte[] pass = ci.doFinal(p.getBytes());
	    out.writeInt(pass.length);
	    out.write(pass);
	    byte[] user = ci.doFinal(u.getBytes());
	    out.writeInt(user.length);
	    out.write(user);
	} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
	    e.printStackTrace();
	    out.close();
	}
	out.close();
    }
    
}
