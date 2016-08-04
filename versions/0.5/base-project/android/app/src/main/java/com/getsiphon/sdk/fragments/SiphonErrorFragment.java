package com.getsiphon.sdk.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.getsiphon.siphonbase.R;

public class SiphonErrorFragment extends Fragment {
    private final String TAG = "SiphonErrorFragment";
    private TextView mTextView;
    private Button mButton;
    private String currentErrorText = "";
    private View.OnClickListener onTryAgainListener = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_screen, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTextView = (TextView) view.findViewById(R.id.error_text);
        mTextView.setText(currentErrorText);

        mButton = (Button) view.findViewById(R.id.try_again_button);
        if (onTryAgainListener != null) {
            mButton.setOnClickListener(onTryAgainListener);
        }
    }

    public void setOnTryAgainListener(View.OnClickListener listener) {
        onTryAgainListener = listener;
        if (mButton != null) mButton.setOnClickListener(listener);
    }

    public void setErrorText(String s) {
        Log.d(TAG, "setErrorText(): " + s);
        currentErrorText = s;
        if (mTextView != null) mTextView.setText(s);
    }
}
