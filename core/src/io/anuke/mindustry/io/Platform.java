package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;
import java.util.Locale;
import java.util.Random;

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
	public boolean isDebug(){return false;}
	/**Must be 8 bytes in length.*/
	public byte[] getUUID(){
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
	public ThreadProvider getThreadProvider(){
		return new ThreadProvider() {
			@Override public boolean isOnThread() {return true;}
			@Override public void sleep(long ms) {}
			@Override public void start(Runnable run) {}
			@Override public void stop() {}
			@Override public void notify(Object object) {}
			@Override public void wait(Object object) {}
			@Override public <T extends Entity> void switchContainer(EntityGroup<T> group) {}
		};
	}
}
