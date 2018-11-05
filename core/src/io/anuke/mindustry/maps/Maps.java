package io.anuke.mindustry.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.io.MapIO;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.ThreadArray;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.*;

public class Maps implements Disposable{
    /**List of all built-in maps.*/
    private static final String[] defaultMapNames = {"sandbox"};
    /**Tile format version.*/
    private static final int version = 0;

    /**Maps map names to the real maps.*/
    private ObjectMap<String, Map> maps = new ObjectMap<>();
    /**All maps stored in an ordered array.*/
    private Array<Map> allMaps = new ThreadArray<>();
    /**Temporary array used for returning things.*/
    private Array<Map> returnArray = new ThreadArray<>();

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
        return maps.get(name.toLowerCase());
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

    /**Save a map. This updates all values and stored data necessary.*/
    public void saveMap(String name, MapTileData data, ObjectMap<String, String> tags){
        try{
            //create copy of tags to prevent mutation later
            ObjectMap<String, String> newTags = new ObjectMap<>();
            newTags.putAll(tags);
            tags = newTags;

            FileHandle file = customMapDirectory.child(name + "." + mapExtension);
            MapIO.writeMap(file.write(false), tags, data);

            if(maps.containsKey(name)){
                if(maps.get(name).texture != null) {
                    maps.get(name).texture.dispose();
                    maps.get(name).texture = null;
                }
                allMaps.removeValue(maps.get(name), true);
            }

            Map map = new Map(name, new MapMeta(version, tags, data.width(), data.height(), null), true, getStreamFor(name));
            if(!headless){
                map.texture = new Texture(MapIO.generatePixmap(data));
            }
            allMaps.add(map);

            maps.put(name, map);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**Removes a map completely.*/
    public void removeMap(Map map){
        if(map.texture != null){
            map.texture.dispose();
            map.texture = null;
        }

        maps.remove(map.name);
        allMaps.removeValue(map, true);

        customMapDirectory.child(map.name + "." + mapExtension).delete();
    }

    private void loadMap(String name, Supplier<InputStream> supplier, boolean custom) throws IOException{
        try(DataInputStream ds = new DataInputStream(supplier.get())) {
            MapMeta meta = MapIO.readMapMeta(ds);
            Map map = new Map(name, meta, custom, supplier);

            if (!headless){
                map.texture = new Texture(MapIO.generatePixmap(MapIO.readTileData(ds, meta, true)));
            }

            maps.put(map.name.toLowerCase(), map);
            allMaps.add(map);
        }
    }

    private void loadCustomMaps(){
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
    }

    /**Returns an input stream supplier for a given map name.*/
    private Supplier<InputStream> getStreamFor(String name){
        return customMapDirectory.child(name + "." + mapExtension)::read;
    }

    @Override
    public void dispose() {

    }
}