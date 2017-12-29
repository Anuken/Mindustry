package io.anuke.mindustry;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Unit;

public class AndroidLauncher extends AndroidApplication{
	boolean doubleScaleTablets = true;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
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
		};
		
		Mindustry.donationsCallable = new Callable(){ @Override public void run(){ showDonations(); } };

		if(doubleScaleTablets && isTablet(this.getContext())){
			Unit.dp.addition = 0.5f;
		}
		
		config.hideStatusBar = true;
		
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
