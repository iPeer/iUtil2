package com.simple.ipeer.iutil2.engine;

import javax.net.ssl.TrustManager;

public class SSLUtils {

	static TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
		new javax.net.ssl.X509TrustManager(){
			public java.security.cert.X509Certificate[] getAcceptedIssuers(){
				return null;
			}
			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType){}
			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType){}
		}
};
	
}
