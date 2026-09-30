package mindustry.core;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.game.markers.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.legacy.*;

import static mindustry.Vars.*;

public class GameState{
    /** Current wave number, can be anything in non-wave modes. */
    public int wave = 1;
    /** Wave countdown in ticks. */
    public float wavetime;
    /** Logic tick. */
    public double tick;
    /** Continuously ticks up every non-paused update. */
    public long updateId;
    /** Whether the game is in game over state. */
    public boolean gameOver = false;
    /** For the campaign, this is whether the map is in a "after game over" state. In this state, the game is always paused. */
    public boolean afterGameOver = false;
    /** Whether the player's team won the match. */
    public boolean won = false;
    /** Server ticks/second. Only valid in multiplayer. */
    public int serverTps = -1;
    /** Map that is currently being played on. */
    public Map map = emptyMap;
    /** The current game rules. */
    public Rules rules = new Rules();
    /** Statistics for this save/game. Displayed after game over. */
    public GameStats stats = new GameStats();
    /** Markers not linked to objectives. Controlled by world processors. */
    public MapMarkers markers = new MapMarkers();
    /** Locale-specific string bundles of current map */
    public MapLocales mapLocales = new MapLocales();
    /** Global attributes of the environment, calculated by weather. */
    public Attributes envAttrs = new Attributes();
    /** Team data. Gets reset every new game. */
    public Teams teams = new Teams();
    /** Handles JSON edits of game content. */
    public DataManager data = new DataManager();
    /** Tile data. */
    public World world = new World();
    /** Indexes spawn positions. */
    public WaveSpawner spawner = new WaveSpawner();
    /** All entity groups. */
    public Entities entities = new Entities();
    /** Updates and stores fog of war state. */
    public FogControl fog = new FogControl(this);
    /** Flowfield pathfinding. */
    public Pathfinder pathfinder = new Pathfinder(this);
    /** Cluster-based pathfinding for player-controlled units and RTS AI. */
    public ControlPathfinder controlPath = new ControlPathfinder(this);
    /** Indexes block info for faster access. */
    public BlockIndexer indexer = new BlockIndexer();
    /** If true, the world is currently being generated/loaded; tile change events do not fire. */
    public boolean generating;
    /** Number of enemies in the game; only used clientside in servers. */
    public int enemies;
    /** Map being playtested (not edited!) */
    public @Nullable Map playtestingMap;
    /** Current game state. */
    private State state = State.menu;

    @Nullable
    public Unit boss(){
        return teams.bosses.firstOpt();
    }

    public void set(State astate){
        //nothing to change.
        if(state == astate) return;

        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    public boolean hasSpawns(){
        return rules.waves && ((rules.waveTeam.cores().size > 0 && rules.attackMode) || rules.spawns.size > 0);
    }

    /** Note that being in a campaign does not necessarily mean having a sector. */
    public boolean isCampaign(){
        return rules.sector != null;
    }

    public boolean hasSector(){
        return rules.sector != null;
    }

    public @Nullable Sector getSector(){
        return rules.sector;
    }

    public @Nullable Planet getPlanet(){
        return rules.sector != null ? rules.sector.planet : rules.planet;
    }

    public boolean isEditor(){
        return rules.editor;
    }

    public boolean isPaused(){
        return state == State.paused;
    }

    /** @return whether there is an unpaused game in progress. */
    public boolean isPlaying(){
        return state == State.playing;
    }

    /** @return whether the current state is *not* the menu. */
    public boolean isGame(){
        return state != State.menu;
    }

    public boolean isMenu(){
        return state == State.menu;
    }

    public boolean is(State astate){
        return state == astate;
    }

    public State getState(){
        return state;
    }

    public enum State{
        paused, playing, menu
    }


    /** Resizes the tile array to the specified size. Only use for loading saves! */
    public World resizeWorld(int width, int height){
        world.clearBuildings();
        world = new World(width, height);

        return world;
    }

    /**
     * Call to signify the beginning of map loading.
     * TileEvents will not be fired until endMapLoad().
     */
    public void beginMapLoad(){
        generating = true;
        Events.fire(new WorldLoadBeginEvent());
    }

    /**
     * Call to signify the end of map loading. Updates tile proximities and sets up physics for the world.
     * A WorldLoadEvent will be fire.
     */
    public void endMapLoad(){
        Events.fire(new WorldLoadEndEvent());

        for(Tile tile : world){
            //remove legacy blocks; they need to stop existing
            if(tile.block() instanceof LegacyBlock l){
                l.removeSelf(tile);
                continue;
            }

            if(tile.build != null){
                tile.build.updateProximity();
            }
        }

        world.applyDarkness();

        entities.resize(-finalWorldBounds, -finalWorldBounds, world.width * tilesize + finalWorldBounds * 2, world.height * tilesize + finalWorldBounds * 2);

        generating = false;
        world.tileChanges = -1;
        world.floorChanges = -1;
        spawner.load();
        indexer.load();
        pathfinder.load();
        fog.load();
        //new instance, so a still-running old thread can't touch the new world's data
        controlPath.stop();
        controlPath = new ControlPathfinder(this);
        controlPath.start();
        Events.fire(new WorldLoadEvent());
        for(var build : entities.build){
            build.checkAllowUpdate();
        }
    }

    public static void loadGenerator(int width, int height, Cons<World> generator){
        Vars.state.beginMapLoad();

        Vars.state.resizeWorld(width, height);
        generator.get(Vars.state.world);

        Vars.state.endMapLoad();
    }

    public static void loadSector(Sector sector, WorldParams params){
        Vars.state.setSectorRules(sector, params.saveInfo);

        int size = sector.getSize();
        loadGenerator(size, size, tiles -> {
            if(sector.preset != null){
                sector.preset.generator.generate(tiles, params);
                sector.preset.rules.get(Vars.state.rules); //apply extra rules
            }else if(sector.planet.generator != null){
                sector.planet.generator.generate(tiles, sector, params);
            }else{
                throw new RuntimeException("Sector " + sector.id + " on planet " + sector.planet.name + " has no generator or preset defined. Provide a planet generator or preset map.");
            }
            //just in case
            Vars.state.rules.sector = sector;
        });

        if(params.saveInfo && Vars.state.rules.waves){
            sector.info.waves = Vars.state.rules.waves;
        }

        //postgenerate for bases
        if(sector.preset == null && sector.planet.generator != null){
            sector.planet.generator.postGenerate(Vars.state.world);
        }

        //reset rules
        Vars.state.setSectorRules(sector, params.saveInfo);

        if(Vars.state.rules.defaultTeam.core() != null){
            sector.info.spawnPosition = Vars.state.rules.defaultTeam.core().pos();
        }
    }

    private void setSectorRules(Sector sector, boolean saveInfo){
        map = new Map(StringMap.of("name", sector.preset == null ? sector.planet.localizedName + "; Sector " + sector.id : sector.preset.localizedName));
        rules.sector = sector;

        sector.planet.generator.addWeather(sector, rules);

        ObjectSet<UnlockableContent> content = new ObjectSet<>();

        //resources can be outside area
        boolean border = rules.limitMapArea;
        rules.limitMapArea = false;

        for(Tile tile : world){
            if(world.getDarkness(tile.x, tile.y) >= 3){
                continue;
            }

            Liquid liquid = tile.floor().liquidDrop;
            if(tile.floor().itemDrop != null && !tile.block().isStatic()) content.add(tile.floor().itemDrop);
            if(tile.overlay().itemDrop != null && !tile.block().isStatic()) content.add(tile.overlay().itemDrop);
            if(tile.wallDrop() != null) content.add(tile.wallDrop());
            if(liquid != null && !tile.block().isStatic()) content.add(liquid);
        }
        rules.limitMapArea = border;

        rules.cloudColor = sector.planet.landCloudColor;
        rules.env = sector.planet.defaultEnv;
        rules.planet = sector.planet;
        sector.planet.applyRules(rules, !saveInfo);
        sector.info.resources = content.toSeq();
        sector.info.resources.sort(Structs.comps(Structs.comparing(Content::getContentType), Structs.comparingInt(c -> c.id)));

        if(saveInfo){
            sector.saveInfo();
        }
    }

    public static void loadMap(Map map) throws SaveLoadException{
        loadMap(map, new Rules());
    }

    /**
     * Loads a map file, throwing an exception if it is unplayable or cannot be loaded.
     * @param checkRules Rules to check map validity against (should set values like pvp, attack mode, etc)
     * */
    public static void loadMap(Map map, Rules checkRules) throws SaveLoadException{

        //load using custom loader if possible
        if(map.loadCustom()){
            return;
        }

        try{
            SaveIO.load(map.file, new FilterContext(map));
            Vars.state.map = map; //SaveIO resets gamestate which resets the map
        }catch(Throwable error){
            if(error instanceof SaveLoadException se){
                throw se;
            }else{
                throw new SaveLoadException(error);
            }
        }finally{
            Vars.state.generating = false;
        }

        if(Vars.state.teams.cores(checkRules.defaultTeam).size == 0 && !checkRules.pvp){
            //non-pvp: needs a core for the player team
            throw new SaveLoadException(headless ? "The player team does not have a core" : Core.bundle.format("map.nospawn", checkRules.defaultTeam.coloredName()));
        }else if(checkRules.pvp){
            //pvp: needs 2 active teams with cores
            if(Vars.state.teams.getActive().count(TeamData::hasCore) < 2){
                throw new SaveLoadException(headless ? "PvP is on, but there are fewer than 2 teams with a core" : Core.bundle.get("map.nospawn.pvp"));
            }
        }else if(checkRules.attackMode){
            //attack maps: need 2 cores to be valid
            if(Vars.state.rules.waveTeam.data().noCores()){
                throw new SaveLoadException(headless ? "Attack mode is on, but there is no enemy core" : Core.bundle.format("map.nospawn.attack", checkRules.waveTeam.coloredName()));
            }
        }
    }
}
