package com.simple.ipeer.iutil2.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author iPeer
 */
public class Util {
    
    public static LinkedList<String> loadLinkedListFromFile(File file) {
	
	LinkedList<String> list = new LinkedList<String>();
	if (file.exists()) {
	    try {
		Scanner s = new Scanner(file);
		while (s.hasNext())
		    list.add(s.next());
	    } catch (FileNotFoundException ex) {
		// This should never be thrown because we have an if to check it...
	    }
	    
	}
	
	return list;
    }
    
    public Util() {
	
    }
    
    public static List<String> loadListFromFile(File file) throws InstantiationException, IllegalAccessException { // Chances are it's a string if we're loading from a file
	
	List<String> list = new ArrayList<String>();
	
	if (file.exists()) {
	    try {
		Scanner s = new Scanner(file);
		while (s.hasNext())
		    list.add(s.next());
	    } catch (FileNotFoundException ex) {
		// This should never be thrown because we have an if to check it...
	    }
	    
	}
	
	return list;
	
    }
    
    @SuppressWarnings("ConvertToTryWithResources")
    public static void saveListToFile(List<String> list, File file) throws IOException {
	
	if (list.isEmpty())
	    return;
	
	FileWriter w = new FileWriter(file, false);
	
	for (String a : list)
	    w.write(a+"\n");
	
	w.close();
	
    }
    
    public static void writeEncrypted(String data, File file) throws IOException {
	DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
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
	    byte[] o1 = ci.doFinal(data.getBytes());
	    out.writeInt(o1.length);
	    out.write(o1);
	} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
	    e.printStackTrace();
	    out.close();
	}
	out.close();
    }
    
    public static String readEncrypted(File file) {
	Key key;
	String decrypted = "";
	try {
	    Cipher ci = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    DataInputStream in = new DataInputStream(new FileInputStream(file));
	    byte[] ke = new byte[in.readInt()];
	    in.readFully(ke);
	    key = new SecretKeySpec(ke, "AES");
	    ci.init(Cipher.DECRYPT_MODE, key);
	    byte[] pass = new byte[in.readInt()];
	    in.readFully(pass);
	    decrypted = new String(ci.doFinal(pass));
	} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException | NoSuchAlgorithmException | NoSuchPaddingException e) {
	    return new String();
	}
	return decrypted;
    }
    
}
