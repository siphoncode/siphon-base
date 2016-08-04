package com.getsiphon.sdk.network;


import android.content.Context;
import android.util.Log;

import com.getsiphon.sdk.models.SiphonConfig;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class SiphonStreamClient extends WebSocketAdapter {
    private final String TAG = "SiphonStreamClient";
    private final SiphonAPIClient mSiphonAPIClient;
    private String streamerURL = null;
    private WebSocket mWebSocket = null;

    protected SiphonStreamClient(Context context, SiphonConfig config) {
        this.mSiphonAPIClient = new SiphonAPIClient(context, config);
    }

    public abstract void handleError(String message);
    public abstract void handleMessage(String text);
    public abstract void handleConnected();
    public abstract String getConnectionType();

    private void setStreamerURL(String streamerURL) {
        this.streamerURL = streamerURL;
    }

    private void connectInner() {
        if (mWebSocket != null && mWebSocket.getState() == WebSocketState.OPEN) {
            Log.d(TAG, "connect(): websocket is already open, disconnecting first");
            mWebSocket.disconnect();
        }

        try {
            if (mWebSocket == null) {
                Log.d(TAG, "connect(): creating a new websocket");
                WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
                mWebSocket = factory.createSocket(streamerURL);
                mWebSocket.addListener(this);
            } else {
                Log.d(TAG, "connect(): recreating the websocket");
                mWebSocket = mWebSocket.recreate();
            }
            mWebSocket.connectAsynchronously();
        } catch (final IOException e) {
            e.printStackTrace();
            handleError("Failed to connect to the notification streamer: " +
                    e.getMessage());
        }
    }

    // Note: on iOS the initializer does this, but here we'll be explicit. You can call this
    // method multiple times and it knows how to reconnect if the socket is already open.
    public void connect() {
        if (streamerURL != null) {
            Log.d(TAG, "connect(): streamer URL is already cached");
            connectInner();
            return;
        }

        Log.d(TAG, "connect(): fetching streamer URL...");
        String connectionType = getConnectionType();
        mSiphonAPIClient.fetchStreamerURL(connectionType, new SiphonAPIClient.StringCallback() {
            public void success(String streamerURL) {
                Log.d(TAG, "connect(): received a streamer URL");
                setStreamerURL(streamerURL);
                connectInner();
            }

            public void error(final String message) {
                handleError(message);
            }
        });
    }

    public boolean isReady() {
        return (mWebSocket != null && mWebSocket.isOpen());
    }

    public void close() {
        if (mWebSocket != null) mWebSocket.disconnect(); // note: this call is non-blocking.
    }

    public void send(String message) {
        mWebSocket.sendText(message);
    }

    // -- WebSocketAdapter --

    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        handleMessage(text);
    }

    @Override
    public void onError(WebSocket websocket, final WebSocketException cause) throws Exception {
        // Note: this method is called before all other onXXError() methods, so we can
        // handle all error cases here.
        cause.printStackTrace();
        Log.d(TAG, "onError(): " + cause + ", message=" + cause.getMessage() +
                ", connectionType=" + getConnectionType());
        handleError("Notification stream error: " + cause.getMessage());
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        handleConnected();
    }
}
