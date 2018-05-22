package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.*;

public class Maps implements Disposable{
	/**List of all built-in maps.*/
	private static final String[] defaultMapNames = {"test"};
	/**Tile format version.*/
	private static final int version = 0;

	/**Maps map names to the real maps.*/
	private ObjectMap<String, Map> maps = new ObjectMap<>();
	/**All maps stored in an ordered array.*/
	private Array<Map> allMaps = new Array<>();
	/**Temporary array used for returning things.*/
	private Array<Map> returnArray = new Array<>();
	/**Used for writing a list of custom map names on GWT.*/
	private Json json = new Json();

	public Maps(){

    }

	/**Returns a list of all maps, including custom ones.*/
	public Array<Map> all(){
		return allMaps;
	}

	/**Returns a list of only custom maps.*/
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
			    FileHandle file = Gdx.files.internal("maps/" + name + "." + mapExtension);
				loadMap(file.nameWithoutExtension(), file::read, false);
			}
		}catch (IOException e){
			throw new RuntimeException(e);
		}

		loadCustomMaps();
	}

	public void saveAndReload(String name, MapTileData data, ObjectMap<String, String> tags){
	    FileHandle file = customMapDirectory.child(name + "." + mapExtension);
	    //todo implement
	}

	private void loadMap(String name, Supplier<InputStream> supplier, boolean custom) throws IOException{
	    try(DataInputStream ds = new DataInputStream(supplier.get())) {
            MapMeta meta = MapIO.readMapMeta(ds);
            Map map = new Map(name, meta, custom, supplier);
            if (!headless) map.texture = new Texture(MapIO.generatePixmap(MapIO.readTileData(ds, meta, true)));

            maps.put(map.name, map);
            allMaps.add(map);
        }
	}

	private void loadCustomMaps(){
	    if(!gwt){
            for(FileHandle file : customMapDirectory.list()){
                try{
                    if(file.extension().equalsIgnoreCase(mapExtension)){
                        loadMap(file.nameWithoutExtension(), file::read, true);
                    }
                }catch (Exception e){
                    Log.err("Failed to load custom map file '{0}'!", file);
                    Log.err(e);
                }
            }

        }else{
            Array<String> maps = json.fromJson(Array.class, Settings.getString("custom-maps", "{}"));

            for(String name : maps){
                try{
                    String data = Settings.getString("map-data-" + name);
                    byte[] bytes = data.getBytes();
                    loadMap(name, () -> new ByteArrayInputStream(bytes), true);
                }catch (Exception e){
                    Log.err("Failed to load custom map '{0}'!", name);
                    Log.err(e);
                }
            }
        }
    }

	@Override
	public void dispose() {

	}
}
