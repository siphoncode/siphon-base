package com.getsiphon.sdk.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.getsiphon.sdk.models.SiphonConfig;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SiphonNotificationStreamClient extends SiphonStreamClient {
    private final String TAG = "NotificationStream";
    private final Handler mHandler;
    private final Gson mGson = new Gson();
    private final Delegate delegate;
    private long lastNotificationTime = -1;

    public interface Delegate {
        void onNotificationReceived(String notificationType);
        void onNotificationStreamError(String message);
    }

    public SiphonNotificationStreamClient(Context context, SiphonConfig config,
                                          final SiphonNotificationStreamClient.Delegate delegate) {
        super(context, config);
        this.mHandler = new Handler(context.getMainLooper());
        this.delegate = delegate;
    }

    private void postToHandler(Runnable runnable) {
        mHandler.post(runnable);
    }

    @Override
    public String getConnectionType() {
        return "notifications";
    }

    @Override
    public void handleError(final String message) {
        postToHandler(new Runnable() {
            @Override
            public void run() {
                delegate.onNotificationStreamError(message);
            }
        });
    }

    // Limits notifications to one-per-second max.
    private boolean shouldPostNotification() {
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        boolean b = false;
        if (lastNotificationTime == -1 || (now - lastNotificationTime) > 1000) {
            b = true;
        }
        lastNotificationTime = now;
        return b;
    }

    @Override
    public void handleMessage(String text) {
        Log.d(TAG, "handleMessage(): '" + text + "'");

        // Parse the JSON -- an example message looks like this:
        // {"type": "app_updated", "app_id": "abcEaaba1a", user_id: "abcEaaba1a"}

        Type t = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> obj;
        try {
            obj = mGson.fromJson(text, t);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            handleError("Failed to parse notification: " + e.getMessage());
            return;
        }

        if (!obj.containsKey("type")) {
            handleError("Malformed notification, expecting a type: " + text);
            return;
        }

        if (shouldPostNotification()) {
            final String notificationType = obj.get("type");
            postToHandler(new Runnable() {
                public void run() {
                    delegate.onNotificationReceived(notificationType);
                }
            });
        } else {
            Log.d(TAG, "Not posting notification due to rate limiting.");
        }
    }

    @Override
    public void handleConnected() {
        Log.d(TAG, "Connected.");
    }
}
