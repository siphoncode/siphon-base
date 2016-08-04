package com.getsiphon.sdk.bundle;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.getsiphon.sdk.models.SiphonConfig;
import com.getsiphon.sdk.models.SiphonHash;
import com.getsiphon.sdk.network.SiphonAPIClient;
import com.squareup.okhttp.Call;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

class SiphonBundleException extends Exception {
    SiphonBundleException(String message) {
        super(message);
    }
}

public class SiphonBundleManager {
    private final String TAG = "SiphonBundleManager";
    private final Handler mHandler;
    private final Context mContext;
    private final SiphonConfig mSiphonConfig;
    private final SiphonAPIClient mSiphonAPIClient;
    private final SiphonBundleManager.Delegate delegate;
    private File bundlePath = null;
    private Call fetchAssetsCall = null;

    public static final String FINAL_BUNDLE_FILE = "bundle"; // final concatenated bundle
    private final String BUNDLE_FOOTER_FILE = "bundle-footer";
    private final String ASSETS_LISTING_FILE = "assets-listing";
    private final String ASSETS_DIRECTORY = "__siphon_assets";
    private final String PRE_HEADER_ASSET_NAME = "pre-header";
    private final String LOCAL_ASSETS_FILE = "assets.zip";

    public interface Delegate {
        void onInitializationError(String message);
        void onInitialized();
        void onBundleLoaded(); // equivalent to bundleDidFinishLoading in iOS
        void onBundleFailedToLoad(String message);
        void onFetchingAssets();
        void onFetchedAssets();
    }

    public SiphonBundleManager(Context context, SiphonConfig config,
                               final SiphonBundleManager.Delegate delegate) {
        this.mContext = context;
        this.mHandler = new Handler(mContext.getMainLooper());
        this.mSiphonConfig = config;
        this.mSiphonAPIClient = new SiphonAPIClient(context, mSiphonConfig);
        this.delegate = delegate;
    }

    public static boolean hasBundle(Context context, String appID) {
        File f = FileUtils.getFile(getAppDirectory(context, appID), FINAL_BUNDLE_FILE);
        return f.exists();
    }

    public static void cleanAppDirectory(Context context, String appID) throws IOException {
        File appDirectory = getAppDirectory(context, appID);
        FileUtils.deleteDirectory(appDirectory);
    }

    public void initialize() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    initializeAppDirectory();
                    // At this point, the bundle manager is loaded successfully.
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onInitialized();
                        }
                    });
                } catch (final SiphonBundleException e) {
                    e.printStackTrace();
                    Log.e(TAG, "onInitializationError: " + e.toString() + ", " + e.getMessage());
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onInitializationError(
                                    "Failed to initialize the bundle manager: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void cancel() {
        Log.d(TAG, "cancel() called");
        if (fetchAssetsCall != null) {
            Log.d(TAG, "cancel(): fetch assets call in progress, cancelling");
            fetchAssetsCall.cancel();
        }
    }

    // Posts a runnable on the UI thread.
    private void postToHandler(Runnable runnable) {
        mHandler.post(runnable);
    }

    // It's best not to cache this (absolute) path because it can change.
    private File getAppDirectory() {
        return SiphonBundleManager.getAppDirectory(mContext, mSiphonConfig.APP_ID);
    }

    public static File getAppDirectory(Context context, String appID) {
        return FileUtils.getFile(context.getFilesDir(), "app_" + appID);
    }

    private File getStoredAssetsDirectory() {
        return FileUtils.getFile(getAppDirectory(), ASSETS_DIRECTORY);
    }

    private void initializeAppDirectory() throws SiphonBundleException {
        // Checks if the app directory exists; if it doesn't, create it.
        File appDirectory = getAppDirectory();
        if (!appDirectory.isDirectory()) {
            try {
                FileUtils.forceMkdir(appDirectory);
            } catch (IOException e) {
                throw new SiphonBundleException("Failed to create app directory at '" +
                        appDirectory.toString() + "', error: " + e.getMessage());
            }
        }

        // Also create the stored assets directory here in case it doesn't already exist.
        try {
            FileUtils.forceMkdir(getStoredAssetsDirectory());
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Could not create the stored assets directory: " +
                    e.getMessage());
        }
    }

    private byte[] readLocalBundleArchive() throws SiphonBundleException {
        InputStream is;
        try {
            is = mContext.getAssets().open(LOCAL_ASSETS_FILE);
        } catch (IOException e) {
            throw new SiphonBundleException("Failed to find local " + LOCAL_ASSETS_FILE);
        }

        byte[] zipData;
        try {
            zipData = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new SiphonBundleException("Failed to read in local " + LOCAL_ASSETS_FILE);
        }
        return zipData;
    }

    public void loadLocalBundle() {
        // This load method should be called if we want to load and process a bundle
        // from assets.zip (which was packaged with this binary).

        // Note: on iOS the bundle-footer is processed in it's equivalent initializeAppDirectory()
        // method, but here we do all local stuff in one place.
        new Thread(new Runnable() {
            public void run() {
                // Attempt to read in our local assets.zip file.
                byte[] zipData;
                try {
                    zipData = readLocalBundleArchive();
                } catch (final SiphonBundleException e) {
                    e.printStackTrace();
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleFailedToLoad(e.getMessage());
                        }
                    });
                    return;
                }

                // This spawns another thread itself.
                processAssets(zipData);
            }
        }).start();
    }

    public void loadBundle() {
        // This load method should be called if we expect that the app directory
        // (i.e. app_<app-id>/) already contains a processed bundle file. Note that on iOS this
        // step is implicit in instantiating the bundle manager, but here we do it explicility.
        new Thread(new Runnable() {
            public void run() {
                // Ensure that the final "bundle" file exists first.
                final File finalBundleFile = getFinalBundleFile();
                if (finalBundleFile.exists()) {
                    // Set the path, so that getBundlePath() will now work for the caller.
                    bundlePath = finalBundleFile;

                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleLoaded();
                        }
                    });
                } else {
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleFailedToLoad("Expected a final bundle file to " +
                                    "exist, not found at: " + finalBundleFile.getAbsolutePath());
                        }
                    });
                }
            }
        }).start();
    }

    public void loadBundleFromURL(final String bundlerURL) {
        new Thread(new Runnable() {
            public void run() {
                // Generate hashes for any currently stored assets that we have.
                File storedAssetsDirectory = getStoredAssetsDirectory();
                Collection<SiphonHash> hashes;
                try {
                    hashes = SiphonBundleUtils.generateHashesForAssets(storedAssetsDirectory);
                } catch (final SiphonBundleException e) {
                    e.printStackTrace();
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleFailedToLoad("Failed to generate hashes: " +
                                    e.getMessage());
                        }
                    });
                    return;
                }

                // Notify that the assets are being fetched
                postToHandler(new Runnable() {
                    public void run() {
                        delegate.onFetchingAssets();
                    }
                });

                fetchAssetsCall = mSiphonAPIClient.fetchAssets(hashes, bundlerURL,
                        new SiphonAPIClient.BytesCallback() {
                    public void success(byte[] result) {
                        fetchAssetsCall = null;
                        // Notify that the assets have been fetched
                        postToHandler(new Runnable() {
                            public void run() {
                                delegate.onFetchedAssets();
                            }
                        });
                        // Process the assets (this spins up another thread for us)
                        processAssets(result);
                    }

                    public void error(final String message) {
                        fetchAssetsCall = null;
                        postToHandler(new Runnable() {
                            public void run() {
                                delegate.onBundleFailedToLoad(message);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void cleanupTemporaryDirectory(File temporaryDir) {
        try {
            FileUtils.deleteDirectory(temporaryDir);
        } catch (IOException e) {
            // Log it and ignore the error.
            Log.e(TAG, "(Ignored) Failed to cleanup temporary dir: " + temporaryDir.toString());
        }
    }

    private String getBundleHeaderName() {
        String name = mSiphonConfig.SANDBOX_MODE ? "sandbox-header" : "header";
        if (mSiphonConfig.DEV_MODE) {
            name = name + "-dev";
        }
        return name;
    }

    private File getFinalBundleFile() {
        return FileUtils.getFile(getAppDirectory(), FINAL_BUNDLE_FILE);
    }

    // Reads in the correct header file based on whether or not we're in sandbox mode.
    private String getBundleAsset(String name) throws SiphonBundleException {
        try {
            InputStream is = mContext.getAssets().open(name);
            return IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Failed to open bundle asset '" + name + "': "
                    + e.getMessage());
        }
    }

    // Long-running, should be called outside of the main thread.
    private void processAssetsInner(byte[] zipData) throws SiphonBundleException {
        File appDirectory = getAppDirectory();
        File storedAssetsDirectory = getStoredAssetsDirectory();

        // Create a temporary directory in the internal app cache. Note that the OS may clean
        // this up without warning if the device is low on space, but we also need to clean it up
        // ourselves.
        File temporaryDir = FileUtils.getFile(mContext.getCacheDir(), UUID.randomUUID().toString());
        if (!temporaryDir.mkdir()) {
            throw new SiphonBundleException("Could not create a temporary directory.");
        }

        try {
            // Unzip the raw bytes into our temporary directory.
            SiphonBundleUtils.unzip(zipData, temporaryDir);

            // Create an assets directory in the temporary directory, in case the .zip file
            // didn't contain one.
            File newAssetsDirectory = FileUtils.getFile(temporaryDir, ASSETS_DIRECTORY);
            if (!newAssetsDirectory.isDirectory()) {
                if (!newAssetsDirectory.mkdir()) {
                    throw new SiphonBundleException("Failed to make a temporary assets " +
                            "directory at: " + newAssetsDirectory.getAbsolutePath());
                }
            }

            // Replace the old footer bundle with the new one
            File oldFooter = FileUtils.getFile(appDirectory, BUNDLE_FOOTER_FILE);
            File newFooter = FileUtils.getFile(temporaryDir, BUNDLE_FOOTER_FILE);
            SiphonBundleUtils.replaceFooter(oldFooter, newFooter);

            // Resolve the assets
            File assetListingFile = FileUtils.getFile(temporaryDir, ASSETS_LISTING_FILE);
            SiphonBundleUtils.resolveAssets(assetListingFile, storedAssetsDirectory,
                    newAssetsDirectory);

            // Concatenates the bundle sections.
            File footerFile = FileUtils.getFile(appDirectory, BUNDLE_FOOTER_FILE);
            File finalBundleFile = getFinalBundleFile();
            String preHeaderContent = getBundleAsset(PRE_HEADER_ASSET_NAME);
            String headerContent = getBundleAsset(getBundleHeaderName());
            SiphonBundleUtils.buildBundle(mSiphonConfig.APP_ID, storedAssetsDirectory,
                    preHeaderContent, headerContent, footerFile, finalBundleFile);

            // If we got this far, set the final bundle path for the benefit of
            // our getBundlePath() method.
            bundlePath = finalBundleFile;
        } finally {
            cleanupTemporaryDirectory(temporaryDir);
        }
    }

    private void processAssets(final byte[] zipData) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    processAssetsInner(zipData);
                    // If we got this far, notify the delegate that the bundle is ready.
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleLoaded();
                        }
                    });
                } catch (final SiphonBundleException e) {
                    // Pass the error to our delegate.
                    e.printStackTrace();
                    postToHandler(new Runnable() {
                        public void run() {
                            delegate.onBundleFailedToLoad(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public String getBundlePath() {
        if (bundlePath == null) {
            throw new RuntimeException("You must call loadBundleFromURL() or loadLocalBundle() " +
                    "before trying to retrieve a bundle path.");
        }
        return bundlePath.getAbsolutePath();
    }
}
