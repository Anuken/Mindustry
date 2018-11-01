package io.anuke.mindustry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Base64Coder;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import io.anuke.kryonet.DefaultThreadImpl;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Unit;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class AndroidLauncher extends AndroidApplication{
	boolean doubleScaleTablets = true;
	int WRITE_REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;

		Platform.instance = new Platform(){
			DateFormat format = SimpleDateFormat.getDateTimeInstance();

			@Override
			public boolean hasDiscord() {
				return isPackageInstalled("com.discord");
			}

			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}

			@Override
			public void addDialog(TextField field, int length){
				TextFieldDialogListener.add(field, 0, length);
			}

			@Override
			public String getLocaleName(Locale locale){
				return locale.getDisplayName(locale);
			}

			@Override
			public void openDonations() {
				showDonations();
			}

			@Override
			public void requestWritePerms() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
							checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
						requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
								Manifest.permission.READ_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
					}else{

						if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
							requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
						}

						if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
							requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
						}
					}
				}
			}

			@Override
			public ThreadProvider getThreadProvider() {
				return new DefaultThreadImpl();
			}

			@Override
			public boolean isDebug() {
				return false;
			}

			@Override
			public byte[] getUUID() {
				try {
					String s = Secure.getString(getContext().getContentResolver(),
							Secure.ANDROID_ID);

					int len = s.length();
					byte[] data = new byte[len / 2];
					for (int i = 0; i < len; i += 2) {
						data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
								+ Character.digit(s.charAt(i + 1), 16));
					}

					if(new String(Base64Coder.encode(data)).equals("AAAAAAAAAOA=")) throw new RuntimeException("Bad UUID.");

					return data;
				}catch (Exception e){

                    String uuid = Settings.getString("uuid", "");
                    if(uuid.isEmpty()){
                        byte[] result = new byte[8];
                        new Random().nextBytes(result);
                        uuid = new String(Base64Coder.encode(result));
                        Settings.putString("uuid", uuid);
                        Settings.save();
                        return result;
                    }
                    return Base64Coder.decode(uuid);
				}
			}
		};

		try {
			ProviderInstaller.installIfNeeded(this);
		} catch (GooglePlayServicesRepairableException e) {
			GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
			apiAvailability.getErrorDialog(this, e.getConnectionStatusCode(), 0).show();
		} catch (GooglePlayServicesNotAvailableException e) {
			Log.e("SecurityException", "Google Play Services not available.");
		}

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
