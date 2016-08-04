package com.getsiphon.sdk.network;

import android.content.Context;
import android.os.Handler;

import com.getsiphon.sdk.models.SiphonConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SiphonLogStreamClient extends SiphonStreamClient {
    private final String TAG = "SiphonLogStreamClient";
    private final Handler mHandler;
    private final Delegate delegate;
    private final ArrayList<String> logBuffer = new ArrayList<>();
    private final DateFormat mDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    private final Calendar mCalendar = Calendar.getInstance();

    public interface Delegate {
        void onLogStreamError(String message);
    }

    public SiphonLogStreamClient(Context context, SiphonConfig config,
                                 final SiphonLogStreamClient.Delegate delegate) {
        super(context, config);
        this.mHandler = new Handler(context.getMainLooper());
        this.delegate = delegate;
    }

    private void postToHandler(Runnable runnable) {
        mHandler.post(runnable);
    }

    private String processLogMessage(String message) {
        // Generate a timestamp
        String timestamp = mDateFormat.format(mCalendar.getTime());

        // TODO: do we need to strip out the single quote marks on Android? (see iOS for how)

        // Truncate
        int maxLength = 1024 * 25;
        if (message.length() > maxLength) {
            message = message.substring(0, maxLength) + " ... [LOG TRUNCATED]\n";
        }

        return String.format("[%s] %s", timestamp, message);
    }

    public void broadcastLog(String message) {
        logBuffer.add(processLogMessage(message));
        broadcastBufferedLogs();
    }

    synchronized private void broadcastBufferedLogs() {
        if (isReady()) {
            while (logBuffer.size() > 0) {
                String message = logBuffer.remove(0);
                send(message);
            }
        }
    }

    @Override
    public String getConnectionType() {
        return "log_writer";
    }

    @Override
    public void handleError(final String message) {
        postToHandler(new Runnable() {
            @Override
            public void run() {
                delegate.onLogStreamError(message);
            }
        });
    }

    @Override
    public void handleMessage(String text) {
        // Do nothing.
    }

    @Override
    public void handleConnected() {
        broadcastBufferedLogs();
    }
}
