package io.anuke.mindustry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Unit;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AndroidLauncher extends AndroidApplication{
	boolean doubleScaleTablets = true;

	int WRITE_REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
			}
		}

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;
		
		Mindustry.hasDiscord = isPackageInstalled("com.discord");
		Mindustry.platforms = new PlatformFunction(){
			DateFormat format = SimpleDateFormat.getDateTimeInstance();

			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}

			@Override
			public void openLink(String link){
				Uri marketUri = Uri.parse(link);
			    Intent intent = new Intent( Intent.ACTION_VIEW, marketUri );
			    startActivity(intent); 
			}

			@Override
			public void addDialog(TextField field){
				TextFieldDialogListener.add(field);
			}

			@Override
			public void onSceneChange(String state, String details, String icon) { }

			@Override
			public void onGameExit() { }

			@Override
			public void openDonations() {
				showDonations();
			}

			@Override
			public void requestWritePerms() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
						requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
					}
				}
			}
		};

		if(doubleScaleTablets && isTablet(this.getContext())){
			Unit.dp.addition = 0.5f;
		}
		
		config.hideStatusBar = true;

        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());
		
		initialize(new Mindustry(), config);
	}
	
	private boolean isPackageInstalled(String packagename) {
	    try {
	    	getPackageManager().getPackageInfo(packagename, 0);
	        return true;
	    } catch (Exception e) {
	        return false;
	    }
	}
	
	private boolean isTablet(Context context) {
		TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		return manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE;
	}
	
	private void showDonations(){
		Intent intent = new Intent(this, DonationsActivity.class);
		startActivity(intent);
	}
}
