package com.getsiphon.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.facebook.react.bridge.NativeModuleCallExceptionHandler;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.getsiphon.sdk.bundle.SiphonBundleManager;
import com.getsiphon.sdk.fragments.SiphonAppFragment;
import com.getsiphon.sdk.fragments.SiphonErrorFragment;
import com.getsiphon.sdk.fragments.SiphonLoadingFragment;
import com.getsiphon.sdk.models.SiphonConfig;
import com.getsiphon.sdk.network.SiphonAPIClient;
import com.getsiphon.sdk.network.SiphonLogStreamClient;
import com.getsiphon.sdk.network.SiphonNotificationStreamClient;
import com.getsiphon.siphonbase.R;


public class SiphonDevelopmentAppActivity extends Activity implements
        SiphonBundleManager.Delegate, SiphonLogStreamClient.Delegate,
        SiphonNotificationStreamClient.Delegate, DefaultHardwareBackBtnHandler, SPLogListener {

    private final String TAG = "DevelopmentAppActivity";
    private Handler mHandler;
    private String bundlerURL = null;

    // We switch between these two fragments
    private SiphonAppFragment mSiphonAppFragment;
    private SiphonLoadingFragment mSiphonLoadingFragment;
    private SiphonErrorFragment mSiphonErrorFragment;
    private boolean showingAppFragment = false; // true when app is showing

    // Siphon SDK
    private SiphonConfig mSiphonConfig;
    private SiphonAPIClient mSiphonAPIClient;
    private SiphonBundleManager mSiphonBundleManager;
    private SiphonLogStreamClient mSiphonLogStreamClient;
    private SiphonNotificationStreamClient mSiphonNotificationStreamClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(getMainLooper());

        // Instantiate our shared config object.
        Intent intent = getIntent();
        mSiphonConfig = new SiphonConfig(
                intent.getStringExtra("appID"), intent.getStringExtra("authToken"),
                intent.getStringExtra("scheme"), intent.getStringExtra("host"),
                intent.getStringExtra("baseVersion"),
                intent.getBooleanExtra("sandboxMode", true),
                intent.getBooleanExtra("devMode", false));

        // We need a container view in which to display our fragments.
        LinearLayout container = new LinearLayout(this);
        container.setId(R.id.fragment_container);
        setContentView(container);
        showLoadingFragment(); // do this early on because delegate callbacks might need it.

        mSiphonAPIClient = new SiphonAPIClient(this, mSiphonConfig);
        mSiphonLogStreamClient = new SiphonLogStreamClient(this, mSiphonConfig, this);
        mSiphonNotificationStreamClient = new SiphonNotificationStreamClient(this,
                mSiphonConfig, this);

        // We'll connect immediately, just as iOS does.
        mSiphonLogStreamClient.connect();
        mSiphonNotificationStreamClient.connect();

        mSiphonBundleManager = new SiphonBundleManager(this, mSiphonConfig, this);
        mSiphonBundleManager.initialize();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }
    }

    private void loadApp() {
        if (bundlerURL == null) {
            mSiphonLoadingFragment.setProgress(SiphonLoadingFragment.Progress.BUNDLE_INITIALIZED);
            mSiphonAPIClient.fetchBundlerURL(new SiphonAPIClient.StringCallback() {
                public void success(String result) {
                    Log.i(TAG, "Loading bundle from remote URL.");
                    bundlerURL = result; // cache the result
                    mSiphonLoadingFragment.setProgress(
                            SiphonLoadingFragment.Progress.BUNDLER_URL_FETCHED);
                    mSiphonBundleManager.loadBundleFromURL(bundlerURL);
                }

                public void error(String message) {
                    Log.e(TAG, "Problem fetching bundler URL: " + message);
                    showErrorMessage(message);
                    hideExitButton();
                }
            });
        } else {
            Log.i(TAG, "Loading bundle from cached URL.");
            mSiphonLoadingFragment.setProgress(
                    SiphonLoadingFragment.Progress.BUNDLER_URL_FETCHED);
            mSiphonBundleManager.loadBundleFromURL(bundlerURL);
        }
    }

    private void reloadApp() {
        Log.d(TAG, "reloadApp()");
        hideExitButton(); // TODO: remove this?

        // Remove the app so that it gets destroyed.
        getFragmentManager().beginTransaction().remove(mSiphonAppFragment).commit();
        mSiphonAppFragment = null;

        // Show the loading fragment and reload the app in the next loop.
        new Handler(getMainLooper()).post(new Runnable() {
            public void run() {
                showLoadingFragment();
                mSiphonLoadingFragment.setProgress(
                        SiphonLoadingFragment.Progress.BUNDLE_INITIALIZED);
                loadApp();
            }
        });
    }

    private void showAppFragment() {
        Log.i(TAG, "showAppFragment()");
        if (mSiphonAppFragment == null) {
            mSiphonAppFragment = new SiphonAppFragment();
            mSiphonAppFragment.setBackButtonHandler(this);
            mSiphonAppFragment.setLogListener(this);
            mSiphonAppFragment.setExceptionHandler(new NativeModuleCallExceptionHandler() {
                public void handleException(Exception e) {
                    String stack = e.getMessage();
                    showErrorFragment(stack);
                }
            });
            mSiphonAppFragment.setContext(this);
            mSiphonAppFragment.setApplication(getApplication());
            mSiphonAppFragment.setBundleFile(mSiphonBundleManager.getBundlePath());
        }
        mSiphonAppFragment.cleanup();
        showFragment(mSiphonAppFragment);
        showingAppFragment = true;
    }

    private void showLoadingFragment() {
        if (mSiphonLoadingFragment == null) {
            mSiphonLoadingFragment = new SiphonLoadingFragment();
            mSiphonLoadingFragment.setOnTryAgainListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "'Try again' was clicked in loading fragment");
                    mSiphonLoadingFragment.reset();
                    loadApp();
                }
            });
        } else {
            mSiphonLoadingFragment.reset();
        }
        showFragment(mSiphonLoadingFragment);
        showingAppFragment = false;
    }

    private void showErrorFragment(String errorMessage) {
        Log.d(TAG, "showErrorFragment()");
        if (mSiphonErrorFragment == null) {
            mSiphonErrorFragment = new SiphonErrorFragment();
            mSiphonErrorFragment.setOnTryAgainListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "'Try again' was clicked in error fragment");
                    reloadApp();
                }
            });
        }
        mSiphonErrorFragment.setErrorText(errorMessage);
        showFragment(mSiphonErrorFragment);
        showingAppFragment = false;
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        //transaction.addToBackStack(null); // TODO: need this? see how back button performs
        transaction.commit();
    }

    private void showExitButton() {
        //if (!mSiphonConfig.SANDBOX_MODE) return; // exit button is only shown in the sandbox
        // TODO
    }

    private void hideExitButton() {
        //if (!mSiphonConfig.SANDBOX_MODE) return; // exit button is only shown in the sandbox
        // TODO
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, "ERROR showErrorMessage(): " + message);
        mSiphonLoadingFragment.setErrorText(message);
        hideExitButton();
    }

    // --- Lifecycle methods ---

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() is disconnecting websockets");
        mSiphonLogStreamClient.close();
        mSiphonNotificationStreamClient.close();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart() is re-connecting websockets");
        if (!mSiphonLogStreamClient.isReady()) mSiphonLogStreamClient.connect();
        if (!mSiphonNotificationStreamClient.isReady()) mSiphonNotificationStreamClient.connect();
    }

    @Override
    public void onBackPressed() {
        // We pass this through to the app fragment, if it's currently showing.
        if (showingAppFragment) {
            mSiphonAppFragment.backPressed();
        } else {
            super.onBackPressed();
        }
    }

    // --- SiphonBundleManagerDelegate.Delegate ---

    @Override
    public void onInitializationError(String message) {
        showErrorMessage(message);
    }

    @Override
    public void onInitialized() {
        loadApp();
    }

    @Override
    public void onBundleLoaded() {
        mSiphonLoadingFragment.setProgress(SiphonLoadingFragment.Progress.BUNDLE_LOADED);
        showAppFragment();
        showExitButton();
    }

    @Override
    public void onBundleFailedToLoad(String message) {
        showErrorMessage(message);
        hideExitButton();
    }

    @Override
    public void onFetchingAssets() {
        mSiphonLoadingFragment.setProgress(SiphonLoadingFragment.Progress.FETCHING_ASSETS);
    }

    @Override
    public void onFetchedAssets() {
        mSiphonLoadingFragment.setProgress(SiphonLoadingFragment.Progress.FETCHED_ASSETS);
    }

    // --- SiphonNotificationStreamClient.Delegate ---

    @Override
    public void onNotificationReceived(String notificationType) {
        Log.i(TAG, "onNotificationReceived(): " + notificationType);
        if (notificationType.equals("app_updated")) {
            reloadApp();
        }
    }

    @Override
    public void onNotificationStreamError(String message) {
        Log.i(TAG, "onNotificationStreamError(): " + message);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mSiphonNotificationStreamClient.connect(); // it knows how to reconnect.
            }
        }, 5000);
    }

    // --- SiphonLogStreamClient.Delegate ---

    @Override
    public void onLogStreamError(String message) {
        Log.i(TAG, "onLogStreamError(): " + message);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mSiphonLogStreamClient.connect(); // it knows how to reconnect.
            }
        }, 5000);
    }

    // -- DefaultHardwareBackBtnHandler --

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    // -- SPLogListener --

    @Override
    public void onMessage(String s) {
        mSiphonLogStreamClient.broadcastLog(s);
    }
}
