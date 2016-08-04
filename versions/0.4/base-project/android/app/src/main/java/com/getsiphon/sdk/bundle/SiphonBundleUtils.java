package com.getsiphon.sdk.bundle;

import android.util.Log;

import com.getsiphon.sdk.models.SiphonHash;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SiphonBundleUtils {
    private static final String TAG = "SiphonBundleUtils";

    // Credit: http://stackoverflow.com/a/10997886
    public static void unzip(byte[] zipData, File outputDirectory) throws SiphonBundleException {
        Log.d(TAG, "unzip(): got " + zipData.length + " bytes.");
        ZipInputStream zis;
        try {
            zis = new ZipInputStream(new ByteArrayInputStream(zipData));
            ZipEntry ze;
            String filename;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.d(TAG, "unzip(): entry: " + filename);
                // If it's a directory, create it.
                if (ze.isDirectory()) {
                    File d = FileUtils.getFile(outputDirectory, filename);
                    if (!d.mkdirs()) {
                        throw new SiphonBundleException("Failed to make temporary assets " +
                                "directory: " + filename);
                    }
                    continue;
                }

                // If we got this far it's a file, so we need to write it out. First, let's make
                // sure the containing directory exists.
                File f = FileUtils.getFile(outputDirectory, filename);
                String base = FilenameUtils.getFullPath(f.getAbsolutePath());
                FileUtils.forceMkdir(new File(base));

                // Then write out the file.
                FileOutputStream fout = new FileOutputStream(f);
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                fout.close();
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Failed to unzip assets file: " + e.getMessage());
        }
    }

    public static void replaceFooter(File oldFooter, File newFooter) throws SiphonBundleException {
        try {
            // Load in the new footer as a string.
            String content = FileUtils.readFileToString(newFooter, "UTF-8");

            // Process the asset URL placeholder ("__SIPHON_ASSET_URL/images/logo.png" becomes:
            // "file://" + __GET_SIPHON_ASSET_DIR() + "/images/logo.png")
            content = content.replace("__SIPHON_ASSET_URL",
                    "file://\" + __GET_SIPHON_ASSET_DIR() + \"");

            // Write it to our app directory.
            FileUtils.writeStringToFile(oldFooter, content, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Failed to process the new footer: " + e.getMessage());
        }
    }

    public static Map<String, File> getFlatListing(File rootDirectory)
            throws SiphonBundleException {

        // Make sure the root directory exists.
        if (!rootDirectory.isDirectory()) {
            throw new SiphonBundleException("Could not find the assets directory at: " +
                    rootDirectory.getAbsolutePath());
        }

        // Recursively fetches all files/directories in our root directory.
        Collection<File> files = FileUtils.listFilesAndDirs(rootDirectory, TrueFileFilter.INSTANCE,
                TrueFileFilter.INSTANCE);

        // Convert the File objects to flat strings. Also, the listFilesAndDirs() helper returns
        // the root directory itself, so we need to filter it out.
        HashMap<String, File> listing = new HashMap<>();
        for (File f : files) {
            if (!f.isDirectory()) {
                // Get the path to this file relative to the root directory. We don't want
                // an absolute path.
                String relative = rootDirectory.toURI().relativize(f.toURI()).getPath();
                listing.put(relative, f);
            }
        }
        return listing;
    }

    private static void removeExpiredAssets(File storedAssetsDirectory, List<String> assetListing)
            throws SiphonBundleException {
        Map<String, File> listing = getFlatListing(storedAssetsDirectory);
        for (Map.Entry<String, File> entry : listing.entrySet()) {
            String name = entry.getKey();
            File f = entry.getValue();
            if (!assetListing.contains(name)) {
                Log.d(TAG, "removeExpiredAssets(): deleting: " + name);
                if (!f.delete()) {
                    throw new SiphonBundleException("Failed to delete an expired asset: " +
                            f.getName());
                }
            }
        }
    }

    private static void copyNewChangedAssets(File storedAssetsDirectory, File newAssetsDirectory)
            throws SiphonBundleException {
        // Make sure the stored assets directory exists.
        if (!storedAssetsDirectory.isDirectory()) {
            throw new SiphonBundleException("Could not find the stored assets directory at: " +
                    storedAssetsDirectory.toString());
        }

        Map<String, File> listing = getFlatListing(newAssetsDirectory);
        for (Map.Entry<String, File> entry : listing.entrySet()) {
            String name = entry.getKey();
            File copyFrom = entry.getValue();
            Log.d(TAG, "copyNewChangedAssets(): found: " + copyFrom.getPath());

            // Make the corresponding relative path in our stored assets directory.
            File copyTo = FileUtils.getFile(storedAssetsDirectory, name);

            try {
                // Make sure the base directory exists in our stored assets directory.
                String base = FilenameUtils.getFullPath(copyTo.getAbsolutePath());
                FileUtils.forceMkdir(new File(base));

                // Do the copy.
                Log.d(TAG, "copyNewChangedAssets(): copying " + copyFrom.getName() +
                        " --> " + copyTo.getName());
                FileUtils.copyFile(copyFrom, copyTo);
            } catch (IOException e) {
                e.printStackTrace();
                throw new SiphonBundleException("Failed to copy over a new/changed asset: " +
                        name);
            }
        }
    }

    /**
     * Takes a flat asset listing file and an extracted directory containing the new assets and
     * resolves them with the current assets. It deletes those that are not in the asset list,
     * and moves the new ones over.
     *
     * Equivalent to resolveAssetsWithAssetFile:andAssetDirectory:error: method in the iOS SDK.
     *
     * @param assetListingFile flat file of all current assets, from the /pull archive.
     * @param storedAssetsDirectory directory containing currently stored assets for this app.
     * @param newAssetsDirectory extracted directory in the /pull archive containing our
     *                           new/changed assets.
     */
    public static void resolveAssets(File assetListingFile, File storedAssetsDirectory,
                                     File newAssetsDirectory) throws SiphonBundleException {
        // Extract the listing file contents to an array.
        List<String> assetListing;
        try {
            assetListing = FileUtils.readLines(assetListingFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Failed to open the asset listing file: " +
                    e.getMessage());
        }

        removeExpiredAssets(storedAssetsDirectory, assetListing);
        copyNewChangedAssets(storedAssetsDirectory, newAssetsDirectory);
    }

    // This is a combination of buildBundleWithError: and buildBundleFromHeader:andFooter:error:
    // in the iOS SDK.
    public static void buildBundle(String appID, File storedAssetsDirectory,
                                   String preHeaderContent, String headerContent, File footerFile,
                                   File destination) throws SiphonBundleException {
        // Process the pre-header.
        preHeaderContent = preHeaderContent.replace("__SIPHON_APP_ID", appID);
        preHeaderContent = preHeaderContent.replace("__SIPHON_ASSET_URL",
                storedAssetsDirectory.toString());

        // Concatenate all the things.
        try {
            FileUtils.writeStringToFile(destination, preHeaderContent, "UTF-8", false);
            FileUtils.writeStringToFile(destination, headerContent, "UTF-8", true);

            String footerContent = FileUtils.readFileToString(footerFile, "UTF-8");
            FileUtils.writeStringToFile(destination, footerContent, "UTF-8", true);
        } catch (IOException e) {
            e.printStackTrace();
            FileUtils.deleteQuietly(destination); // clean up
            throw new SiphonBundleException("Failed to concatenate bundle: " + e.getMessage());
        }
    }

    private static String sha256(String s) throws SiphonBundleException {
        MessageDigest digest;
        byte[] bytes;
        try {
            digest = MessageDigest.getInstance("SHA256");
            bytes = digest.digest(s.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new SiphonBundleException("Failed to find SHA-256: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new SiphonBundleException("UTF-8 not found: " + e.getMessage());
        }

        // Convert to hex representation.
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static Collection<SiphonHash> generateHashesForAssets(File storedAssetsDirectory)
            throws SiphonBundleException {
        ArrayList<SiphonHash> hashes = new ArrayList<>();
        if (!storedAssetsDirectory.isDirectory()) {
            return hashes; // return it empty
        }
        // Generate a SHA-256 hash for every asset file.
        Map<String, File> listing = getFlatListing(storedAssetsDirectory);
        for (Map.Entry<String, File> entry : listing.entrySet()) {
            String name = entry.getKey();
            File f = entry.getValue();
            String sha;
            try {
                sha = sha256(FileUtils.readFileToString(f, "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new SiphonBundleException("Failed to open an asset for hashing: " +
                        e.getMessage());
            }
            hashes.add(new SiphonHash(name, sha));
        }
        return hashes;
    }
}
