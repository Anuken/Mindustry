package io.anuke.ucore.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.util.Mathf;

public class Sounds{
	private static ObjectMap<String, Sound> map = new ObjectMap<>();
	private static float volume = 1f, globalVolume = 1f;
	private static float falloff = 20000f;
	private static boolean muted;
	private static boolean headless;

	public static void setHeadless(boolean headless){
		Sounds.headless = headless;
	}
	
	public static void load(String... names){
		Settings.defaults("sfxvol", 10);
		
		for(String s : names){
			map.put(s.split("\\.")[0], Gdx.audio.newSound(Gdx.files.internal("sounds/"+s)));
		}
	}
	
	/**Default = 20000, bigger numbers mean higher volume.*/
	public static void setFalloff(float afalloff){
		falloff = afalloff;
	}
	
	public static Sound get(String name){
		return map.get(name);
	}
	
	public static void playDistance(String name, float distance){
		if(distance < 1) distance = 1;
		play(name, Mathf.clamp(1f/(distance*distance/falloff)));
	}
	
	public static void play(String name){
		play(name, 1f);
	}
	
	public static void play(String name, float tvol){
		if(headless) return;
		if(!map.containsKey(name)) throw new IllegalArgumentException("Sound \""+name+"\" does not exist!");
		if(muted) return;
		float vol = Settings.getInt("sfxvol")/10f;
		get(name).play(volume*vol*tvol*globalVolume);
	}

	public static void setMuted(boolean muted){
		Sounds.muted = muted;
	}
	
	/**This is modified by things such as settings dialogs.*/
	public static void setVolume(float vol){
		volume = vol;
	}
	
	/**Global volume; everything is multiplied by this.*/
	public static void setGlobalVolume(float vol){
		globalVolume = vol;
	}
	
	static void dispose(){
		volume = 1f;
		globalVolume = 1f;
		
		for(Sound sound : map.values()){
			sound.dispose();
		}
	}
}
