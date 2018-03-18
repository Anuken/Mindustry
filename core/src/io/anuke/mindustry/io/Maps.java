package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.customMapDirectory;
import static io.anuke.mindustry.Vars.mapExtension;

public class Maps implements Disposable{
	private ObjectMap<String, Map> maps = new ObjectMap<>();
	private Array<Map> allMaps = new Array<>();
	private Array<Map> customMaps = new Array<>();
	private Array<Map> defaultMaps = new Array<>();

	public void load(){
		//TODO
	}

	public void save(){
		//TODO?
	}

	/**Returns a list of all maps, including custom ones.*/
	public Array<Map> all(){
		return allMaps;
	}

	/**Returns map by internal name.*/
	public Map getByName(String name){
		return maps.get(name);
	}

	//TODO GWT support: read from prefs string if custom
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
