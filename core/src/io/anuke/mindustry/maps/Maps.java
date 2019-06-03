package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.ExceptionRunnable;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.util.*;
import io.anuke.arc.util.serialization.Json;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.io.LegacyMapIO;
import io.anuke.mindustry.io.MapIO;

import java.io.IOException;
import java.io.StringWriter;

import static io.anuke.mindustry.Vars.*;

public class Maps implements Disposable{
    /** List of all built-in maps. Filenames only. */
    private static final String[] defaultMapNames = {"fortress", "shoreline", "labyrinth"};
    /** All maps stored in an ordered array. */
    private Array<Map> maps = new Array<>();
    /** Serializer for meta. */
    private Json json = new Json();

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

    public Map byName(String name){
        return maps.find(m -> m.name().equals(name));
    }

    /**
     * Loads a map from the map folder and returns it. Should only be used for zone maps.
     * Does not add this map to the map list.
     */
    public Map loadInternalMap(String name){
        FileHandle file = Core.files.internal("maps/" + name + "." + mapExtension);

        try{
            return MapIO.createMap(file, false);
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

    public void reload(){
        dispose();
        load();
    }

    /**
     * Save a custom map to the directory. This updates all values and stored data necessary.
     * The tags are copied to prevent mutation later.
     */
    public void saveMap(ObjectMap<String, String> baseTags){

        try{
            StringMap tags = new StringMap(baseTags);
            String name = tags.get("name");
            if(name == null) throw new IllegalArgumentException("Can't save a map with no name. How did this happen?");
            FileHandle file;

            //find map with the same exact display name
            Map other = maps.find(m -> m.name().equals(name));

            if(other != null){
                //dispose of map if it's already there
                if(other.texture != null){
                    other.texture.dispose();
                    other.texture = null;
                }
                maps.remove(other);
                file = other.file;
            }else{
                file = findFile();
            }

            //create map, write it, etc etc etc
            Map map = new Map(file, world.width(), world.height(), tags, true);
            MapIO.writeMap(file, map);

            if(!headless){
                map.texture = new Texture(MapIO.generatePreview(world.getTiles()));
            }
            maps.add(map);
            maps.sort();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Creates a legacy map by converting it to a non-legacy map and pasting it in a temp directory.
     * Should be followed up by {@link #importMap(FileHandle)} .*/
    public Map makeLegacyMap(FileHandle file) throws IOException{
        FileHandle dst = tmpDirectory.child("conversion_map." + mapExtension);
        LegacyMapIO.convertMap(file, dst);
        return MapIO.createMap(dst, true);
    }

    /** Import a map, then save it. This updates all values and stored data necessary. */
    public void importMap(FileHandle file) throws IOException{
        FileHandle dest = findFile();
        file.copyTo(dest);

        loadMap(dest, true);
    }

    /** Attempts to run the following code;
     * catches any errors and attempts to display them in a readable way.*/
    public void tryCatchMapError(ExceptionRunnable run){
        try{
            run.run();
        }catch(Exception e){
            Log.err(e);

            if("Outdated legacy map format".equals(e.getMessage())){
                ui.showError("$editor.errorlegacy");
            }else if(e.getMessage() != null && e.getMessage().contains("Incorrect header!")){
                ui.showError("$editor.errorheader");
            }else{
                ui.showError(Core.bundle.format("editor.errorload", Strings.parseException(e, true)));
            }
        }
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

    public String writeWaves(Array<SpawnGroup> groups){
        if(groups == null){
            return "[]";
        }

        StringWriter buffer = new StringWriter();
        json.setWriter(buffer);

        json.writeArrayStart();
        for(int i = 0; i < groups.size; i++){
            json.writeObjectStart(SpawnGroup.class, SpawnGroup.class);
            groups.get(i).write(json);
            json.writeObjectEnd();
        }
        json.writeArrayEnd();
        return buffer.toString();
    }

    public Array<SpawnGroup> readWaves(String str){
        return str == null ? null : str.equals("[]") ? new Array<>() : Array.with(json.fromJson(SpawnGroup[].class, str));
    }

    public void loadLegacyMaps(){
        boolean convertedAny = false;
        for(FileHandle file : customMapDirectory.list()){
            if(file.extension().equalsIgnoreCase(oldMapExtension)){
                try{
                    convertedAny = true;
                    LegacyMapIO.convertMap(file, file.sibling(file.nameWithoutExtension() + "." + mapExtension));
                    //delete old, converted file; it is no longer useful
                    file.delete();
                    Log.info("Converted file {0}", file);
                }catch(Exception e){
                    //rename the file to a 'mmap_conversion_failed' extension to keep it there just in case
                    //but don't delete it
                    file.copyTo(file.sibling(file.name() + "_conversion_failed"));
                    file.delete();
                    Log.err(e);
                }
            }
        }

        //free up any potential memory that was used up during conversion
        if(convertedAny){
            world.createTiles(1, 1);
            //reload maps to load the converted ones
            reload();
        }
    }

    /** Find a new filename to put a map to. */
    private FileHandle findFile(){
        //find a map name that isn't used.
        int i = maps.size;
        while(customMapDirectory.child("map_" + i + "." + mapExtension).exists()){
            i++;
        }
        return customMapDirectory.child("map_" + i + "." + mapExtension);
    }

    private void loadMap(FileHandle file, boolean custom) throws IOException{
        Map map = MapIO.createMap(file, custom);

        if(map.name() == null){
            throw new IOException("Map name cannot be empty! File: " + file);
        }

        if(!headless){
            map.texture = new Texture(MapIO.generatePreview(map));
        }

        maps.add(map);
        maps.sort();
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
        for(Map map : maps){
            if(map.texture != null){
                map.texture.dispose();
                map.texture = null;
            }
        }
        maps.clear();
    }
}