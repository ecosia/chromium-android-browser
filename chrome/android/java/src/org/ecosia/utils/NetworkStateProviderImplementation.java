package org.ecosia.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

public class NetworkStateProviderImplementation implements NetworkStateProvider {

    private final Context mContext;

    public NetworkStateProviderImplementation(@NonNull final Context context) {
        mContext = context;
    }

    @Override
    public boolean isConnected() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network activeNetwork = connectivityManager.getActiveNetwork();
        final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return hasNeededCapability(networkCapabilities);
    }

    private boolean hasNeededCapability(@NonNull final NetworkCapabilities networkCapabilities) {
        return networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_CELLULAR)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_ETHERNET)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_WIFI)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_LOWPAN)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                || networkCapabilities.hasCapability(NetworkCapabilities.TRANSPORT_VPN);
    }
}
