package org.ecosia.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.Nullable;

public class BroadcastReceiverHelper {
    private Context mContext;

    public BroadcastReceiverHelper(Context context) {
        mContext = context;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void registerUnexportedReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.registerReceiver(receiver, filter);
        }
    }
}