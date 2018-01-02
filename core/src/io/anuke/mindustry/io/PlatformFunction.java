package io.anuke.mindustry.io;

import java.util.Date;

import io.anuke.ucore.scene.ui.TextField;

public interface PlatformFunction{
	public String format(Date date);
	public String format(int number);
	public void openLink(String link);
	public void addDialog(TextField field);
	public void onSceneChange(String state, String details, String icon);
	public void onGameExit();
}
