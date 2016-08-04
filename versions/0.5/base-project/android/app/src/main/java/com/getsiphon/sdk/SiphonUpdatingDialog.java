package com.getsiphon.sdk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

public class SiphonUpdatingDialog extends ProgressDialog {
    private final String TAG = "SiphonUpdatingDialog";
    private OnRetryListener onRetryListener = null;
    private OnCancelListener onCancelListener = null;

    interface OnRetryListener {
        void onRetryUpdate();
    }

    interface OnCancelListener {
        void onCancelUpdate();
    }

    public SiphonUpdatingDialog(Context context) {
        super(context);
    }

    public void setOnRetryListener(OnRetryListener onRetryListener) {
        this.onRetryListener = onRetryListener;
    }

    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }

    public void show() {
        if (onRetryListener == null) {
            throw new RuntimeException("Must call setOnRetryListener() before show() is called.");
        } else if (onCancelListener == null) {
            throw new RuntimeException("Must call setOnCancelListener() before show() is called.");
        }

        setCanceledOnTouchOutside(false);
        setCancelable(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setMax(100);
        setTitle("One moment, we're updating the app...");
        setProgressNumberFormat(null); // hide the "X/100" text

        setButton(DialogInterface.BUTTON_NEUTRAL, "Retry",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Retry was clicked.");
                onRetryListener.onRetryUpdate();
                dismiss();
            }
        });


        setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Cancel was clicked.");
                onCancelListener.onCancelUpdate();
                dismiss();
            }
        });

        super.show();

        // Hide the buttons
        getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.GONE);
        getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
    }

    public void showButtons(String message, boolean hideProgressBar) {
        setTitle(message);
        if (hideProgressBar) {
            setProgressDrawable(null);
            setProgressPercentFormat(null);
        }
        getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
    }
}
