package com.getsiphon.sdk.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.getsiphon.sdk.models.SiphonConfig;
import com.getsiphon.sdk.models.SiphonHash;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SiphonAPIClient {
    private final String TAG = "SiphonAPIClient";
    private final Handler mHandler;
    private final Gson mGson = new Gson();
    private final OkHttpClient client = new OkHttpClient();
    private final SiphonConfig mSiphonConfig;

    private interface Callback<T> {
        void success(T result);
        void error(String message);
    }

    public interface StringCallback extends SiphonAPIClient.Callback<String> {}
    public interface BytesCallback extends SiphonAPIClient.Callback<byte[]> {}
    public interface MapCallback extends SiphonAPIClient.Callback<Map<String, String>> {}

    public SiphonAPIClient(Context context, SiphonConfig config) {
        this.mHandler = new Handler(context.getMainLooper());
        this.mSiphonConfig = config;
    }

    private void postToHandler(Runnable runnable) {
        mHandler.post(runnable);
    }

    private Call makeCall(String url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json");
        if (mSiphonConfig.AUTH_TOKEN != null) {
            builder = builder.addHeader("X-Siphon-Token", mSiphonConfig.AUTH_TOKEN);
        }
        return this.client.newCall(builder.build());
    }

    private void handleJSONResponse(Response response, final String key,
                                    final StringCallback callback) {
        Type t = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> obj;
        final String s;
        try {
            s = response.body().string();
            obj = mGson.fromJson(s, t);
        } catch (Exception e) {
            e.printStackTrace();
            postToHandler(new Runnable() {
                public void run() {
                    callback.error("Unexpected error while fetching '" + key + "'.");
                }
            });
            return;
        }
        if (obj.containsKey(key)) {
            final String result = obj.get(key);
            postToHandler(new Runnable() {
                public void run() {
                    callback.success(result);
                }
            });
        } else if (obj.containsKey("message")) {
            final String message = obj.get("message");
            postToHandler(new Runnable() {
                public void run() {
                    callback.error(message);
                }
            });
        } else {
            postToHandler(new Runnable() {
                public void run() {
                    callback.error("Unexpected response from the server while fetching a '" + key
                            + "': " + s);
                }
            });
        }
    }

    public Call fetchStreamerURL(String connectionType, final StringCallback callback) {
        String url = String.format(
                "%s://%s/api/v1/streamers/?app_id=%s&type=%s",
                mSiphonConfig.SCHEME, mSiphonConfig.HOST, mSiphonConfig.APP_ID, connectionType);
        Call call = makeCall(url);
        call.enqueue(new com.squareup.okhttp.Callback() {
            public void onFailure(Request request, final IOException e) {
                postToHandler(new Runnable() {
                    public void run() {
                        callback.error("Error communicating with the server: " + e.getMessage());
                    }
                });
            }

            public void onResponse(Response response) throws IOException {
                handleJSONResponse(response, "streamer_url", callback);
            }
        });
        return call;
    }

    public Call fetchAssets(Collection<SiphonHash> hashes, String bundlerURL,
                            final BytesCallback callback) {
        // Parse the hashes to JSON.
        Map<String, String> hashesObject = new HashMap<>();
        for (SiphonHash hash : hashes) {
            hashesObject.put(hash.name, hash.sha256);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("asset_hashes", hashesObject);
        String content = mGson.toJson(data);
        Log.d(TAG, "fetchAssets(): payload: " + content);

        // Build a request.
        MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonMediaType, content);
        Request request = new Request.Builder()
                .url(bundlerURL)
                .post(body)
                .build();

        Call call = this.client.newCall(request);
        call.enqueue(new com.squareup.okhttp.Callback() {
            public void onFailure(Request request, final IOException e) {
                postToHandler(new Runnable() {
                    public void run() {
                        callback.error("Error communicating with the server: " + e.getMessage());
                    }
                });
            }

            public void onResponse(final Response response) {
                Log.d(TAG, "fetchAssets(): response=" + response.code());
                if (response.code() != 200) {
                    postToHandler(new Runnable() {
                        public void run() {
                            String body;
                            try {
                                body = response.body().string();
                                Log.d(TAG, "fetchAssets(): body=" + body);
                            } catch (IOException e) {
                                body = "<failed to read body>";
                                Log.d(TAG, "fetchAssets(): failed to read body");
                            }
                            callback.error("Error pulling from the server: " + body);
                        }
                    });
                    return;
                }

                final byte[] zipData;
                try {
                    zipData = response.body().bytes();
                    Log.d(TAG, "fetchAssets(): got bytes: " + zipData.length);
                } catch (final IOException e) {
                    e.printStackTrace();
                    postToHandler(new Runnable() {
                        public void run() {
                            callback.error("Error communicating with the server: " +
                                    e.getMessage());
                        }
                    });
                    return;
                }
                postToHandler(new Runnable() {
                    public void run() {
                        callback.success(zipData);
                    }
                });
            }
        });
        return call;
    }

    public Call fetchBundlerURL(final StringCallback callback) {
        SiphonConfig conf = mSiphonConfig;
        String url = String.format(
                "%s://%s/api/v1/bundlers/?app_id=%s&base_version=%s&action=pull&platform=android",
                conf.SCHEME, conf.HOST, conf.APP_ID, conf.BASE_VERSION);
        Log.d(TAG, "fetchBundlerURL() from: " + url);
        Call call = makeCall(url);
        call.enqueue(new com.squareup.okhttp.Callback() {
            public void onFailure(Request request, final IOException e) {
                postToHandler(new Runnable() {
                    public void run() {
                        callback.error("Error communicating with the server: " + e.getMessage());
                    }
                });
            }

            public void onResponse(Response response) throws IOException {
                handleJSONResponse(response, "bundler_url", callback);
            }
        });
        return call;
    }

    public Call fetchDataForSubmission(final String currentSubmissionID,
                                       final MapCallback callback) {
        SiphonConfig conf = mSiphonConfig;
        String url = String.format(
                "%s://%s/api/v1/bundlers/?action=pull&current_submission_id=%s",
                conf.SCHEME, conf.HOST, currentSubmissionID);
        Log.d(TAG, "fetchDataForSubmission() from: " + url);

        Call call = makeCall(url);
        call.enqueue(new com.squareup.okhttp.Callback() {
            public void onFailure(Request request, final IOException e) {
                postToHandler(new Runnable() {
                    public void run() {
                        callback.error("Error communicating with the server: " + e.getMessage());
                    }
                });
            }

            public void onResponse(Response response) throws IOException {
                Type t = new TypeToken<Map<String, String>>(){}.getType();
                final Map<String, String> obj;
                final String s;
                try {
                    s = response.body().string();
                    obj = mGson.fromJson(s, t);
                    postToHandler(new Runnable() {
                        public void run() {
                            callback.success(obj);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    postToHandler(new Runnable() {
                        public void run() {
                            callback.error("Unexpected error while fetching submission " +
                                    "data (current=" + currentSubmissionID + ")");
                        }
                    });
                }
            }
        });
        return call;
    }
}