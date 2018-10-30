package io.anuke.ucore.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.util.Mathf;

public class Musics{
	private static Array<Music> music = new Array<>();
	private static ObjectMap<String, Music> map = new ObjectMap<>();
	private static ObjectMap<String, TrackList> tracks = new ObjectMap<>();
	private static Music playing;
	private static TrackList current;
	private static float volume = 1f;
	private static float fadeTime = 100f;
	private static float fading = 0f;

	/** Requires file extensions (e.g "something.mp3") */
	public static void load(String... names){
		Settings.defaults("musicvol", 10);
		
		for(String s : names){
			String name = s.split("\\.")[0];
			music.add(Gdx.audio.newMusic(Gdx.files.internal("music/" + s)));
			map.put(name, music.peek());
			
			music.peek().setOnCompletionListener(new OnCompletionListener(){
				public void onCompletion(Music other){
					//don't switch while transitioning
					if(!Mathf.zero(fading)) return;
					
					Music music = current.select(other);
					playing = music;
					updateVolume();
					music.play();
				}
			});
		}
	}

	public static void setMuted(boolean muted){
		if(muted){
			volume = 0f;
		}else{
			volume = 1f;
		}
		updateVolume();
	}
	
	public static void setVolume(float vol){
		volume = vol;
	}

	public static Music getPlaying(){
		return playing;
	}
	
	public static void updateVolume(){
		if(playing == null) return;
		float vol = Settings.getInt("musicvol")/10f*volume;
		playing.setVolume(vol);
	}
	
	public static float baseVolume(){
		return Settings.getInt("musicvol")/10f*volume;
	}

	public static void shuffleAll(){
		Music[] out = new Music[music.size];
		for(int i = 0; i < music.size; i ++)
			out[i] = music.get(i);
		
		TrackList list = new TrackList(out);
		playTracks(list);
	}

	public static Music get(String name){
		if(!map.containsKey(name)) throw new IllegalArgumentException("The music \"" + name + "\" does not exist!");
		
		return map.get(name);
	}
	
	public static void setFadeTime(float fadetime){
		fadeTime = fadetime;
	}
	
	public static void playTracks(String name){
		playTracks(tracks.get(name));
	}
	
	private static void playTracks(TrackList list){
		if(current == list) return;
		
		Music select = list.select(playing);
		
		current = list;
		
		if(playing == null){
			playing = select;
			updateVolume();
			select.play();
		}else{
			fading = 0f;
			select.play();
			
			Timers.runFor(fadeTime, ()->{
				select.setVolume(Mathf.clamp(fading/fadeTime*baseVolume()));
				playing.setVolume(Mathf.clamp((1f-fading/fadeTime)*baseVolume()));
				fading += Timers.delta();
			}, ()->{
				current = list;
				playing.stop();
				playing = select;
				updateVolume();
				select.play();
				fading = 0f;
			});
		}
	}
	
	//TODO less crappy method name
	public static void createTracks(String name, String...names){
		TrackList list = new TrackList(names);
		tracks.put(name, list);
	}
	
	private static TrackList getTracks(String name){
		if(!tracks.containsKey(name)) throw new IllegalArgumentException("The tracks \"" + name + "\" do not exist!");
		
		return tracks.get(name);
	}
	
	static void dispose(){
		music = new Array<>();
		tracks = new ObjectMap<>();
		playing = null;
		current = null;
		volume = 1f;
		fading = 100f;
		
		for(Music music : map.values()){
			music.dispose();
		}
	}
	
	static class TrackList{
		Music[] tracks;
		
		TrackList(Music[] tracks){
			this.tracks = tracks;
		}
		
		TrackList(String[] tracknames){
			tracks = new Music[tracknames.length];
			
			for(int i = 0; i < tracks.length; i ++){
				tracks[i] = get(tracknames[i]);
			}
		}
		
		Music select(Music previous){
			Music select = previous;
			
			if(!(tracks.length == 1 && tracks[0] == previous)){
				while(select == previous){
					select = Mathf.select(tracks);
				}
			}
			
			return select;
		}
	}
}
