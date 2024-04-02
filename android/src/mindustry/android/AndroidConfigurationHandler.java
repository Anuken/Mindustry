package mindustry.android;

import android.os.Build;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidConfigurationHandler {

    public static AndroidApplicationConfiguration configure() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.hideStatusBar = true;
        return config;
    }

    private boolean isTablet(Context context){
        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager != null && manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE;
    }
}
