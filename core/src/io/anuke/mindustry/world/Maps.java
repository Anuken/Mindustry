package io.anuke.mindustry.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Json.Serializer;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import io.anuke.mindustry.Vars;
import io.anuke.ucore.graphics.Pixmaps;

public class Maps implements Disposable{
	private IntMap<Map> maps = new IntMap<>();
	private ObjectMap<String, Map> mapNames = new ObjectMap<>();
	private int lastID;
	private Json json = new Json();

	public Maps() {
		json.setOutputType(OutputType.json);
		json.setElementType(ArrayContainer.class, "maps", Map.class);
		json.setSerializer(Color.class, new ColorSerializer());
	}

	public Iterable<Map> list(){
		return maps.values();
	}

	public Map getMap(int id){
		return maps.get(id);
	}

	public Map getMap(String name){
		return mapNames.get(name);
	}

	public void loadMaps(){
		if(!loadMapFile(Gdx.files.internal("maps/maps.json"))){
			throw new RuntimeException("Failed to load maps!");
		}
		
		if(!loadMapFile(Vars.customMapDirectory.child("maps.json"))){
			try{
				Vars.customMapDirectory.child("maps.json").writeString("{}", false);
			}catch(Exception e){
				throw new RuntimeException("Failed to create custom map directory!");
			}
		}
	}

	public void saveMaps(Array<Map> array, FileHandle file){
		json.toJson(new ArrayContainer(array), file);
	}
	
	public void saveCustomMap(Map toSave){
		Array<Map> out = new Array<>();
		for(Map map : maps.values()){
			if(map.custom)
				out.add(map);
		}
		Pixmaps.write(toSave.pixmap, Vars.customMapDirectory.child(toSave.name + ".png"));
		saveMaps(out, Vars.customMapDirectory.child("maps.json"));
	}

	private boolean loadMapFile(FileHandle file){
		try{
			Array<Map> arr = json.fromJson(ArrayContainer.class, file).maps;
			if(arr != null){ //can be an empty map file
				for(Map map : arr){
					map.pixmap = new Pixmap(file.sibling(map.name + ".png"));
					map.texture = new Texture(map.pixmap);
					maps.put(map.id, map);
					mapNames.put(map.name, map);
				}
			}
			return true;
		}catch(Exception e){
			e.printStackTrace();
			Gdx.app.error("Mindustry-Maps", "Failed loading map file: " + file);
			return false;
		}
	}

	@Override
	public void dispose(){
		for(Map map : maps.values()){
			map.texture.dispose();
			map.pixmap.dispose();
		}
		maps.clear();
	}

	private static class ArrayContainer{
		Array<Map> maps;

		ArrayContainer() {
		}

		ArrayContainer(Array<Map> maps) {
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
