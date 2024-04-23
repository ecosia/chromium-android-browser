package org.ecosia.referrals;

import static org.ecosia.tracking.TrackingManager.ACTION_VIEW;
import static org.ecosia.tracking.TrackingManager.LABEL_INVITE_SCREEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;

import org.chromium.base.IntentUtils;
import org.chromium.base.supplier.OneshotSupplier;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.chrome.browser.init.ActivityProfileProvider;
import org.chromium.chrome.browser.init.AsyncInitializationActivity;
import org.chromium.chrome.browser.profiles.ProfileProvider;
import org.chromium.ui.base.PageTransition;
import org.ecosia.ntp.cards.ImpactCardContainer;
import org.ecosia.ntp.cards.ReferralsCardView;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.ViewResizer;

public class ReferralsActivity extends AsyncInitializationActivity {
    @Override
    public void startNativeInitialization() {
        super.startNativeInitialization();
    }

    @Override
    public boolean shouldStartGpuProcess() {
        return true;
    }

    @NonNull
    @Override
    protected OneshotSupplier<ProfileProvider> createProfileProvider() {
        return new ActivityProfileProvider(getLifecycleDispatcher());
    }

    @Override
    public boolean startMinimalBrowser() {
        return super.startMinimalBrowser();
    }

    private Referrals mReferrals;

    private TrackingManager mTrackingManager;

    @Nullable
    private ReferralsCardView mReferralsCardview;

    private AlertDialog mCreateCodeErrorAlert;

    private final String SHARE_ACTION = "org.ecosia.referrals.SHARE_ACTION";
    private final BroadcastReceiver mShareActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            if (mTrackingManager != null) {
                mTrackingManager.invitationsEvent(TrackingManager.ACTION_SEND, TrackingManager.LABEL_INVITE);
            }
        }
    };

    private void resizeContentViews() {
        final View scrollView = findViewById(R.id.scroll_view);
        final View primaryContainer = findViewById(R.id.primary_container);

        new ViewResizer(this).resizeContentViews(scrollView, primaryContainer);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void triggerLayoutInflation() {
        setContentView(R.layout.ecosia_activity_referrals);
        mReferrals = Referrals.getInstance(this);
        mReferralsCardview = new ReferralsCardView(this, R.layout.ecosia_referrals_activity_card);
        mTrackingManager = TrackingManager.getInstance(this);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.ecosia_referrals_activity_title));
        toolbar.setTitleTextColor(getResources().getColor(R.color.modern_white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImpactCardContainer personalImpactContainer = findViewById(R.id.global_impact_card_container);
        personalImpactContainer.addImpactCard(mReferralsCardview);

        TextView copyButton = findViewById(R.id.copy_button);
        copyButton.setOnClickListener(v -> {
            mTrackingManager.invitationsEvent(TrackingManager.ACTION_CLICK, TrackingManager.LABEL_LINK_COPYING);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getReferralShareTitle(), getReferralShareText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        });

        Button shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(v -> {
            mTrackingManager.invitationsEvent(TrackingManager.ACTION_CLICK, TrackingManager.LABEL_INVITE);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getReferralShareTitle());
            sendIntent.putExtra(Intent.EXTRA_TEXT, getReferralShareText());
            sendIntent.setType("text/plain");

            Intent receiver = new Intent(SHARE_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Intent shareIntent = Intent.createChooser(sendIntent, null, pendingIntent.getIntentSender());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mShareActionReceiver, new IntentFilter(SHARE_ACTION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mShareActionReceiver, new IntentFilter(SHARE_ACTION));
            }
            startActivity(shareIntent);
        });

        TextView learnMoreButton = findViewById(R.id.learn_more_button);
        learnMoreButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.ecosia_referrals_learn_more_url)));
            Bundle startActivityOptions = ActivityOptionsCompat.makeCustomAnimation(
                    this, org.chromium.chrome.R.anim.abc_fade_in, org.chromium.chrome.R.anim.abc_fade_out).toBundle();
            myIntent.setPackage(getPackageName());
            myIntent.putExtra(IntentHandler.EXTRA_PAGE_TRANSITION_TYPE, PageTransition.LINK);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            IntentUtils.addTrustedIntentExtras(myIntent);
            startActivity(myIntent, startActivityOptions);
        });

        setupHowto();
        mTrackingManager.invitationsEvent(ACTION_VIEW, LABEL_INVITE_SCREEN);
    }

    private String getReferralShareTitle() {
        return getString(R.string.ecosia_referrals_share_title);
    }

    private String getReferralShareText() {
        return getString(R.string.ecosia_referrals_share_text, mReferrals.getReferralShareLinkUriString(), mReferrals.getIosReferralUriString());
    }

    private void setupHowtoElement(Drawable image, String title, String description) {
        LinearLayout container = findViewById(R.id.how_it_works_card_container);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View card = layoutInflater.inflate(R.layout.ecosia_referrals_how_it_works_card, null);

        ImageView imageView = card.findViewById(R.id.image_view);
        imageView.setImageDrawable(image);
        imageView.setColorFilter(getColor(R.color.ecosia_button_primary_default));

        TextView titleView = card.findViewById(R.id.title_text_view);
        titleView.setText(title);

        TextView descriptionView = card.findViewById(R.id.description_text_view);
        descriptionView.setText(description);

        container.addView(card);
    }

    private void setupHowto() {
        setupHowtoElement(AppCompatResources.getDrawable(this, R.drawable.ic_referrals_invite_friends), getString(R.string.ecosia_referrals_activity_share_how_it_works_section1_title), getString(R.string.ecosia_referrals_activity_share_how_it_works_section1_description));
        setupHowtoElement(AppCompatResources.getDrawable(this, R.drawable.ic_referrals_download), getString(R.string.ecosia_referrals_activity_share_how_it_works_section2_title), getString(R.string.ecosia_referrals_activity_share_how_it_works_section2_description));
        setupHowtoElement(AppCompatResources.getDrawable(this, R.drawable.ic_referrals_link), getString(R.string.ecosia_referrals_activity_share_how_it_works_section3_title), getString(R.string.ecosia_referrals_activity_share_how_it_works_section3_description));
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity context = this;

        View scrollView = findViewById(R.id.scroll_view);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                resizeContentViews();
            }
        });

        mReferrals.getReferralCode(true, new Referrals.ReferralCodeCallback() {
            @Override
            public void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                if (mReferralsCardview == null) return;

                mReferralsCardview.setTopText(String.valueOf(currentClaimsCount));
                mReferralsCardview.setBottomText(getResources().getQuantityString(R.plurals.ecosia_referrals_ntp_x_friends_invited, currentClaimsCount, currentClaimsCount));

                TextView referralCodeTv = findViewById(R.id.referral_code_text_view);
                String referralShareLinkUriString = mReferrals.getReferralShareLinkUriString();
                if (referralShareLinkUriString != null) {
                    referralCodeTv.setText(referralShareLinkUriString.replace("https://", ""));
                }

                LinearLayout yourFriendsContainer = findViewById(R.id.your_invites_container);
                yourFriendsContainer.setVisibility(View.VISIBLE);

                LinearLayout shareYourLinkLayout = findViewById(R.id.share_your_link_container);
                shareYourLinkLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Referrals.Error error) {
                mCreateCodeErrorAlert = new AlertDialog.Builder(context)
                        .setTitle(R.string.ecosia_referrals_error_network_title)
                        .setMessage(R.string.ecosia_referrals_activity_no_able_to_create_referral_code)
                        .setNeutralButton(R.string.ok, (dialog, which) -> {
                            mCreateCodeErrorAlert.dismiss();
                            context.finish();
                        })
                        .create();
                mCreateCodeErrorAlert.show();
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
