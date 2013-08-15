package com.simple.ipeer.iutil2.util;


public class Filesize {
	
	public static String calculate(long bytes) {
		return calculate(bytes, false);
	}
	
	public static String calculate(long bytes, boolean si) { // Not my code, credit: http://stackoverflow.com/users/276052/aioobe
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + "iB";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.2f%sB", bytes / Math.pow(unit, exp), pre);
	}
	
/*	public static final String calculate(long bytes) {
		NumberFormat nf = NumberFormat.getInstance();
		DecimalFormat df = new DecimalFormat("#.##");
		System.err.println(984000L / 1024L);
		if (bytes < 1024)
			return nf.format(bytes)+"B";
		else if (bytes < 1048576)
			return df.format((float)(bytes / 1024L));
		else if (bytes < 1073741824)
			return df.format((double)(bytes / 1048576L));
		else if (bytes < 1099511627776L)
			return df.format((bytes / 1073741824L));
		else
			return df.format((bytes / 1099511627776L));
		
	}*/

}
