package io.anuke.mindustry;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import android.os.Bundle;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.SaveIO.FormatProvider;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;
		
		SaveIO.setFormatProvider(new FormatProvider(){
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			
			public String format(Date date){
				return format.format(date);
			}
		});
		
		initialize(new Mindustry(), config);
	}
}
