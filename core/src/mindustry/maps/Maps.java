package mindustry.maps;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.IntSet.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
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
    /** All generation filter types. */
    public static Prov<GenerateFilter>[] allFilterTypes = new Prov[]{
    NoiseFilter::new, ScatterFilter::new, TerrainFilter::new, DistortFilter::new,
    RiverNoiseFilter::new, OreFilter::new, OreMedianFilter::new, MedianFilter::new,
    BlendFilter::new, MirrorFilter::new, ClearFilter::new, CoreSpawnFilter::new,
    EnemySpawnFilter::new, SpawnPathFilter::new
    };

    /** List of all built-in maps. Filenames only. */
    private static String[] defaultMapNames = {"maze", "fortress", "labyrinth", "islands", "tendrils", "caldera", "wasteland", "shattered", "fork", "triad", "mudFlats", "moltenLake", "archipelago", "debrisField", "domain", "veins", "glacier", "passage"};
    /** Maps tagged as PvP */
    private static String[] pvpMaps = {"veins", "glacier", "passage"};

    /** All maps stored in an ordered array. */
    private Seq<Map> maps = new Seq<>();
    private ShuffleMode shuffleMode = ShuffleMode.all;
    private @Nullable MapProvider shuffler;

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
    public @Nullable Map getNextMap(Gamemode mode, @Nullable Map previous){
        if(shuffler != null) return shuffler.next(mode, previous);
        return shuffleMode.next(mode, previous);
    }

    /** Returns a list of all maps, including custom ones. */
    public Seq<Map> all(){
        return maps;
    }

    /** Returns a list of only custom maps. */
    public Seq<Map> customMaps(){
        return maps.select(m -> m.custom);
    }

    /** Returns a list of only default maps. */
    public Seq<Map> defaultMaps(){
        return maps.select(m -> !m.custom);
    }

    public Map byName(String name){
        return maps.find(m -> m.name().equals(name));
    }

    public Maps(){
        Events.on(ClientLoadEvent.class, event -> maps.sort());

        if(Core.assets != null){
            ((CustomLoader)Core.assets.getLoader(ContentLoader.class)).loaded = this::createAllPreviews;
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
                file = findFile(name);
            }

            //create map, write it, etc etc etc
            Map map = new Map(file, world.width(), world.height(), tags, true);
            fogControl.resetFog();
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
                mainExecutor.submit(() -> map.previewFile().writePng(pix));
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
        Fi dest = findFile(file.name());
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
                ui.showErrorMessage("@editor.errornot");
            }else if(e.getMessage() != null && e.getMessage().contains("Incorrect header!")){
                ui.showErrorMessage("@editor.errorheader");
            }else{
                ui.showException("@editor.errorload", e);
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
    public Seq<GenerateFilter> readFilters(String str){
        if(str == null || str.isEmpty()){
            //create default filters list

            Seq<GenerateFilter> filters = new Seq<>();

            for(Block block : content.blocks()){
                if(block.isFloor() && block.inEditor && block.asFloor().decoration != Blocks.air){
                    var filter = new ScatterFilter();
                    filter.flooronto = block.asFloor();
                    filter.block = block.asFloor().decoration;
                    filters.add(filter);
                }
            }

            addDefaultOres(filters);

            return filters;
        }else{
            try{
                return JsonIO.read(Seq.class, str);
            }catch(Throwable e){
                e.printStackTrace();
                return readFilters("");
            }
        }
    }

    public void addDefaultOres(Seq<GenerateFilter> filters){
        Seq<Block> ores = content.blocks().select(b -> b.isOverlay() && b.asFloor().oreDefault);
        for(Block block : ores){
            OreFilter filter = new OreFilter();
            filter.threshold = block.asFloor().oreThreshold;
            filter.scl = block.asFloor().oreScale;
            filter.ore = block;
            filters.add(filter);
        }
    }

    public String writeWaves(Seq<SpawnGroup> groups){
        if(groups == null) return "[]";

        StringWriter buffer = new StringWriter();
        JsonIO.json.setWriter(new JsonWriter(buffer));

        JsonIO.json.writeArrayStart();
        for(int i = 0; i < groups.size; i++){
            JsonIO.json.writeObjectStart(SpawnGroup.class, SpawnGroup.class);
            groups.get(i).write(JsonIO.json);
            JsonIO.json.writeObjectEnd();
        }
        JsonIO.json.writeArrayEnd();
        return buffer.toString();
    }

    public Seq<SpawnGroup> readWaves(String str){
        return str == null ? null : str.equals("[]") ? new Seq<>() : Seq.with(JsonIO.json.fromJson(SpawnGroup[].class, str));
    }

    public void loadPreviews(){

        for(Map map : maps){
            //try to load preview
            if(map.previewFile().exists()){
                //this may fail, but calls queueNewPreview
                Core.assets.load(new AssetDescriptor<>(map.previewFile().path() + "." + mapExtension, Texture.class, new MapPreviewParameter(map))).loaded = t -> map.texture = t;

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
            mainExecutor.submit(() -> {
                try{
                    map.previewFile().writePng(pix);
                    writeCache(map);
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    pix.dispose();
                }
            });
        }catch(Exception e){
            failed.get(e);
            Log.err("Failed to generate preview!", e);
        }
    }

    private void writeCache(Map map) throws IOException{
        try(DataOutputStream stream = new DataOutputStream(map.cacheFile().write(false, Streams.defaultBufferSize))){
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
        try(DataInputStream stream = new DataInputStream(map.cacheFile().read(Streams.defaultBufferSize))){
            stream.read(); //version
            map.spawns = stream.readInt();
            int teamsize = stream.readByte();
            for(int i = 0; i < teamsize; i++){
                map.teams.add(stream.read());
            }
        }
    }

    /** Find a new filename to put a map to. */
    private Fi findFile(String unsanitizedName){
        String name = Strings.sanitizeFilename(unsanitizedName);
        if(name.isEmpty()) name = "blank";

        Fi result = null;
        int index = 0;

        while(result == null || result.exists()){
            result = customMapDirectory.child(name + (index == 0 ? "" : "_" + index) + "." + mapExtension);
            index ++;
        }

        return result;
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
        @Nullable Map next(Gamemode mode, @Nullable Map previous);
    }

    public enum ShuffleMode implements MapProvider{
        none((mode, map) -> null),
        all((mode, prev) -> next(mode, prev, Vars.maps.defaultMaps(), Vars.maps.customMaps())),
        custom((mode, prev) -> next(mode, prev, Vars.maps.customMaps().isEmpty() ? Vars.maps.defaultMaps() : Vars.maps.customMaps())),
        builtin((mode, prev) -> next(mode, prev, Vars.maps.defaultMaps()));

        private final MapProvider provider;

        ShuffleMode(MapProvider provider){
            this.provider = provider;
        }

        @SafeVarargs
        private static Map next(Gamemode mode, Map prev, Seq<Map>... mapArray){
            Seq<Map> maps = Seq.withArrays((Object[])mapArray);
            maps.shuffle();

            return maps.find(m -> (m != prev || maps.size == 1) && valid(mode, m));
        }

        private static boolean valid(Gamemode mode, Map map){
            boolean pvp = !map.custom && Structs.contains(pvpMaps, map.file.nameWithoutExtension());
            if(mode == Gamemode.survival || mode == Gamemode.attack || mode == Gamemode.sandbox) return !pvp;
            if(mode == Gamemode.pvp) return map.custom || pvp;
            return true;
        }

        @Override
        public Map next(Gamemode mode, @Nullable Map previous){
            return provider.next(mode, previous);
        }
    }
}
