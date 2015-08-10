package Tests;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

/**
 *
 * @author iPeer
 */
public class Tests {
    
    public static void main(String[] args) throws DatatypeConfigurationException {
	
	Tests t = new Tests();
	String[] testCases = new String [] { "PT1S", "PT1M10S", "PT10M7S", "PT4H12M7S", "PT10H", "PT7H1M45S" };
	
	for (String _case : testCases) {
	    System.err.println(formatTime(_case));
	}
	
    }
    
    public static String formatTime(String time) throws DatatypeConfigurationException {
	
	Duration dur = DatatypeFactory.newInstance().newDuration(time);
	
	String hours = pad(dur.getHours());
	String minutes = pad(dur.getMinutes());
	String secs = pad(dur.getSeconds());
	
	
	return (!hours.equals("00") ? hours+":" : "")+minutes+":"+secs;
	
	
	/*String hours = "";
	String min = "";
	if (tsd.length == 3) {
	    hours = tsd[0];
	    min = tsd[1];
	} else {
	    min = tsd[0];
	}
	
	String sec = tsd[tsd.length - 1];
	
	sec = pad(sec);
	min = pad(min);
	hours = pad(hours);
	
	String format = min + ":" + sec;
	
	if (!hours.equals("00") && !hours.equals("0")) {
	    format = hours + ":" + format;
	}
	
	return format;*/
	
    }

    private static String pad(int what) {
	return String.format("%02d", what);
    }
    
    public Tests() {
	
    }
    
}
