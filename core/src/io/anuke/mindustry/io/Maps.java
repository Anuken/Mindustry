package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.customMapDirectory;
import static io.anuke.mindustry.Vars.mapExtension;

public class Maps implements Disposable{
	/**List of all built-in maps.*/
	private static final String[] defaultMapNames = {};

	private ObjectMap<String, Map> maps = new ObjectMap<>();
	private Array<Map> allMaps = new Array<>();
	private Array<Map> returnArray = new Array<>();

	/**Returns a list of all maps, including custom ones.*/
	public Array<Map> all(){
		return allMaps;
	}

	/**Returns a list of only custo maps.*/
	public Array<Map> customMaps(){
		returnArray.clear();
		for(Map map : allMaps){
			if(map.custom) returnArray.add(map);
		}
		return returnArray;
	}

	/**Returns a list of only default maps.*/
	public Array<Map> defaultMaps(){
		returnArray.clear();
		for(Map map : allMaps){
			if(!map.custom) returnArray.add(map);
		}
		return returnArray;
	}

	/**Returns map by internal name.*/
	public Map getByName(String name){
		return maps.get(name);
	}

	//TODO GWT support: read from prefs string if custom
	/**Reads all tile data from a map. Should be used sparingly.*/
	public MapTileData readTileData(Map map){
		try {
			InputStream stream;

			if (map.custom) {
				stream = Gdx.files.local("maps/" + map.name + "." + mapExtension).read();
			} else {
				stream = customMapDirectory.child(map.name + "." + mapExtension).read();
			}

			DataInputStream ds = new DataInputStream(stream);
			MapTileData data = readTileData(ds);
			ds.close();
			return data;
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	/**Load all maps. Should be called at application start.*/
	public void load(){
		try {
			for (String name : defaultMapNames) {
				loadMap(Gdx.files.internal("maps/" + name + "." + mapExtension), false);
			}
		}catch (IOException e){
			throw new RuntimeException(e);
		}

		for(FileHandle file : customMapDirectory.list()){
			try{
				loadMap(file, true);
			}catch (IOException e){
				Log.err("Failed to load custom map file '{0}'!", file);
				Log.err(e);
			}
		}
	}

	private void loadMap(FileHandle file, boolean custom) throws IOException{
		DataInputStream ds = new DataInputStream(file.read());
		MapMeta meta = readMapMeta(ds);
		Map map = new Map(file.nameWithoutExtension(), meta, custom);

		maps.put(map.name, map);
		allMaps.add(map);
	}

	private MapTileData readTileData(DataInputStream stream) throws IOException{
		MapMeta meta = readMapMeta(stream);
		byte[] bytes = new byte[stream.available()];
		stream.read(bytes);
		return new MapTileData(bytes, meta.width, meta.height);
	}

	private MapMeta readMapMeta(DataInputStream stream) throws IOException{
		ObjectMap<String, String> tags = new ObjectMap<>();

		int version = stream.readInt();

		byte tagAmount = stream.readByte();

		for(int i = 0; i < tagAmount; i ++){
			String name = stream.readUTF();
			String value = stream.readUTF();
			tags.put(name, value);
		}

		int width = stream.readShort();
		int height = stream.readShort();

		return new MapMeta(version, tags, width, height);
	}

	@Override
	public void dispose() {

	}
}
