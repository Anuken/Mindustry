package io.anuke.mindustry;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import android.annotation.SuppressLint;
import android.os.Bundle;
import io.anuke.mindustry.io.Formatter;

public class AndroidLauncher extends AndroidApplication {
	@SuppressLint("SimpleDateFormat")
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;
		
		Mindustry.formatter = new Formatter(){
			@SuppressLint("SimpleDateFormat")
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm");
			
			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}
		};
		
		initialize(new Mindustry(), config);
	}
}
