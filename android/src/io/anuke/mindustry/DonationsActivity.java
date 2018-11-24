package io.anuke.mindustry;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import org.sufficientlysecure.donations.DonationsFragment;

public class DonationsActivity extends FragmentActivity{
    /**
     * Google
     */
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzG93KhpfBPKTo2jF0yxbWkkmMKwsPNM4SsMj1aDq7vv6n3R+mqJVfprOJxFfJh7JchXTflLIgiaKXFAiU70gJbMTniEWnEaFSxAeF09a7U0RjOwN+7rFwjCG91c2CpYxPanBTQP4zasc1ODPVzq4q6/4ByjhenN71V4WmR08NFIAodcfFPrOkDPil7i8y7cgcd1Ky53U0TS+LLYJttAK3XdTK4s7VE3I5IKoeNa4uwCmIM59R67q2k3cXjLk/nP6MP+y++EzHN/PTiR1sVg4dMP8K31RPw/1QNLPQwJz6Wc872oWwb7xo5gkoXbDc5WPPydsi8F3SyKNaYwzN6CDFQIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{
            "mindustry.donation.1", "mindustry.donation.2", "mindustry.donation.5",
            "mindustry.donation.10", "mindustry.donation.15",
            "mindustry.donation.25", "mindustry.donation.50"};
    DonationsFragment donationsFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setTheme(R.style.GdxTheme);

        setContentView(R.layout.donations_activity);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(BuildConfig.DONATIONS_GOOGLE){
            donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, false, null, null, false, null);
        }


        ft.replace(R.id.donations_activity_container, donationsFragment, "donationsFragment");
        ft.commit();
    }

    public void onStart(){
        super.onStart();
        Button b = findViewById(org.sufficientlysecure.donations.R.id.donations__google_android_market_donate_button);
        b.setOnClickListener(view -> {
            donationsFragment.donateGoogleOnClick(donationsFragment.getView());
            b.setEnabled(false);
        });
    }


    /**
     * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
     * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        Button b = findViewById(org.sufficientlysecure.donations.R.id.donations__google_android_market_donate_button);
        b.setEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
        if(fragment != null){
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
