package io.anuke.mindustry.io;

import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;
import java.util.Locale;

public abstract class Platform {
	public static Platform instance = new Platform() {};

	public String format(Date date){return "invalid";}
	public String format(int number){return "invalid";}
	public void showError(String text){}
	public void addDialog(TextField field){
		addDialog(field, 16);
	}
	public void addDialog(TextField field, int maxLength){}
	public void updateRPC(){}
	public void onGameExit(){}
	public void openDonations(){}
	public boolean hasDiscord(){return true;}
	public void requestWritePerms(){}
	public String getLocaleName(Locale locale){
		return locale.toString();
	}
	public boolean canJoinGame(){
		return true;
	}
}
