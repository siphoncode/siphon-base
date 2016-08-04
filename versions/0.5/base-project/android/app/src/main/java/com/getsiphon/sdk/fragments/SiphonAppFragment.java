package com.getsiphon.sdk.fragments;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//import com.facebook.CallbackManager;
import com.facebook.react.LifecycleState;
import com.facebook.react.bridge.NativeModuleCallExceptionHandler;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.shell.MainReactPackage;

import com.brentvatne.react.ReactVideoPackage;
import com.getsiphon.sdk.SPLogListener;
import com.getsiphon.sdk.SPAppManagerPackage;
import com.getsiphon.sdk.SPLogPackage;
import com.getsiphon.sdk.thirdparty.RNMixpanelPackage;
import com.lwansbrough.RCTCamera.RCTCameraPackage;
import com.learnium.RNDeviceInfo.RNDeviceInfo;
import com.rnfs.RNFSPackage;
import com.BV.LinearGradient.LinearGradientPackage;
import com.github.xinthink.rnmk.ReactMaterialKitPackage;
import com.AirMaps.AirPackage;
import com.rt2zz.reactnativecontacts.ReactNativeContacts;
//import com.facebook.reactnative.androidsdk.FBSDKPackage;

public class SiphonAppFragment extends Fragment {
    private final String TAG = "SiphonAppFragment";
    private ReactInstanceManager mReactInstanceManager;
    private ReactRootView mReactRootView = null;
    private DefaultHardwareBackBtnHandler mDefaultHardwareBackBtnHandler = null;
    private Application mApplication = null;
    private SPLogListener mLogListener = null;
    private NativeModuleCallExceptionHandler mNativeModuleCallExceptionHandler = null;
    private Context mContext = null;
    private String bundleFile = null;
//    CallbackManager mCallbackManager; // TODO

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mReactRootView = new ReactRootView(mContext);
        return mReactRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (bundleFile != null) {
            startReactApplication();
        }
    }

    public void cleanup() {
        // TODO: see SPAppView.cleanup()
    }

    /**
     * @param bundleFile can be assets://<name> or /path/to/bundle
     */
    public void setBundleFile(String bundleFile) {
        if (this.bundleFile != null) {
            // TODO: really we should be able to handle this situation -- it needs to cleanup
            // TODO: and create a new ReactInstanceManager (?) if bundleFile was already set
            throw new RuntimeException("setBundleFile() was already called.");
        }
        this.bundleFile = bundleFile;
    }

    private void startReactApplication() {
        Log.i(TAG, "startReactApplication(): " + bundleFile);
//        mCallbackManager = CallbackManager.Factory.create(); // TODO
        mReactInstanceManager = ReactInstanceManager.builder()
                .setApplication(mApplication)
                .setJSBundleFile(bundleFile)
                .addPackage(new MainReactPackage())
                .addPackage(new ReactVideoPackage()) // react-native-video
                .addPackage(new RCTCameraPackage())
                .addPackage(new RNDeviceInfo())
                .addPackage(new RNFSPackage())
                .addPackage(new LinearGradientPackage())
                .addPackage(new ReactMaterialKitPackage())
                .addPackage(new RNMixpanelPackage())
                .addPackage(new AirPackage()) // react-native-maps
                .addPackage(new ReactNativeContacts())
                //.addPackage(new FBSDKPackage(mCallbackManager))
                .addPackage(new SPAppManagerPackage())
                .addPackage(new SPLogPackage(mLogListener))
                .setUseDeveloperSupport(true) // so that exceptions trigger red box
                .setNativeModuleCallExceptionHandler(mNativeModuleCallExceptionHandler)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        mReactRootView.startReactApplication(mReactInstanceManager, "App", null);

    }

    public void setBackButtonHandler(DefaultHardwareBackBtnHandler handler) {
        mDefaultHardwareBackBtnHandler = handler;
    }

    public void setApplication(Application application) {
        mApplication = application;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setLogListener(SPLogListener logListener) {
        mLogListener = logListener;
    }

    public void setExceptionHandler(NativeModuleCallExceptionHandler handler) {
        mNativeModuleCallExceptionHandler = handler;
    }

    // TODO
//    // Required for react-native-fbsdk
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        mCallbackManager.onActivityResult(requestCode, resultCode, data);
//    }

    /**
     * Back button presses are passed through by the host activity.
     */
    public void backPressed() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (mDefaultHardwareBackBtnHandler== null) {
            throw new RuntimeException("Must call setBackButtonHandler() on SiphonAppFragment.");
        }
        super.onResume();
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostResume(getActivity(), mDefaultHardwareBackBtnHandler);
        }
    }
}
