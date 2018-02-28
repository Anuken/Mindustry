package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Json.Serializer;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.*;

public class Maps implements Disposable{
	private IntMap<Map> maps = new IntMap<>();
	private ObjectMap<String, Map> mapNames = new ObjectMap<>();
	private Array<Map> defaultMaps = new Array<>();
	private Map networkMap;
	private int lastID;
	private Json json = new Json();
	private Array<Map> array = new Array<>();

	public Maps() {
		json.setOutputType(OutputType.json);
		json.setElementType(ArrayContainer.class, "maps", Map.class);
		json.setSerializer(Color.class, new ColorSerializer());
	}

	public Iterable<Map> list(){
		return maps.values();
	}

	public Array<Map> getDefaultMaps(){
		return defaultMaps;
	}

	public Array<Map> getCustomMaps(){
		array.clear();
		for(Map map : list()){
			if(map.custom) array.add(map);
		}
		return array;
	}

	public Array<Map> getAllMaps(){
		array.clear();
		for(Map map : list()){
			array.add(map);
		}
		return array;
	}

	public void setNetworkMap(Map map){
		if(networkMap != null){
			networkMap.pixmap.dispose();
			networkMap = null;
		}

		networkMap = map;
	}

	public Map getMap(int id){
		if(id == -1){
			return networkMap;
		}
		return maps.get(id);
	}

	public Map getMap(String name){
		return mapNames.get(name);
	}

	public void loadMaps(){
		if(!loadMapFile(Gdx.files.internal("maps/maps.json"), true)){
			throw new RuntimeException("Failed to load maps!");
		}

		if(!gwt) {
			if (!loadMapFile(customMapDirectory.child("maps.json"), false)) {
				try {
					Log.info("Failed to find custom map directory.");
					customMapDirectory.child("maps.json").writeString("{}", false);
				} catch (Exception e) {
					throw new RuntimeException("Failed to create custom map directory!");
				}
			}
		}
	}
	
	public void removeMap(Map map){
		maps.remove(map.id);
		mapNames.remove(map.name);
		Array<Map> out = new Array<>();
		for(Map m : maps.values()){
			if(m.custom){
				out.add(m);
			}
		}
		saveMaps(out, customMapDirectory.child("maps.json"));
	}
	
	public void saveAndReload(Map map, Pixmap out){
		if(map.pixmap != null && out != map.pixmap && map.texture != null){
			map.texture.dispose();
			map.texture = new Texture(out);
		}else if (out == map.pixmap){
			map.texture.draw(out, 0, 0);
		}
		
		map.pixmap = out;
		if(map.texture == null) map.texture = new Texture(map.pixmap);
		
		if(map.id == -1){
			if(mapNames.containsKey(map.name)){
				map.id = mapNames.get(map.name).id;
			}else{
				map.id = ++lastID;
			}
		}
		
		if(!Settings.has("hiscore" + map.name)){
			Settings.defaults("hiscore" + map.name, 0);
		}
		
		saveCustomMap(map);
		ui.levels.reload();
	}

	public void saveMaps(Array<Map> array, FileHandle file){
		json.toJson(new ArrayContainer(array), file);
	}
	
	public void saveCustomMap(Map toSave){
		toSave.custom = true;
		Array<Map> out = new Array<>();
		boolean added = false;
		for(Map map : maps.values()){
			if(map.custom){
				if(map.name.equals(toSave.name)){
					out.add(toSave);
					toSave.id = map.id;
					added = true;
				}else{
					out.add(map);
				}
			}
		}
		if(!added){
			out.add(toSave);
		}
		maps.remove(toSave.id);
		mapNames.remove(toSave.name);
		maps.put(toSave.id, toSave);
		mapNames.put(toSave.name, toSave);
		Pixmaps.write(toSave.pixmap, customMapDirectory.child(toSave.name + ".png"));
		saveMaps(out, customMapDirectory.child("maps.json"));
	}

	private boolean loadMapFile(FileHandle file, boolean logException){
		try {
			Array<Map> arr = json.fromJson(ArrayContainer.class, file).maps;
			if (arr != null) { //can be an empty map file
				for (Map map : arr) {
					map.pixmap = new Pixmap(file.sibling(map.name + ".png"));
					if (!headless) map.texture = new Texture(map.pixmap);
					maps.put(map.id, map);
					mapNames.put(map.name, map);
					lastID = Math.max(lastID, map.id);
					if (!map.custom) {
						defaultMaps.add(map);
					}
				}
			}
			return true;
		}catch (GdxRuntimeException e){
			Log.err(e);
			return true;
		}catch(Exception e){
			if(logException) {
				Log.err(e);
				Log.err("Failed loading map file: {0}", file);
			}
			return false;
		}
	}

	@Override
	public void dispose(){
		for(Map map : maps.values()){
			if(map.texture != null) map.texture.dispose();
			map.pixmap.dispose();
		}
		maps.clear();
	}

	public static class ArrayContainer{
		Array<Map> maps;

		public ArrayContainer() {
		}

		public ArrayContainer(Array<Map> maps) {
			this.maps = maps;
		}
	}

	private class ColorSerializer implements Serializer<Color>{

		@Override
		public void write(Json json, Color object, Class knownType){
			json.writeValue(object.toString().substring(0, 6));
		}

		@Override
		public Color read(Json json, JsonValue jsonData, Class type){
			return Color.valueOf(jsonData.asString());
		}

	}
}
