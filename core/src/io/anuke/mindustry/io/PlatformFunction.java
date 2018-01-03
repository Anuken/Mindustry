package io.anuke.mindustry.io;

import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;

public interface PlatformFunction{
	public String format(Date date);
	public String format(int number);
	public void openLink(String link);
	public void addDialog(TextField field);
	public void onSceneChange(String state, String details, String icon);
	public void onGameExit();
	public void openDonations();
	public void requestWritePerms();
}
