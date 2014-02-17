package com.simple.ipeer.iutil2.admin;

import com.simple.ipeer.iutil2.engine.Main;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author iPeer
 */
public class Admin {
    
    protected Main engine;
    private File dataDir;

    public Admin(Main main) {
	this.engine = main;
	engine.log("Admin Control is starting", "Admin Control");
	HashMap<String, String> s = new HashMap<String, String>();
	s.put("acReauthDelay", "3600000");
	s.put("acRequireReauthOnAddressChange", "true");
	s.put("acDataDir", "./admin/");
	s.put("acDefaultUserLevel", "0");
	engine.createConfigDefaults(s);
	this.dataDir = new File(engine.config.getProperty("acDataDir"));
	if (!dataDir.exists())
	    dataDir.mkdirs();
	engine.log("Admin Control has started succesfully.", "Admin Control");
    }
    
    public void registerUser(String nick, String password) throws NoSuchAlgorithmException, NoSuchPaddingException, FileNotFoundException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
	
	KeyGenerator kg = KeyGenerator.getInstance("AES");
	Cipher ci = Cipher.getInstance("AES/ECB/PKCS5Padding");
	File userFile = new File(this.dataDir, nick+".iua");
	DataOutputStream out = new DataOutputStream(new FileOutputStream(userFile));
    
	kg.init(128);
	Key key = kg.generateKey();
	out.writeInt(key.getEncoded().length);
	out.write(key.getEncoded());
	
	ci.init(Cipher.ENCRYPT_MODE, key);
	byte[] pass = ci.doFinal(password.getBytes());
	out.writeInt(pass.length);
	out.write(pass);
	
	out.writeUTF(nick);
	
	out.writeInt(Integer.parseInt(engine.config.getProperty("acDefaultUserLevel")));
	
	out.flush();
	out.close();
	
    }
    
    public AdminUser loginUser(String nick, String password) throws NoSuchAlgorithmException, NoSuchPaddingException {
	
	Cipher ci = Cipher.getInstance("AES/ECB/PKCS5Padding");
	File userFile = new File(this.dataDir, nick+".iua");
	
	return new AdminUser(nick, 0);
    }

}
