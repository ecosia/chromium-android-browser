// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.settings;

import static org.chromium.chrome.browser.password_manager.PasswordManagerHelper.hasChosenToSyncPasswords;
import static org.chromium.chrome.browser.password_manager.PasswordManagerHelper.usesUnifiedPasswordManagerBranding;
import static org.chromium.chrome.browser.password_manager.PasswordManagerHelper.usesUnifiedPasswordManagerUI;
import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_ACCEPT;
import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_ACTION;
import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_DATA;
import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_DISMISS;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.chromium.base.ContextUtils;
import org.chromium.base.supplier.ObservableSupplier;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.flags.ChromeFeatureList;
import org.chromium.chrome.browser.homepage.HomepageManager;
import org.chromium.chrome.browser.night_mode.NightModeMetrics.ThemeSettingsEntry;
import org.chromium.chrome.browser.night_mode.NightModeUtils;
import org.chromium.chrome.browser.night_mode.settings.ThemeSettingsFragment;
import org.chromium.chrome.browser.password_check.PasswordCheck;
import org.chromium.chrome.browser.password_check.PasswordCheckFactory;
import org.chromium.chrome.browser.password_manager.ManagePasswordsReferrer;
import org.chromium.chrome.browser.password_manager.PasswordManagerLauncher;
import org.chromium.chrome.browser.preferences.Pref;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.search_engines.TemplateUrlServiceFactory;
import org.chromium.chrome.browser.signin.SyncConsentActivityLauncherImpl;
import org.chromium.chrome.browser.signin.services.IdentityServicesProvider;
import org.chromium.chrome.browser.signin.services.SigninManager;
import org.chromium.chrome.browser.sync.SyncService;
import org.chromium.chrome.browser.sync.settings.ManageSyncSettings;
import org.chromium.chrome.browser.sync.settings.SignInPreference;
import org.chromium.chrome.browser.sync.settings.SyncSettingsUtils;
import org.chromium.chrome.browser.toolbar.adaptive.AdaptiveToolbarStatePredictor;
import org.chromium.chrome.browser.tracing.settings.DeveloperSettings;
import org.chromium.chrome.browser.ui.signin.TangibleSyncCoordinator;
import org.chromium.components.browser_ui.settings.ChromeBasePreference;
import org.chromium.components.browser_ui.settings.ManagedPreferenceDelegate;
import org.chromium.components.browser_ui.settings.SettingsLauncher;
import org.chromium.components.browser_ui.settings.SettingsUtils;
import org.chromium.components.browser_ui.styles.SemanticColorUtils;
import org.chromium.components.search_engines.TemplateUrl;
import org.chromium.components.search_engines.TemplateUrlService;
import org.chromium.components.signin.base.CoreAccountInfo;
import org.chromium.components.signin.identitymanager.ConsentLevel;
import org.chromium.components.signin.metrics.SigninAccessPoint;
import org.chromium.components.user_prefs.UserPrefs;
import org.chromium.components.version_info.VersionInfo;
import org.chromium.ui.modaldialog.ModalDialogManager;
import org.chromium.ui.text.SpanApplier;
import org.chromium.ui.text.SpanApplier.SpanInfo;
import org.ecosia.utils.SettingsHelpers;

import java.util.HashMap;
import java.util.Map;

/**
 * The main settings screen, shown when the user first opens Settings.
 */
public class MainSettings extends PreferenceFragmentCompat
        implements TemplateUrlService.LoadListener, SyncService.SyncStateChangedListener,
                   SigninManager.SignInStateObserver {
    public static final String PREF_SYNC_PROMO = "sync_promo";
    public static final String PREF_ACCOUNT_AND_GOOGLE_SERVICES_SECTION =
            "account_and_google_services_section";
    public static final String PREF_SIGN_IN = "sign_in";
    public static final String PREF_MANAGE_SYNC = "manage_sync";
    public static final String PREF_GOOGLE_SERVICES = "google_services";
    public static final String PREF_SEARCH_ENGINE = "search_engine";
    public static final String PREF_PASSWORDS = "passwords";
    public static final String PREF_HOMEPAGE = "homepage";
    public static final String PREF_TOOLBAR_SHORTCUT = "toolbar_shortcut";
    public static final String PREF_UI_THEME = "ui_theme";
    public static final String PREF_PRIVACY = "privacy";
    public static final String PREF_SAFETY_CHECK = "safety_check";
    public static final String PREF_NOTIFICATIONS = "notifications";
    public static final String PREF_DOWNLOADS = "downloads";
    public static final String PREF_DEVELOPER = "developer";
    public static final String PREF_DEFAULT_BROWSER = "default_browser"; // Ecosia: default browser

    // Used for elevating the privacy section behind the flag (see crbug.com/1099233).
    public static final int PRIVACY_ORDER_DEFAULT = 18;
    public static final int PRIVACY_ORDER_ELEVATED = 12;

    private final ManagedPreferenceDelegate mManagedPreferenceDelegate;
    private final Map<String, Preference> mAllPreferences = new HashMap<>();
    // private SignInPreference mSignInPreference; // Ecosia: deactive sync
    private ChromeBasePreference mManageSync;
    private @Nullable PasswordCheck mPasswordCheck;
    private ObservableSupplier<ModalDialogManager> mModalDialogManagerSupplier;

    private ActivityResultLauncher<Intent> mActivityResultLauncher; // Ecosia: default browser selection preference

    public MainSettings() {
        setHasOptionsMenu(true);
        mManagedPreferenceDelegate = createManagedPreferenceDelegate();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        createPreferences();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.settings);
        mPasswordCheck = PasswordCheckFactory.getOrCreate(new SettingsLauncherImpl());

        /*
            Ecosia: default browser selection preference
            MOB-1761: registerDefaultBrowserActivity() crashes the app on local/dev builds
         */
        if (VersionInfo.isOfficialBuild()) {
            registerDefaultBrowserActivity();
        }
    }

    // Ecosia: default browser selection preference
    private void registerDefaultBrowserActivity() {
        mActivityResultLauncher =  registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        sendTrackingBroadcast(DEFAULT_BROWSER_ACCEPT);
                    } else {
                        sendTrackingBroadcast(DEFAULT_BROWSER_DISMISS);
                    }
                });
    }

    // Ecosia: default browser selection preference
    private void sendTrackingBroadcast(String event) {
        Intent intent = new Intent(DEFAULT_BROWSER_ACTION);
        intent.putExtra(DEFAULT_BROWSER_DATA, event);
        ContextUtils.getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Disable animations of preference changes.
        getListView().setItemAnimator(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /* Ecosia: deactivate sync
        // The component should only be destroyed when the activity has been closed by the user
        // (e.g. by pressing on the back button) and not when the activity is temporarily destroyed
        // by the system.
        if (getActivity().isFinishing() && mPasswordCheck != null) PasswordCheckFactory.destroy();
        */
    }

    @Override
    public void onStart() {
        super.onStart();
        /* Ecosia: deactivate sync
        SigninManager signinManager = IdentityServicesProvider.get().getSigninManager(
                Profile.getLastUsedRegularProfile());
        if (signinManager.isSigninSupported()) {
            signinManager.addSignInStateObserver(this);
        }
        SyncService syncService = SyncService.get();
        if (syncService != null) {
            syncService.addSyncStateChangedListener(this);
        }
        */
    }

    @Override
    public void onStop() {
        super.onStop();
        /* Ecosia: deactivate sync
        SigninManager signinManager = IdentityServicesProvider.get().getSigninManager(
                Profile.getLastUsedRegularProfile());
        if (signinManager.isSigninSupported()) {
            signinManager.removeSignInStateObserver(this);
        }
        SyncService syncService = SyncService.get();
        if (syncService != null) {
            syncService.removeSyncStateChangedListener(this);
        }
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    private void createPreferences() {
        // Ecosia: mSyncPromoPreference preferences
        SettingsUtils.addPreferencesFromResource(this, R.xml.ecosia_main_preferences);
        cachePreferences();

        updatePasswordsPreference();

        if (usesUnifiedPasswordManagerUI()) {
            setManagedPreferenceDelegateForPreference(PREF_PASSWORDS);
        }
		/* Ecosia: remove search preferences
        setManagedPreferenceDelegateForPreference(PREF_SEARCH_ENGINE);
		*/
	
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If we are on Android O+ the Notifications preference should lead to the Android
            // Settings notifications page.
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE,
                    ContextUtils.getApplicationContext().getPackageName());
            PackageManager pm = getActivity().getPackageManager();
            if (intent.resolveActivity(pm) != null) {
                Preference notifications = findPreference(PREF_NOTIFICATIONS);
                notifications.setOnPreferenceClickListener(preference -> {
                    startActivity(intent);
                    // We handle the click so the default action isn't triggered.
                    return true;
                });
            } else {
                removePreferenceIfPresent(PREF_NOTIFICATIONS);
            }
        } else {
            // The per-website notification settings page can be accessed from Site
            // Settings, so we don't need to show this here.
            getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATIONS));
        }

        if (!TemplateUrlServiceFactory.get().isLoaded()) {
            TemplateUrlServiceFactory.get().registerLoadListener(this);
            TemplateUrlServiceFactory.get().load();
        }

        new AdaptiveToolbarStatePredictor(null).recomputeUiState(uiState -> {
            // We don't show the toolbar shortcut settings page if disabled from finch.
            if (uiState.canShowUi) return;
            /* Ecosia : Removing A/B experiment for toolbar shortcut which caused crash in settings
            getPreferenceScreen().removePreference(findPreference(PREF_TOOLBAR_SHORTCUT));
            */
        });
    }

    /**
     * Stores all preferences in memory so that, if they needed to be added/removed from the
     * PreferenceScreen, there would be no need to reload them from 'main_preferences.xml'.
     */
    private void cachePreferences() {
        int preferenceCount = getPreferenceScreen().getPreferenceCount();
        for (int index = 0; index < preferenceCount; index++) {
            Preference preference = getPreferenceScreen().getPreference(index);
            mAllPreferences.put(preference.getKey(), preference);
        }
        // Ecosia: deactivate sync
        // mSignInPreference = (SignInPreference) mAllPreferences.get(PREF_SIGN_IN);
        mManageSync = (ChromeBasePreference) findPreference(PREF_MANAGE_SYNC);
    }

    private void setManagedPreferenceDelegateForPreference(String key) {
        ChromeBasePreference chromeBasePreference = (ChromeBasePreference) mAllPreferences.get(key);
        chromeBasePreference.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
    }

    private void updatePreferences() {
        /* Ecosia : Signin is removed from settings
        if (IdentityServicesProvider.get()
                        .getSigninManager(Profile.getLastUsedRegularProfile())
                        .isSigninSupported()) {
            addPreferenceIfAbsent(PREF_SIGN_IN);
        } else {
            removePreferenceIfPresent(PREF_SIGN_IN);
        }
        */
        updateManageSyncPreference();
        updateSearchEnginePreference();
        updatePasswordsPreference();

		/* Ecosia: deactivate Homepage setting
        Preference homepagePref = addPreferenceIfAbsent(PREF_HOMEPAGE);
        setOnOffSummary(homepagePref, HomepageManager.isHomepageEnabled());
		*/

        if (NightModeUtils.isNightModeSupported()) {
            addPreferenceIfAbsent(PREF_UI_THEME)
                    .getExtras()
                    .putInt(ThemeSettingsFragment.KEY_THEME_SETTINGS_ENTRY,
                            ThemeSettingsEntry.SETTINGS);
        } else {
            removePreferenceIfPresent(PREF_UI_THEME);
        }

        if (DeveloperSettings.shouldShowDeveloperSettings()) {
            addPreferenceIfAbsent(PREF_DEVELOPER);
        } else {
            removePreferenceIfPresent(PREF_DEVELOPER);
        }

        // Ecosia: default browser selection preference
        updateDefaultBrowserPreference();
    }

    // Ecosia: default browser selection preference
    private void updateDefaultBrowserPreference() {
        Preference defaultBrowserPreference = findPreference(PREF_DEFAULT_BROWSER);
        Context context = ContextUtils.getApplicationContext();
        if (defaultBrowserPreference != null) {
            if (SettingsHelpers.isEcosiaDefaultBrowser(context)) {
                defaultBrowserPreference.setVisible(false);
            } else {
                defaultBrowserPreference.setVisible(true);
                defaultBrowserPreference.setOnPreferenceClickListener(preference -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER);
                        mActivityResultLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_APP_BROWSER);
                        startActivity(intent);
                    }
                    return true;
                });
            }
        }
    }

    private Preference addPreferenceIfAbsent(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if (preference == null) getPreferenceScreen().addPreference(mAllPreferences.get(key));
        return mAllPreferences.get(key);
    }

    private void removePreferenceIfPresent(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        if (preference != null) getPreferenceScreen().removePreference(preference);
    }

    private void updateManageSyncPreference() {
        /* Ecosia remove sync
        String primaryAccountName = CoreAccountInfo.getEmailFrom(
                IdentityServicesProvider.get()
                        .getIdentityManager(Profile.getLastUsedRegularProfile())
                        .getPrimaryAccountInfo(ConsentLevel.SIGNIN));
        boolean showManageSync = primaryAccountName != null;
        mManageSync.setVisible(showManageSync);
        if (!showManageSync) return;

        boolean isSyncConsentAvailable =
                IdentityServicesProvider.get()
                        .getIdentityManager(Profile.getLastUsedRegularProfile())
                        .getPrimaryAccountInfo(ConsentLevel.SYNC)
                != null;
        mManageSync.setIcon(SyncSettingsUtils.getSyncStatusIcon(getActivity()));
        mManageSync.setSummary(SyncSettingsUtils.getSyncStatusSummary(getActivity()));
        mManageSync.setOnPreferenceClickListener(pref -> {
            Context context = getContext();
            if (SyncService.get().isSyncDisabledByEnterprisePolicy()) {
                SyncSettingsUtils.showSyncDisabledByAdministratorToast(context);
            } else if (isSyncConsentAvailable) {
                SettingsLauncher settingsLauncher = new SettingsLauncherImpl();
                settingsLauncher.launchSettingsActivity(context, ManageSyncSettings.class);
            } else if (ChromeFeatureList.isEnabled(ChromeFeatureList.TANGIBLE_SYNC)) {
                TangibleSyncCoordinator.start(requireContext(), mModalDialogManagerSupplier.get(),
                        SyncConsentActivityLauncherImpl.get(),
                        SigninAccessPoint.SETTINGS_SYNC_OFF_ROW);
            } else {
                SyncConsentActivityLauncherImpl.get().launchActivityForPromoDefaultFlow(
                        context, SigninAccessPoint.SETTINGS_SYNC_OFF_ROW, primaryAccountName);
            }
            return true;
        });
        */
    }

    private void updateSearchEnginePreference() {
        /* Ecosia: disabled 
        if (!TemplateUrlServiceFactory.get().isLoaded()) {
            ChromeBasePreference searchEnginePref =
                    (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE);
            searchEnginePref.setEnabled(false);
            return;
        }

        String defaultSearchEngineName = null;
        TemplateUrl dseTemplateUrl =
                TemplateUrlServiceFactory.get().getDefaultSearchEngineTemplateUrl();
        if (dseTemplateUrl != null) defaultSearchEngineName = dseTemplateUrl.getShortName();

        Preference searchEnginePreference = findPreference(PREF_SEARCH_ENGINE);
        searchEnginePreference.setEnabled(true);
        searchEnginePreference.setSummary(defaultSearchEngineName);
        */
    }

    private void updatePasswordsPreference() {
        Preference passwordsPreference = findPreference(PREF_PASSWORDS);
        if (usesUnifiedPasswordManagerBranding()) {
            // TODO(crbug.com/1217070): Move this to the layout xml once the feature is rolled out
            passwordsPreference.setTitle(getPasswordsPreferenceElementTitle());
        }
        passwordsPreference.setOnPreferenceClickListener(preference -> {
            if (shouldShowNewLabelForPasswordsPreference()) {
                UserPrefs.get(Profile.getLastUsedRegularProfile())
                        .setBoolean(Pref.PASSWORDS_PREF_WITH_NEW_LABEL_USED, true);
            }
            PasswordManagerLauncher.showPasswordSettings(getActivity(),
                    ManagePasswordsReferrer.CHROME_SETTINGS, mModalDialogManagerSupplier);
            return true;
        });
    }

    private boolean shouldShowNewLabelForPasswordsPreference() {
        return usesUnifiedPasswordManagerUI() && hasChosenToSyncPasswords(SyncService.get())
                && !UserPrefs.get(Profile.getLastUsedRegularProfile())
                            .getBoolean(Pref.PASSWORDS_PREF_WITH_NEW_LABEL_USED);
    }

    // TODO(crbug.com/1217070): remove this method once UPM feature is rolled out.
    // String should be defined in the layout XML.
    private CharSequence getPasswordsPreferenceElementTitle() {
        Context context = getContext();
        if (shouldShowNewLabelForPasswordsPreference()) {
            // Show the styled "New" text if the user did not accessed the new Password Manager
            // settings.
            return SpanApplier.applySpans(context.getString(R.string.password_settings_title_gpm),
                    new SpanInfo("<new>", "</new>", new SuperscriptSpan(),
                            new RelativeSizeSpan(0.75f),
                            new ForegroundColorSpan(
                                    SemanticColorUtils.getDefaultTextColorAccent1(context))));
        } else {
            // Remove the "NEW" text and the trailing whitespace.
            return (CharSequence) (SpanApplier
                                           .removeSpanText(
                                                   context.getString(
                                                           R.string.password_settings_title_gpm),
                                                   new SpanInfo("<new>", "</new>"))
                                           .toString()
                                           .trim());
        }
    }

    private void setOnOffSummary(Preference pref, boolean isOn) {
        pref.setSummary(isOn ? R.string.text_on : R.string.text_off);
    }

    // SigninManager.SignInStateObserver implementation.
    @Override
    public void onSignedIn() {
       // Ecosia: disabled
    }

    @Override
    public void onSignedOut() {
        updatePreferences();
    }

    // TemplateUrlService.LoadListener implementation.
    @Override
    public void onTemplateUrlServiceLoaded() {
        TemplateUrlServiceFactory.get().unregisterLoadListener(this);
        updateSearchEnginePreference();
    }

    @Override
    public void syncStateChanged() {
        updateManageSyncPreference();
        updatePasswordsPreference();
    }

    @VisibleForTesting
    public ManagedPreferenceDelegate getManagedPreferenceDelegateForTest() {
        return mManagedPreferenceDelegate;
    }

    private ManagedPreferenceDelegate createManagedPreferenceDelegate() {
        return new ChromeManagedPreferenceDelegate() {
            @Override
            public boolean isPreferenceControlledByPolicy(Preference preference) {
				/* Ecosia: remove search settings
                if (PREF_SEARCH_ENGINE.equals(preference.getKey())) {
                    return TemplateUrlServiceFactory.get().isDefaultSearchManaged();
                }
                if (usesUnifiedPasswordManagerUI() && PREF_PASSWORDS.equals(preference.getKey())) {
                    return UserPrefs.get(Profile.getLastUsedRegularProfile())
                            .isManagedPreference(Pref.CREDENTIALS_ENABLE_SERVICE);
                }
				*/
                return false;
            }

            @Override
            public boolean isPreferenceClickDisabledByPolicy(Preference preference) {
				/* Ecosia: remove search preferences
                if (PREF_SEARCH_ENGINE.equals(preference.getKey())) {
                    return TemplateUrlServiceFactory.get().isDefaultSearchManaged();
                }
                if (usesUnifiedPasswordManagerUI() && PREF_PASSWORDS.equals(preference.getKey())) {
                    return false;
                }
                return isPreferenceControlledByPolicy(preference)
                        || isPreferenceControlledByCustodian(preference);
				*/
				return false;
            }
        };
    }

    public void setModalDialogManagerSupplier(
            ObservableSupplier<ModalDialogManager> modalDialogManagerSupplier) {
        mModalDialogManagerSupplier = modalDialogManagerSupplier;
    }
}
