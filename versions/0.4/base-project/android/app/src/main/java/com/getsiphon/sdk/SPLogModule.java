package com.getsiphon.sdk;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import javax.annotation.Nullable;

/**
 * We use this module for intercepting JS logging and relaying it to the streamer.
 */
public class SPLogModule extends ReactContextBaseJavaModule {
    @Nullable private SPLogListener mLogListener;

    public SPLogModule(ReactApplicationContext reactContext, @Nullable SPLogListener logListener) {
        super(reactContext);
        mLogListener = logListener;
    }

    @Override
    public String getName() {
        return "SPLog";
    }

    @ReactMethod
    public void log(String s) {
        if (mLogListener != null) {
            mLogListener.onMessage(s);
        }
    }
}
