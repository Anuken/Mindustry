package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.customMapDirectory;
import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.mapExtension;

public class Maps implements Disposable{
	/**List of all built-in maps.*/
	private static final String[] defaultMapNames = {"test", "trinity", "routerhell", "conveyorhell"};
	/**Tile format version.*/
	private static final int version = 0;

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
				if(file.extension().toLowerCase().equals(mapExtension)) loadMap(file, true);
			}catch (IOException e){
				Log.err("Failed to load custom map file '{0}'!", file);
				Log.err(e);
			}
		}
	}

	private void loadMap(FileHandle file, boolean custom) throws IOException{
		DataInputStream ds = new DataInputStream(file.read());
		MapMeta meta = MapIO.readMapMeta(ds);
		Map map = new Map(file.nameWithoutExtension(), meta, custom);
		if(!headless) map.texture = new Texture(MapIO.generatePixmap(MapIO.readTileData(ds, meta, true)));

		maps.put(map.name, map);
		allMaps.add(map);
	}

	@Override
	public void dispose() {

	}
}
