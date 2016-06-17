package Tests;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author iPeer
 */
public class TwitchClientIDWriter {

    public TwitchClientIDWriter() { }
	
    public static void main(String[] args) throws IOException {
	
	com.simple.ipeer.iutil2.util.Util.writeEncrypted("[redacted]", new File("./Twitch/config/clientid.uic"));
	
    }
	

}
