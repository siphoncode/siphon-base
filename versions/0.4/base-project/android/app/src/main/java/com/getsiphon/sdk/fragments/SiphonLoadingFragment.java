package com.getsiphon.sdk.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getsiphon.siphonbase.R;

public class SiphonLoadingFragment extends Fragment {
    private final String TAG = "SiphonLoadingFragment";
    private ProgressBar mProgressBar;
    private Button mButton;
    private TextView mTextView;
    private String currentErrorText = "";
    private View.OnClickListener onTryAgainListener = null;

    public enum Progress {
        BUNDLE_INITIALIZED (10),
        BUNDLER_URL_FETCHED (20),
        FETCHING_ASSETS (40),
        FETCHED_ASSETS (60),
        BUNDLE_LOADED (80);

        private final int progress;

        Progress(int progress) {
            this.progress = progress;
        }

        public int getProgress() {
            return this.progress;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.loading_screen, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mButton = (Button) view.findViewById(R.id.try_again_button);
        mButton.setVisibility(View.INVISIBLE);
        if (onTryAgainListener != null) {
            mButton.setOnClickListener(onTryAgainListener);
        }

        mTextView = (TextView) view.findViewById(R.id.error_text);
        mTextView.setText(currentErrorText);

        mProgressBar = (ProgressBar) view.findViewById(R.id.loading_bar);
        mProgressBar.getProgressDrawable().setColorFilter(Color.parseColor("#2567ce"),
                PorterDuff.Mode.SRC_IN);
        mProgressBar.setMax(100);
        mProgressBar.setProgress(0);
    }

    public void setProgress(SiphonLoadingFragment.Progress progress) {
        Log.i(TAG, "setProgress: " + progress + " == " + progress.getProgress());
        mProgressBar.setProgress(progress.getProgress());
    }

    public void setOnTryAgainListener(View.OnClickListener listener) {
        onTryAgainListener = listener;
        if (mButton != null) mButton.setOnClickListener(listener);
    }

    public void setErrorText(String s) {
        currentErrorText = s;
        if (mTextView != null) {
            mTextView.setText(s);
        }
        if (mButton != null) {
            mButton.setVisibility(s.equals("") ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public void reset() {
        setErrorText(""); // hides the button too
    }
}
