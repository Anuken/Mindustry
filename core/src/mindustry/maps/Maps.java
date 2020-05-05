package mindustry.maps;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.struct.*;
import arc.struct.IntSet.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.async.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.MapPreviewLoader.*;
import mindustry.maps.filters.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import java.io.*;

import static mindustry.Vars.*;

public class Maps{
    /** List of all built-in maps. Filenames only. */
    private static String[] defaultMapNames = {"maze", "fortress", "labyrinth", "islands", "tendrils", "caldera", "wasteland", "shattered", "fork", "triad", "veins", "glacier"};
    /** All maps stored in an ordered array. */
    private Array<Map> maps = new Array<>();
    /** Serializer for meta. */
    private Json json = new Json();

    private ShuffleMode shuffleMode = ShuffleMode.all;
    private @Nullable MapProvider shuffler;

    private AsyncExecutor executor = new AsyncExecutor(2);
    private ObjectSet<Map> previewList = new ObjectSet<>();

    public ShuffleMode getShuffleMode(){
        return shuffleMode;
    }

    public void setShuffleMode(ShuffleMode mode){
        this.shuffleMode = mode;
    }

    /** Set the provider for the map(s) to be played on. Will override the default shuffle mode setting.*/
    public void setMapProvider(MapProvider provider){
        this.shuffler = provider;
    }

    /** @return the next map to shuffle to. May be null, in which case the server should be stopped. */
    public @Nullable Map getNextMap(@Nullable Map previous){
        return shuffler != null ? shuffler.next(previous) : shuffleMode.next(previous);
    }

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

    public Maps(){
        Events.on(ClientLoadEvent.class, event -> {
            maps.sort();
        });

        Events.on(ContentReloadEvent.class, event -> {
            reload();
            for(Map map : maps){
                try{
                    map.texture = map.previewFile().exists() ? new Texture(map.previewFile()) : new Texture(MapIO.generatePreview(map));
                    readCache(map);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        if(Core.assets != null){
            ((CustomLoader) Core.assets.getLoader(Content.class)).loaded = this::createAllPreviews;
        }
    }

    /**
     * Loads a map from the map folder and returns it. Should only be used for zone maps.
     * Does not add this map to the map list.
     */
    public Map loadInternalMap(String name){
        Fi file = tree.get("maps/" + name + "." + mapExtension);

        try{
            return MapIO.createMap(file, false);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Load all maps. Should be called at application start. */
    public void load(){
        //defaults; must work
        try{
            for(String name : defaultMapNames){
                Fi file = Core.files.internal("maps/" + name + "." + mapExtension);
                loadMap(file, false);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        //custom
        for(Fi file : customMapDirectory.list()){
            try{
                if(file.extension().equalsIgnoreCase(mapExtension)){
                    loadMap(file, true);
                }
            }catch(Exception e){
                Log.err("Failed to load custom map file '@'!", file);
                Log.err(e);
            }
        }

        //workshop
        for(Fi file : platform.getWorkshopContent(Map.class)){
            try{
                Map map = loadMap(file, false);
                map.workshop = true;
                map.tags.put("steamid", file.parent().name());
            }catch(Exception e){
                Log.err("Failed to load workshop map file '@'!", file);
                Log.err(e);
            }
        }

        //mod
        mods.listFiles("maps", (mod, file) -> {
            try{
                Map map = loadMap(file, false);
                map.mod = mod;
            }catch(Exception e){
                Log.err("Failed to load mod map file '@'!", file);
                Log.err(e);
            }
        });
    }

    public void reload(){
        for(Map map : maps){
            if(map.texture != null){
                map.texture.dispose();
                map.texture = null;
            }
        }
        maps.clear();
        load();
    }

    /**
     * Save a custom map to the directory. This updates all values and stored data necessary.
     * The tags are copied to prevent mutation later.
     */
    public Map saveMap(ObjectMap<String, String> baseTags){

        try{
            StringMap tags = new StringMap(baseTags);
            String name = tags.get("name");
            if(name == null) throw new IllegalArgumentException("Can't save a map with no name. How did this happen?");
            Fi file;

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
                //reset attributes
                map.teams.clear();
                map.spawns = 0;

                for(int x = 0; x < map.width; x++){
                    for(int y = 0; y < map.height; y++){
                        Tile tile = world.rawTile(x, y);

                        if(tile.block() instanceof CoreBlock){
                            map.teams.add(tile.getTeamID());
                        }

                        if(tile.overlay() == Blocks.spawn){
                            map.spawns ++;
                        }
                    }
                }

                if(Core.assets.isLoaded(map.previewFile().path() + "." + mapExtension)){
                    Core.assets.unload(map.previewFile().path() + "." + mapExtension);
                }

                Pixmap pix = MapIO.generatePreview(world.tiles);
                executor.submit(() -> map.previewFile().writePNG(pix));
                writeCache(map);

                map.texture = new Texture(pix);
            }
            maps.add(map);
            maps.sort();

            return map;

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Import a map, then save it. This updates all values and stored data necessary. */
    public void importMap(Fi file) throws IOException{
        Fi dest = findFile();
        file.copyTo(dest);

        Map map = loadMap(dest, true);
        Exception[] error = {null};

        createNewPreview(map, e -> {
            maps.remove(map);
            try{
                map.file.delete();
            }catch(Throwable ignored){

            }
            error[0] = e;
        });

        if(error[0] != null){
            throw new IOException(error[0]);
        }
    }

    /** Attempts to run the following code;
     * catches any errors and attempts to display them in a readable way.*/
    public void tryCatchMapError(UnsafeRunnable run){
        try{
            run.run();
        }catch(Throwable e){
            Log.err(e);

            if("Outdated legacy map format".equals(e.getMessage())){
                ui.showErrorMessage("$editor.errornot");
            }else if(e.getMessage() != null && e.getMessage().contains("Incorrect header!")){
                ui.showErrorMessage("$editor.errorheader");
            }else{
                ui.showException("$editor.errorload", e);
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

    /** Reads JSON of filters, returning a new default array if not found.*/
    @SuppressWarnings("unchecked")
    public Array<GenerateFilter> readFilters(String str){
        if(str == null || str.isEmpty()){
            //create default filters list
            Array<GenerateFilter> filters =  Array.with(
                new ScatterFilter(){{
                    flooronto = Blocks.stone;
                    block = Blocks.rock;
                }},
                new ScatterFilter(){{
                    flooronto = Blocks.shale;
                    block = Blocks.shaleBoulder;
                }},
                new ScatterFilter(){{
                    flooronto = Blocks.snow;
                    block = Blocks.snowrock;
                }},
                new ScatterFilter(){{
                    flooronto = Blocks.ice;
                    block = Blocks.snowrock;
                }},
                new ScatterFilter(){{
                    flooronto = Blocks.sand;
                    block = Blocks.sandBoulder;
                }}
            );

            addDefaultOres(filters);

            return filters;
        }else{
            try{
                return JsonIO.read(Array.class, str);
            }catch(Exception e){
                e.printStackTrace();
                return readFilters("");
            }
        }
    }

    public void addDefaultOres(Array<GenerateFilter> filters){
        Array<Block> ores = content.blocks().select(b -> b.isOverlay() && b.asFloor().oreDefault);
        for(Block block : ores){
            OreFilter filter = new OreFilter();
            filter.threshold = block.asFloor().oreThreshold;
            filter.scl = block.asFloor().oreScale;
            filter.ore = block;
            filters.add(filter);
        }
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

    public void loadPreviews(){

        for(Map map : maps){
            //try to load preview
            if(map.previewFile().exists()){
                //this may fail, but calls queueNewPreview
                Core.assets.load(new AssetDescriptor<>(map.previewFile().path() + "." + mapExtension, Texture.class, new MapPreviewParameter(map))).loaded = t -> map.texture = (Texture)t;

                try{
                    readCache(map);
                }catch(Exception e){
                    e.printStackTrace();
                    queueNewPreview(map);
                }
            }else{
                queueNewPreview(map);
            }
        }
    }

    private void createAllPreviews(){
        Core.app.post(() -> {
            for(Map map : previewList){
                createNewPreview(map, e -> Core.app.post(() -> map.texture = Core.assets.get("sprites/error.png")));
            }
            previewList.clear();
        });
    }

    public void queueNewPreview(Map map){
        Core.app.post(() -> previewList.add(map));
    }

    private void createNewPreview(Map map, Cons<Exception> failed){
        try{
            //if it's here, then the preview failed to load or doesn't exist, make it
            //this has to be done synchronously!
            Pixmap pix = MapIO.generatePreview(map);
            map.texture = new Texture(pix);
            executor.submit(() -> {
                try{
                    map.previewFile().writePNG(pix);
                    writeCache(map);
                }catch(Exception e){
                    e.printStackTrace();
                }
            });
        }catch(Exception e){
            failed.get(e);
            Log.err("Failed to generate preview!", e);
        }
    }

    private void writeCache(Map map) throws IOException{
        try(DataOutputStream stream = new DataOutputStream(map.cacheFile().write(false, Streams.DEFAULT_BUFFER_SIZE))){
            stream.write(0);
            stream.writeInt(map.spawns);
            stream.write(map.teams.size);
            IntSetIterator iter = map.teams.iterator();
            while(iter.hasNext){
                stream.write(iter.next());
            }
        }
    }

    private void readCache(Map map) throws IOException{
        try(DataInputStream stream = new DataInputStream(map.cacheFile().read(Streams.DEFAULT_BUFFER_SIZE))){
            stream.read(); //version
            map.spawns = stream.readInt();
            int teamsize = stream.readByte();
            for(int i = 0; i < teamsize; i++){
                map.teams.add(stream.read());
            }
        }
    }

    /** Find a new filename to put a map to. */
    private Fi findFile(){
        //find a map name that isn't used.
        int i = maps.size;
        while(customMapDirectory.child("map_" + i + "." + mapExtension).exists()){
            i++;
        }
        return customMapDirectory.child("map_" + i + "." + mapExtension);
    }

    private Map loadMap(Fi file, boolean custom) throws IOException{
        Map map = MapIO.createMap(file, custom);

        if(map.name() == null){
            throw new IOException("Map name cannot be empty! File: " + file);
        }

        maps.add(map);
        maps.sort();
        return map;
    }

    public interface MapProvider{
        @Nullable Map next(@Nullable Map previous);
    }

    public enum ShuffleMode implements MapProvider{
        none(map -> null),
        all(prev -> {
            Array<Map> maps = Array.withArrays(Vars.maps.defaultMaps(), Vars.maps.customMaps());
            maps.shuffle();
            return maps.find(m -> m != prev || maps.size == 1);
        }),
        custom(prev -> {
            Array<Map> maps = Array.withArrays(Vars.maps.customMaps().isEmpty() ? Vars.maps.defaultMaps() : Vars.maps.customMaps());
            maps.shuffle();
            return maps.find(m -> m != prev || maps.size == 1);
        }),
        builtin(prev -> {
            Array<Map> maps = Array.withArrays(Vars.maps.defaultMaps());
            maps.shuffle();
            return maps.find(m -> m != prev || maps.size == 1);
        });

        private final MapProvider provider;

        ShuffleMode(MapProvider provider){
            this.provider = provider;
        }

        @Override
        public Map next(@Nullable Map previous){
            return provider.next(previous);
        }
    }
}
