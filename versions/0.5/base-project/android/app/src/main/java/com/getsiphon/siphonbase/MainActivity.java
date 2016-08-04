package com.getsiphon.siphonbase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.getsiphon.sdk.SiphonAppActivity;

import java.util.Properties;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: this needs to be configurable from our automatic builds somehow
        Properties props = new Properties(System.getProperties());
        props.setProperty("siphon.scheme", "https");
        props.setProperty("siphon.host", "getsiphon.com");
        props.setProperty("siphon.base-version", "0.5");
        System.setProperties(props);


        Intent intent = new Intent(this, SiphonAppActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("scheme", System.getProperty("siphon.scheme"));
        intent.putExtra("host", System.getProperty("siphon.host"));
        intent.putExtra("baseVersion", System.getProperty("siphon.base-version"));
        intent.putExtra("appID", "bvycwIGjzX"); // Siphon Sandbox
        intent.putExtra("submissionID", "lGoWlITfks");
        startActivity(intent);
    }
}
