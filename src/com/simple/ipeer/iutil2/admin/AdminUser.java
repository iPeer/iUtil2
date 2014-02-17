package com.simple.ipeer.iutil2.admin;

/**
 *
 * @author iPeer
 */
public class AdminUser {
    
    private long invalidTime = -1L;
    private String nick = "MISSINGNO.";
    private int userLevel = 0;
    private String userAddress = "";
    private String userIdent = "";

    public AdminUser(String nick, int userLevel) {
	this.nick = nick;
	this.userLevel = userLevel;
    }
    
    public int getUserLevel() {
	return this.userLevel;
    }
    
    public boolean isInvalid(long time) {
	return this.invalidTime == -1L || (time - this.invalidTime) > 3600000;
    }
    
    public boolean isInvalid(String ident, String address) {
	return !ident.equals(this.userIdent) || !address.equals(this.userAddress);
    }

}
