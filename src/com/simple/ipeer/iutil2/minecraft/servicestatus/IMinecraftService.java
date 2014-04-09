package com.simple.ipeer.iutil2.minecraft.servicestatus;

import java.net.URLConnection;
import java.util.HashMap;

/**
 *
 * @author iPeer
 */
public interface IMinecraftService {
    
    public HashMap<String, String> getData();
    public void update();
    public String getAddress();
    //public URLConnection getConnection();

}
