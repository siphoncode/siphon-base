package com.getsiphon.sdk.models;

import javax.annotation.Nullable;

public class SiphonConfig {
    public final String APP_ID;
    @Nullable public final String SUBMISSION_ID;
    @Nullable public final String AUTH_TOKEN;
    public final String SCHEME; // "http" or "https"
    public final String HOST;
    public final String BASE_VERSION;
    public final boolean SANDBOX_MODE;
    public final boolean DEV_MODE;

    private void checkScheme(String scheme) {
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new RuntimeException("SiphonConfig: scheme must be 'http' or 'https'.");
        }
    }

    public SiphonConfig(String appID, String authToken, String scheme, String host,
                        String baseVersion, boolean sandboxMode, boolean devMode) {
        checkScheme(scheme);
        this.APP_ID = appID;
        this.SUBMISSION_ID = null;
        this.AUTH_TOKEN = authToken;
        this.SCHEME = scheme;
        this.HOST = host;
        this.BASE_VERSION = baseVersion;
        this.SANDBOX_MODE = sandboxMode;
        this.DEV_MODE = devMode;
    }

    // Production constructor.
    public SiphonConfig(String appID, String submissionID, String scheme, String host,
                        String baseVersion) {
        checkScheme(scheme);
        this.APP_ID = appID;
        this.SUBMISSION_ID = submissionID;
        this.AUTH_TOKEN = null;
        this.SCHEME = scheme;
        this.HOST = host;
        this.BASE_VERSION = baseVersion;
        this.SANDBOX_MODE = false;
        this.DEV_MODE = false;
    }
}
