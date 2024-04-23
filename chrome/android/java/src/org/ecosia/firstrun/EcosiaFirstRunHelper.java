package org.ecosia.firstrun;

import android.content.Context;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import org.chromium.base.Log;
import org.chromium.chrome.browser.firstrun.FirstRunStatus;

import org.ecosia.mmp.Singular;
import org.ecosia.referrals.Referrals;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.SharedPreferencesHelpers;


public class EcosiaFirstRunHelper {
    private final Context mContext;
    private static volatile EcosiaFirstRunHelper sInstance;
    private static final String TAG = "org.ecosia.firstrun.EcosiaFirstRunHelper";
    private static final String PREF_FIRST_RUN_DONE = "com.ecosia.PREF_FIRST_RUN_DONE";

    public static EcosiaFirstRunHelper getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (EcosiaFirstRunHelper.class) {
                if (sInstance == null) {
                    sInstance = new EcosiaFirstRunHelper(context);
                }
            }
        }
        return sInstance;
    }

   
    private EcosiaFirstRunHelper(final Context context) {
        mContext = context;
    }

    public void performStepsForFirstRunIfNeeded() {
        if (isFirstRunDone()) {
            // just send Singular session when it's not first run
            Singular.getInstance(mContext).sendSessionInfo();
            return;
        }

        obtainInstallReferrer();
    }

    private boolean isFirstRunDone() {
        // default value needed for migration when onboarding was always shown on startup 
        boolean chromiumFirstRunWasDone = FirstRunStatus.getFirstRunFlowComplete();
        return SharedPreferencesHelpers.getBoolean(mContext, PREF_FIRST_RUN_DONE, chromiumFirstRunWasDone);
    }

    private void markFirstRunAsDone() {
        SharedPreferencesHelpers.putBoolean(mContext, PREF_FIRST_RUN_DONE, true);
    }

    private void obtainInstallReferrer() {
        // InstallReferrer is called once based on first run setting
        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(mContext).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                String installReferrer = null;
                
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            installReferrer = response.getInstallReferrer();
                            Singular.getInstance(mContext).setInstallRef(response);
                            Referrals.getInstance(mContext).setInstallReferrer(installReferrer);
                        } catch (RemoteException e) {
                            Log.e(TAG, "" + e.getMessage());
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        Log.e(TAG, "InstallReferrer client connection feature not supported error");
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        Log.e(TAG, "InstallReferrer service unavailable error");
                        break;
                }
                
                // Make sure to track the install and inform Singular in any case
                onObtainingInstallReferrerFinished(installReferrer);
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                Log.e(TAG, "InstallReferrer service disconnected error");
                // Make sure to track the install in any case
                onObtainingInstallReferrerFinished(null);
            }
        });
    }

    private void onObtainingInstallReferrerFinished(@Nullable String referrer) {
        TrackingManager.getInstance(mContext).trackInstall(referrer);
        Singular.getInstance(mContext).sendSessionInfo();
        markFirstRunAsDone();
    }

}
