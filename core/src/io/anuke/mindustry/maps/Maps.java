package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.util.Disposable;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.world.Tile;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Maps implements Disposable{
    /** List of all built-in maps. */
    private static final String[] defaultMapNames = {"impact0079"};
    /** All maps stored in an ordered array. */
    private Array<Map> maps = new Array<>();

    /** Returns a list of all maps, including custom ones. */
    public Array<Map> all(){
        return maps;
    }

    /** Returns a list of only custom maps. */
    public Array<Map> customMaps(){
        return maps.select(m -> m.custom);
    }

    /** Returns a list of only default maps. */
    public Array<Map> defaultMaps(){
        return maps.select(m -> !m.custom);
    }

    /**
     * Loads a map from the map folder and returns it. Should only be used for zone maps.
     * Does not add this map to the map list.
     */
    public Map loadInternalMap(String name){
        FileHandle file = Core.files.internal("maps/" + name + "." + mapExtension);

        try{
            return MapIO.readMap(file, false);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Load all maps. Should be called at application start. */
    public void load(){
        try{
            for(String name : defaultMapNames){
                FileHandle file = Core.files.internal("maps/" + name + "." + mapExtension);
                loadMap(file, false);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        loadCustomMaps();
    }

    /** Save a map. This updates all values and stored data necessary. */
    public void saveMap(Map map, Tile[][] data){
        try{
            MapIO.writeMap(map.file.write(false), map, data);
            if(!headless){
                map.texture = new Texture(MapIO.generatePreview(data));
            }
            maps.add(map);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Import a map, then save it. This updates all values and stored data necessary. */
    public void importMap(FileHandle file, Map map){
        file.copyTo(customMapDirectory.child(file.name()));
        if(!headless){
            map.texture = new Texture(MapIO.generatePreview(map));
        }
        maps.add(map);
    }

    /** Removes a map completely. */
    public void removeMap(Map map){
        if(map.texture != null){
            map.texture.dispose();
            map.texture = null;
        }

        maps.remove(map);
        map.file.delete();
    }

    private void loadMap(FileHandle file, boolean custom) throws IOException{
        Map map = MapIO.readMap(file, custom);

        if(!headless){
            map.texture = new Texture(MapIO.generatePreview(map));
        }

        maps.add(map);
    }

    private void loadCustomMaps(){
        for(FileHandle file : customMapDirectory.list()){
            try{
                if(file.extension().equalsIgnoreCase(mapExtension)){
                    loadMap(file, true);
                }
            }catch(Exception e){
                Log.err("Failed to load custom map file '{0}'!", file);
                Log.err(e);
            }
        }
    }

    @Override
    public void dispose(){

    }
}