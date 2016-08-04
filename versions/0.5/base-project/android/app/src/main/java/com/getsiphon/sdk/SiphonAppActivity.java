package com.getsiphon.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

//import com.facebook.FacebookSdk;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.getsiphon.sdk.bundle.SiphonBundleManager;
import com.getsiphon.sdk.fragments.SiphonAppFragment;
import com.getsiphon.sdk.models.SiphonConfig;
import com.getsiphon.sdk.network.SiphonAPIClient;
import com.getsiphon.siphonbase.R;

import java.io.IOException;
import java.util.Map;

public class SiphonAppActivity extends Activity implements SiphonBundleManager.Delegate,
        DefaultHardwareBackBtnHandler {
    private final String TAG = "SiphonAppActivity";
    private Handler mHandler;
    private SiphonBundleManager mSiphonBundleManager;
    private SiphonConfig mSiphonConfig;
    private SiphonAppFragment mSiphonAppFragment;
    private SiphonUpdatingDialog mSiphonUpdatingDialog;
    private boolean showingAppFragment = false; // true when app is showing
    private boolean shouldLoadLocal;
    private String newSubmissionID = null; // only used for bundle delegate callbacks

    // Constants for SharedPreferences
    private final String PREFS_BASE_VERSION = "baseVersion";
    private final String PREFS_SUBMISSION_ID = "submissionID";
    private final String PREFS_LAST_UPDATE = "lastUpdate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(getMainLooper());
        if (!instantiateConfig()) return;

        // Initialize the SDK before executing any other operations,
        //FacebookSdk.sdkInitialize(getApplicationContext());
        // TODO: more instructions here: https://developers.facebook.com/docs/android/getting-started/

        // We need a container view in which to display our fragments.
        LinearLayout container = new LinearLayout(this);
        container.setId(R.id.fragment_container);
        setContentView(container);

        // Kick off the bundle manager.
        mSiphonBundleManager = new SiphonBundleManager(this, mSiphonConfig, this);
        mSiphonBundleManager.initialize();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only trigger an update if the app is already loaded, because onResume() is also
        // called on first load.
        if (mSiphonAppFragment != null) {
            Log.d(TAG, "App resumed from foreground, will try to check for an update...");
            updateIfTimeOK();
        }
    }

    private boolean instantiateConfig() {
        Intent intent = getIntent();
        String appID = intent.getStringExtra("appID");
        String bakedBaseVersion = intent.getStringExtra("baseVersion"); // Baked in to the .apk
        String bakedSubmissionID = intent.getStringExtra("submissionID");

        // Sanity check
        if (appID == null || bakedBaseVersion == null || bakedSubmissionID == null) {
            throw new RuntimeException("Expecting 'appID', 'baseVersion', 'submissionID' " +
                    "to be passed in the intent.");
        }

        // Set or update our stored submission ID and base version.
        SharedPreferences preferences = getSharedPreferences(appID, MODE_PRIVATE);
        SharedPreferences.Editor editor  = preferences.edit();
        String lastStoredBaseVersion = preferences.getString(PREFS_BASE_VERSION, "");
        String submissionID;

        // This tells us where we already have a final processed bundle file for this app ID.
        boolean finalBundleIsReady = SiphonBundleManager.hasBundle(this, appID);

        if (!lastStoredBaseVersion.equals(bakedBaseVersion) || !finalBundleIsReady) {
            // The APK has been hard updated, or this is a fresh install, so we update the
            // current base version and Submission ID in SharedPreferences (the Submission ID
            // should reflect the baked one supplied to the intent).
            try {
                SiphonBundleManager.cleanAppDirectory(this, appID);
            } catch (IOException e) {
                e.printStackTrace();
                showErrorMessage("Failed to clean app directory.");
                return false;
            }
            editor.putString(PREFS_BASE_VERSION, bakedBaseVersion);
            editor.putString(PREFS_SUBMISSION_ID, bakedSubmissionID);
            submissionID = bakedSubmissionID;

            // Note: to load local means to load from the assets.zip that's bundled with the app,
            // i.e. unzip it and copy it to the app directory
            shouldLoadLocal = true;
        } else {
            // Otherwise, the Submission ID should be set to the last one stored.
            submissionID = preferences.getString(PREFS_SUBMISSION_ID, null);

            // Note: we're not loading locally, which means we're loading from the already
            // processed footer and assets that have already been stored in the
            // app cache directory.
            shouldLoadLocal = false;
        }

        // Synchronously write these to disk before we do anything else.
        if (!editor.commit()) {
            throw new RuntimeException("Failed to write preferences in instantiateConfig().");
        }

        // Instantiate our shared config object.
        mSiphonConfig = new SiphonConfig(appID, submissionID, intent.getStringExtra("scheme"),
                intent.getStringExtra("host"), intent.getStringExtra("baseVersion"));
        return true;
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

    private void showAppFragment() {
        if (mSiphonAppFragment != null) {
            // Remove the app so that it gets destroyed.
            Log.d(TAG, "App already exists, destroying it first.");
            getFragmentManager().beginTransaction().remove(mSiphonAppFragment).commit();
            mSiphonAppFragment = null;

            // Wait for the next loop until we load the app.
            mHandler.post(new Runnable() {
                public void run() {
                    showAppFragment();
                }
            });
            return;
        }

        Log.i(TAG, "showAppFragment()");
        mSiphonAppFragment = new SiphonAppFragment();
        mSiphonAppFragment.setBackButtonHandler(this);
        mSiphonAppFragment.setContext(this);
        mSiphonAppFragment.setApplication(getApplication());
        mSiphonAppFragment.setBundleFile(mSiphonBundleManager.getBundlePath());
        mSiphonAppFragment.cleanup();
        showFragment(mSiphonAppFragment);
        showingAppFragment = true;
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        //transaction.addToBackStack(null); // TODO: need this? see how back button performs
        transaction.commit();
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, "ERROR showErrorMessage(): " + message);
        new AlertDialog.Builder(this).setTitle("Error").setMessage(message).show();
    }

    private void showUpdatingDialog() {
        if (mSiphonUpdatingDialog != null) {
            Log.d(TAG, "Updating dialog was already showing! Dismissing.");
            mSiphonUpdatingDialog.dismiss();
            mSiphonUpdatingDialog = null;
        }

        mSiphonUpdatingDialog = new SiphonUpdatingDialog(this);

        // Handle the case when the user clicks the 'Retry' button.
        mSiphonUpdatingDialog.setOnRetryListener(new SiphonUpdatingDialog.OnRetryListener() {
            @Override
            public void onRetryUpdate() {
                mHandler.post(new Runnable() {
                    public void run() {
                        // Cancels the fetch assets operation (doesn't matter if it was
                        // already finished).
                        mSiphonBundleManager.cancel();

                        mSiphonUpdatingDialog = null;
                        update();
                    }
                });
            }
        });

        // Handle the case when the user clicks the 'Cancel' button.
        mSiphonUpdatingDialog.setOnCancelListener(new SiphonUpdatingDialog.OnCancelListener() {
            @Override
            public void onCancelUpdate() {
                mHandler.post(new Runnable() {
                    public void run() {
                        // Cancels the fetch assets operation (doesn't matter if it was
                        // already finished).
                        mSiphonBundleManager.cancel();
                    }
                });
            }
        });

        mSiphonUpdatingDialog.show();
    }

    private void updateIfTimeOK() {
        Log.d(TAG, "updateIfTimeOK() called");
        SharedPreferences preferences = getSharedPreferences(mSiphonConfig.APP_ID, MODE_PRIVATE);
        long lastChecked = preferences.getLong(PREFS_LAST_UPDATE, -1); // Unix time
        long now = System.currentTimeMillis() / 1000;

        long t = now - lastChecked;
        if (t >= (4 * 60 * 60)) {
            Log.d(TAG, "Checking for an update because " + t + "s has elapsed since last check.");
            update();
        } else {
            Log.d(TAG, "Not checking for update because only " + t + "s has elapsed since " +
                    "last check.");
        }
    }

    private void handleAPIError(String message) {
        // For now we just ignore these errors but log them to console.
        Log.e(TAG, "handleAPIError(): " + message);
        if (mSiphonUpdatingDialog != null && mSiphonUpdatingDialog.isShowing()) {
            mSiphonUpdatingDialog.dismiss();
            mSiphonUpdatingDialog = null;
            mSiphonBundleManager.cancel();
        }
    }

    private void handleAPIResponse(Map<String, String> obj) {
        Log.d(TAG, "handleAPIResponse(): obj=" + obj);

        String submissionID = obj.get("submission_id");
        if (submissionID == null) {
            handleAPIError("Missing submission ID, obj=" + obj);
            return;
        }

        String bundlerURL = obj.get("bundler_url");
        if (bundlerURL != null) {
            Log.d(TAG, "handleAPIResponse(): there is a 'bundler_url', updating...");
            showUpdatingDialog();
            newSubmissionID = submissionID; // bundle delegate callback will handle this
            mSiphonBundleManager.loadBundleFromURL(bundlerURL);
        } else {
            Log.d(TAG, "handleAPIResponse(): no 'bundler_url', not updating.");
        }
    }

    private void update() {
        // Bump the last checked timestamp.
        SharedPreferences preferences = getSharedPreferences(mSiphonConfig.APP_ID, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREFS_LAST_UPDATE, System.currentTimeMillis() / 1000);
        editor.apply(); // writes in the background

        // Do a /pull for the current Submission ID and deal with the various cases.
        String currentSubmissionID = preferences.getString(PREFS_SUBMISSION_ID, null);
        SiphonAPIClient client = new SiphonAPIClient(this, mSiphonConfig);
        client.fetchDataForSubmission(currentSubmissionID, new SiphonAPIClient.MapCallback() {
            @Override
            public void success(Map<String, String> obj) {
                String message = obj.get("message");
                if (message != null) {
                    handleAPIError(message);
                } else {
                    handleAPIResponse(obj);
                }
            }

            @Override
            public void error(String message) {
                handleAPIError(message);
            }
        });
    }

    // -- SiphonBundleManager.Delegate --

    @Override
    public void onInitializationError(String message) {
        showErrorMessage(message);
    }

    @Override
    public void onInitialized() {
        if (shouldLoadLocal) {
            // Loading from assets.zip because it's a fresh install or hard update.
            Log.d(TAG, "Loading local bundle.");
            mSiphonBundleManager.loadLocalBundle();
        } else {
            // Final bundle final should already be ready to go.
            Log.d(TAG, "Loading from app directory.");
            mSiphonBundleManager.loadBundle();
        }
    }

    @Override
    public void onBundleLoaded() {
        Log.d(TAG, "onBundleLoaded()");

        // If we're syncing a new Submission ID, we were successful, so write the new ID.
        if (newSubmissionID != null) {
            Log.d(TAG, "Setting new stored Submission ID to: " + newSubmissionID);
            SharedPreferences preferences =
                    getSharedPreferences(mSiphonConfig.APP_ID, MODE_PRIVATE);
            SharedPreferences.Editor editor  = preferences.edit();
            editor.putString(PREFS_SUBMISSION_ID, newSubmissionID);
            editor.apply();
            newSubmissionID = null;
        }

        // Hide the update dialog if it's showing.
        if (mSiphonUpdatingDialog != null) {
            Log.d(TAG, "Hiding the updating dialog.");
            mSiphonUpdatingDialog.dismiss();
            mSiphonUpdatingDialog = null;
        }

        // Show the app.
        shouldLoadLocal = false; // don't try again to load from assets.zip, it's processed now.
        showAppFragment();
        updateIfTimeOK();
    }

    @Override
    public void onBundleFailedToLoad(String message) {
        Log.d(TAG, "onBundleFailedToLoad(): " + message);
        newSubmissionID = null; // soft update was not successful.

        if (mSiphonUpdatingDialog != null) {
            // This error occurred during a soft update.
            Log.d(TAG, "Error during update; showing buttons in the updating dialog.");
            mSiphonUpdatingDialog.showButtons("A problem occurred while updating the app.", true);
        } else {
            showErrorMessage(message);
        }
    }

    @Override
    public void onFetchingAssets() {
        Log.d(TAG, "onFetchingAssets()");
        if (mSiphonUpdatingDialog != null) {
            // The point at which we start fetching assets is the bit that could take ages
            // on a slow connection, so fire off a timer after 6-secs which gives the
            // user the ability to cancel.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mSiphonUpdatingDialog != null &&  mSiphonUpdatingDialog.isShowing()) {
                        mSiphonUpdatingDialog.showButtons("Sorry, it's taking a while...", false);
                    }
                }
            }, 6000);

            mSiphonUpdatingDialog.setProgress(33);
        }
    }

    @Override
    public void onFetchedAssets() {
        Log.d(TAG, "onFetchedAssets()");
        if (mSiphonUpdatingDialog != null) {
            mSiphonUpdatingDialog.setProgress(66);
            mSiphonUpdatingDialog.setTitle("Installing...");
        }
    }

    // -- DefaultHardwareBackBtnHandler --

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }
}
