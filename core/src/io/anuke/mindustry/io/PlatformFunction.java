package io.anuke.mindustry.io;

import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;

public abstract class PlatformFunction{
	public String format(Date date){return "invalid";}
	public String format(int number){return "invalid";}
	public void openLink(String link){}
	public void showError(String text){}
	public void addDialog(TextField field){
		addDialog(field, 16);
	}
	public void addDialog(TextField field, int maxLength){}
	public void updateRPC(){}
	public void onGameExit(){}
	public void openDonations(){}
	public void requestWritePerms(){}
}
