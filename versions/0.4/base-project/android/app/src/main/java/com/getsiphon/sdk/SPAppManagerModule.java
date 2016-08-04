package com.getsiphon.sdk;

import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class SPAppManagerModule extends ReactContextBaseJavaModule {
    private final String TAG = "SPAppManagerModule";

    public SPAppManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "SPAppManager";
    }

    @ReactMethod
    public void presentApp(String appID, String authToken, boolean devMode) {
        Log.d(TAG, "presentApp(): ID=" + appID);
        ReactApplicationContext context = getReactApplicationContext();

        Intent intent = new Intent(context, SiphonDevelopmentAppActivity.class);
        intent.putExtra("scheme", System.getProperty("siphon.scheme"));
        intent.putExtra("host", System.getProperty("siphon.host"));
        intent.putExtra("baseVersion", System.getProperty("siphon.base-version"));
        intent.putExtra("appID", appID);
        intent.putExtra("authToken", authToken);
        intent.putExtra("sandboxMode", true);
        intent.putExtra("devMode", devMode);

        // Launch it. Note that we also need to set a flag because we're launching from a
        // context not an activity. It throws an error otherwise.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}