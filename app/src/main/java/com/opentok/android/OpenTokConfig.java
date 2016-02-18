package com.opentok.android;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by rpc on 03/02/16.
 */
public class OpenTokConfig {
    static {
        System.loadLibrary("opentok");
    }

    public static void setAPIRootURL(String apiRootURL, boolean rumorSSL) throws MalformedURLException {
        URL url = new URL(apiRootURL);
        boolean ssl = false;
        int port = url.getPort();
        if ("https".equals(url.getProtocol())) {
            ssl = true;
            if (port == -1) {
                port = 443;
            }
        } else if ("http".equals(url.getProtocol())) {
            ssl = false;
            if (port == -1) {
                port = 80;
            }
        }

        setAPIRootURLNative(url.getHost(), ssl, port, rumorSSL);
    }

    public static void enableWebRTCLogs(boolean enable) {
        (new OpenTokConfig()).setWebRTCLogsNative(enable);
    }

    private native void setWebRTCLogsNative(boolean webrtcLogs);
    private static native void setAPIRootURLNative(String host, boolean ssl, int port, boolean rumorSSL);

}