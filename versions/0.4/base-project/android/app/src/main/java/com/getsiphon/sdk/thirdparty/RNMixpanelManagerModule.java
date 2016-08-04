package com.getsiphon.sdk.thirdparty;


import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class RNMixpanelManagerModule extends ReactContextBaseJavaModule {
    private final String TAG = "RNMixpanelManagerModule";
    private MixpanelAPI mMixpanelAPI;

    public RNMixpanelManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNMixpanelManager";
    }

    @ReactMethod
    public void sharedInstanceWithToken(String token) {
        mMixpanelAPI = MixpanelAPI.getInstance(getReactApplicationContext(), token);
        Log.d(TAG, "sharedInstanceWithToken(): instantiated " + mMixpanelAPI.toString());
    }

    @ReactMethod
    public void registerSuperProperties(ReadableMap superProperties) {
        JSONObject props = new JSONObject(toHashMap(superProperties));
        Log.d(TAG, "registerSuperProperties(): " + props);
        mMixpanelAPI.registerSuperProperties(props);
    }

    @ReactMethod
    public void track(String event) {
        Log.d(TAG, "track(): " + event);
        mMixpanelAPI.track(event);
    }

    @ReactMethod
    public void identify(String distinctID) {
        Log.d(TAG, "identify(): " + distinctID);
        mMixpanelAPI.identify(distinctID);
    }

    @ReactMethod
    public void createAlias(String alias, String distinctID) {
        Log.d(TAG, "createAlias(): " + alias + " --> " + distinctID);
        mMixpanelAPI.alias(alias, distinctID);
    }

    @ReactMethod
    public void trackWithProperties(String event, ReadableMap properties) {
        HashMap<String, Object> map = toHashMap(properties);
        Log.d(TAG, "trackWithProperties(): " + map);
        mMixpanelAPI.trackMap(event, map);
    }

    /**
     * Stolen from: https://github.com/facebook/react-native/pull/4658
     */
    private static ArrayList<Object> toArrayList(ReadableArray readableArray) {
        ArrayList<Object> arrayList = new ArrayList<>();

        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    arrayList.add(null);
                    break;
                case Boolean:
                    arrayList.add(readableArray.getBoolean(i));
                    break;
                case Number:
                    arrayList.add(readableArray.getDouble(i));
                    break;
                case String:
                    arrayList.add(readableArray.getString(i));
                    break;
                case Map:
                    arrayList.add(toHashMap(readableArray.getMap(i)));
                    break;
                case Array:
                    arrayList.add(toArrayList(readableArray.getArray(i)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object at index: " +
                            i + ".");
            }
        }
        return arrayList;
    }

    /**
     * Stolen from: https://github.com/facebook/react-native/pull/4658
     */
    private static HashMap<String, Object> toHashMap(ReadableMap readableMap) {
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        HashMap<String, Object> hashMap = new HashMap<>();

        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    hashMap.put(key, null);
                    break;
                case Boolean:
                    hashMap.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    hashMap.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    hashMap.put(key, readableMap.getString(key));
                    break;
                case Map:
                    hashMap.put(key, toHashMap(readableMap.getMap(key)));
                    break;
                case Array:
                    hashMap.put(key, toArrayList(readableMap.getArray(key)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " +
                            key + ".");
            }
        }
        return hashMap;
    }
}
