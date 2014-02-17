package com.simple.ipeer.iutil2.engine;

import com.simple.ipeer.iutil2.irc.protocol.Protocol;

/**
 *
 * @author iPeer
 */
public class OfflineMessageSender {
    
    private Main engine;

    public OfflineMessageSender(Main engine) {
	
	this.engine = engine;
	
    }
    
    public void sendMessages() {
	for (OfflineMessage om : engine.offlineMessages) {
	    engine.send(om.getText(), om.shouldLog(), om.shouldSendWhenNotConnected());
	}
    }

}
