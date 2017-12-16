package io.anuke.mindustry.io;

import java.util.Date;

public interface PlatformFunction{
	public String format(Date date);
	public String format(int number);
	public void openLink(String link);
}
