package com.opentok.android;

import java.net.MalformedURLException;
import java.net.URL;

public class OpenTokConfig {
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

    public static void enableSimulcast(PublisherKit publisher, boolean enable) {
        setPublisherVGASimulcastNative(publisher, enable);
    }

    public static void enableWebRTCLogs(boolean enable) {
        setWebRTCLogsNative(enable);
    }

    public static void enableJNILogs(boolean enable) {
        setJNILogsNative(enable);
    }

    public static void enableOTKLogs(boolean enable) {
        setOTKitLogsNative(enable);
    }

    private static native void setOTKitLogsNative(boolean otkitLogs);
    private static native void setJNILogsNative(boolean jniLogs);
    private static native void setWebRTCLogsNative(boolean webrtcLogs);
    private static native void setAPIRootURLNative(String host, boolean ssl, int port, boolean rumorSSL);
    private static native void setPublisherVGASimulcastNative(PublisherKit publisher, boolean enable);

    static {
        System.loadLibrary("opentok");
    }
}