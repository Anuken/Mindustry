package mindustry.core;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.Geometry.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;

import static mindustry.Vars.*;

public class World{
    public final Context context = new Context();

    public Tiles tiles = new Tiles(0, 0);
    /** The number of times tiles have changed in this session. Used for blocks that need to poll world state, but not frequently. */
    public int tileChanges = -1;

    private boolean generating, invalidMap;
    private ObjectMap<Map, Runnable> customMapLoaders = new ObjectMap<>();

    public World(){
        Events.on(TileChangeEvent.class, e -> {
            tileChanges ++;
        });

        Events.on(WorldLoadEvent.class, e -> {
            tileChanges = -1;
        });
    }

    /** Adds a custom handler function for loading a custom map - usually a generated one. */
    public void addMapLoader(Map map, Runnable loader){
        customMapLoaders.put(map, loader);
    }

    public boolean isInvalidMap(){
        return invalidMap;
    }

    public boolean solid(int x, int y){
        Tile tile = tile(x, y);

        return tile == null || tile.solid();
    }

    public boolean passable(int x, int y){
        Tile tile = tile(x, y);

        return tile != null && tile.passable();
    }

    public boolean wallSolid(int x, int y){
        Tile tile = tile(x, y);
        return tile == null || tile.block().solid;
    }

    public boolean wallSolidFull(int x, int y){
        Tile tile = tile(x, y);
        return tile == null || (tile.block().solid && tile.block().fillsTile);
    }

    public boolean isAccessible(int x, int y){
        return !wallSolid(x, y - 1) || !wallSolid(x, y + 1) || !wallSolid(x - 1, y) || !wallSolid(x + 1, y);
    }

    public int width(){
        return tiles.width;
    }

    public int height(){
        return tiles.height;
    }

    public int unitWidth(){
        return width()*tilesize;
    }

    public int unitHeight(){
        return height()*tilesize;
    }

    public Floor floor(int x, int y){
        Tile tile = tile(x, y);
        return tile == null ? Blocks.air.asFloor() : tile.floor();
    }

    public Floor floorWorld(float x, float y){
        Tile tile = tileWorld(x, y);
        return tile == null ? Blocks.air.asFloor() : tile.floor();
    }

    @Nullable
    public Tile tile(int pos){
        return tile(Point2.x(pos), Point2.y(pos));
    }

    @Nullable
    public Tile tile(int x, int y){
        return tiles.get(x, y);
    }

    @Nullable
    public Tile tileBuilding(int x, int y){
        Tile tile = tiles.get(x, y);
        if(tile == null) return null;
        if(tile.build != null){
            return tile.build.tile();
        }
        return tile;
    }

    @Nullable
    public Building build(int x, int y){
        Tile tile = tile(x, y);
        if(tile == null) return null;
        return tile.build;
    }

    @Nullable
    public Building build(int pos){
        Tile tile = tile(pos);
        if(tile == null) return null;
        return tile.build;
    }

    public Tile rawTile(int x, int y){
        return tiles.getn(x, y);
    }

    @Nullable
    public Tile tileWorld(float x, float y){
        return tile(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    @Nullable
    public Building buildWorld(float x, float y){
        return build(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    /** Convert from world to logic tile coordinates. Whole numbers are at centers of tiles. */
    public static float conv(float coord){
        return coord / tilesize;
    }

    /** Convert from tile to world coordinates. */
    public static float unconv(float coord){
        return coord * tilesize;
    }

    public static int toTile(float coord){
        return Math.round(coord / tilesize);
    }

    public int packArray(int x, int y){
        return x + y * tiles.width;
    }

    public void clearBuildings(){
        for(Tile tile : tiles){
            if(tile != null && tile.build != null){
                tile.build.remove();
            }
        }
    }

    /**
     * Resizes the tile array to the specified size and returns the resulting tile array.
     * Only use for loading saves!
     */
    public Tiles resize(int width, int height){
        clearBuildings();

        if(tiles.width != width || tiles.height != height){
            tiles = new Tiles(width, height);
        }

        return tiles;
    }

    /**
     * Call to signify the beginning of map loading.
     * TileEvents will not be fired until endMapLoad().
     */
    public void beginMapLoad(){
        generating = true;
    }

    /**
     * Call to signify the end of map loading. Updates tile proximities and sets up physics for the world.
     * A WorldLoadEvent will be fire.
     */
    public void endMapLoad(){

        for(Tile tile : tiles){
            //remove legacy blocks; they need to stop existing
            if(tile.block() instanceof LegacyBlock l){
                l.removeSelf(tile);
                continue;
            }

            if(tile.build != null){
                tile.build.updateProximity();
            }
        }

        addDarkness(tiles);

        Groups.resize(-finalWorldBounds, -finalWorldBounds, tiles.width * tilesize + finalWorldBounds * 2, tiles.height * tilesize + finalWorldBounds * 2);

        generating = false;
        Events.fire(new WorldLoadEvent());
    }

    public Rect getQuadBounds(Rect in){
        return in.set(-finalWorldBounds, -finalWorldBounds, width() * tilesize + finalWorldBounds * 2, height() * tilesize + finalWorldBounds * 2);
    }

    public void setGenerating(boolean gen){
        this.generating = gen;
    }

    public boolean isGenerating(){
        return generating;
    }

    public void loadGenerator(int width, int height, Cons<Tiles> generator){
        beginMapLoad();

        resize(width, height);
        generator.get(tiles);

        endMapLoad();
    }

    public void loadSector(Sector sector){
        loadSector(sector, 0, true);
    }

    public void loadSector(Sector sector, int seedOffset, boolean saveInfo){
        setSectorRules(sector, saveInfo);

        int size = sector.getSize();
        loadGenerator(size, size, tiles -> {
            if(sector.preset != null){
                sector.preset.generator.generate(tiles);
                sector.preset.rules.get(state.rules); //apply extra rules
            }else if(sector.planet.generator != null){
                sector.planet.generator.generate(tiles, sector, seedOffset);
            }else{
                throw new RuntimeException("Sector " + sector.id + " on planet " + sector.planet.name + " has no generator or preset defined. Provide a planet generator or preset map.");
            }
            //just in case
            state.rules.sector = sector;
        });

        //postgenerate for bases
        if(sector.preset == null && sector.planet.generator != null){
            sector.planet.generator.postGenerate(tiles);
        }

        //reset rules
        setSectorRules(sector, saveInfo);

        if(state.rules.defaultTeam.core() != null){
            sector.info.spawnPosition = state.rules.defaultTeam.core().pos();
        }
    }

    private void setSectorRules(Sector sector, boolean saveInfo){
        state.map = new Map(StringMap.of("name", sector.preset == null ? sector.planet.localizedName + "; Sector " + sector.id : sector.preset.localizedName));
        state.rules.sector = sector;
        state.rules.weather.clear();

        sector.planet.generator.addWeather(sector, state.rules);

        ObjectSet<UnlockableContent> content = new ObjectSet<>();

        //TODO duplicate code?
        for(Tile tile : tiles){
            if(getDarkness(tile.x, tile.y) >= 3){
                continue;
            }

            Liquid liquid = tile.floor().liquidDrop;
            if(tile.floor().itemDrop != null) content.add(tile.floor().itemDrop);
            if(tile.overlay().itemDrop != null) content.add(tile.overlay().itemDrop);
            if(tile.wallDrop() != null) content.add(tile.wallDrop());
            if(liquid != null) content.add(liquid);
        }

        state.rules.cloudColor = sector.planet.landCloudColor;
        state.rules.env = sector.planet.defaultEnv;
        state.rules.hiddenBuildItems.clear();
        state.rules.hiddenBuildItems.addAll(sector.planet.hiddenItems);
        sector.planet.applyRules(state.rules);
        sector.info.resources = content.toSeq();
        sector.info.resources.sort(Structs.comps(Structs.comparing(Content::getContentType), Structs.comparingInt(c -> c.id)));
        if(saveInfo){
            sector.saveInfo();
        }
    }

    public Context filterContext(Map map){
        return new FilterContext(map);
    }

    public void loadMap(Map map){
        loadMap(map, new Rules());
    }

    public void loadMap(Map map, Rules checkRules){
        //load using custom loader if possible
        if(customMapLoaders.containsKey(map)){
            customMapLoaders.get(map).run();
            return;
        }

        try{
            SaveIO.load(map.file, new FilterContext(map));
        }catch(Throwable e){
            Log.err(e);
            if(!headless){
                ui.showErrorMessage("@map.invalid");
                Core.app.post(() -> state.set(State.menu));
                invalidMap = true;
            }
            generating = false;
            return;
        }

        state.map = map;

        invalidMap = false;

        if(!headless){
            if(state.teams.cores(checkRules.defaultTeam).size == 0 && !checkRules.pvp){
                ui.showErrorMessage(Core.bundle.format("map.nospawn", checkRules.defaultTeam.color, checkRules.defaultTeam.localized()));
                invalidMap = true;
            }else if(checkRules.pvp){ //pvp maps need two cores to be valid
                if(state.teams.getActive().count(TeamData::hasCore) < 2){
                    invalidMap = true;
                    ui.showErrorMessage("@map.nospawn.pvp");
                }
            }else if(checkRules.attackMode){ //attack maps need two cores to be valid
                invalidMap = state.rules.waveTeam.data().noCores();
                if(invalidMap){
                    ui.showErrorMessage(Core.bundle.format("map.nospawn.attack", checkRules.waveTeam.color, checkRules.waveTeam.localized()));
                }
            }
        }else{
            invalidMap = !state.teams.getActive().contains(TeamData::hasCore);

            if(invalidMap){
                throw new MapException(map, "Map has no cores!");
            }
        }

        if(invalidMap) Core.app.post(() -> state.set(State.menu));
    }

    public void addDarkness(Tiles tiles){
        byte[] dark = new byte[tiles.width * tiles.height];
        byte[] writeBuffer = new byte[tiles.width * tiles.height];

        byte darkIterations = darkRadius;

        for(int i = 0; i < dark.length; i++){
            Tile tile = tiles.geti(i);
            if(tile.isDarkened()){
                dark[i] = darkIterations;
            }
        }

        for(int i = 0; i < darkIterations; i++){
            for(Tile tile : tiles){
                int idx = tile.y * tiles.width + tile.x;
                boolean min = false;
                for(Point2 point : Geometry.d4){
                    int newX = tile.x + point.x, newY = tile.y + point.y;
                    int nidx = newY * tiles.width + newX;
                    if(tiles.in(newX, newY) && dark[nidx] < dark[idx]){
                        min = true;
                        break;
                    }
                }
                writeBuffer[idx] = (byte)Math.max(0, dark[idx] - Mathf.num(min));
            }

            System.arraycopy(writeBuffer, 0, dark, 0, writeBuffer.length);
        }

        for(Tile tile : tiles){
            int idx = tile.y * tiles.width + tile.x;

            if(tile.isDarkened()){
                tile.data = dark[idx];
            }

            if(dark[idx] == darkRadius){
                boolean full = true;
                for(Point2 p : Geometry.d4){
                    int px = p.x + tile.x, py = p.y + tile.y;
                    int nidx = py * tiles.width + px;
                    if(tiles.in(px, py) && !(tile.isDarkened() && dark[nidx] == 4)){
                        full = false;
                        break;
                    }
                }

                if(full) tile.data = darkRadius + 1;
            }
        }
    }

    public byte getWallDarkness(Tile tile){
        if(tile.isDarkened()){
            int minDst = darkRadius + 1;
            for(int cx = tile.x - darkRadius; cx <= tile.x + darkRadius; cx++){
                for(int cy = tile.y - darkRadius; cy <= tile.y + darkRadius; cy++){
                    if(tiles.in(cx, cy) && !rawTile(cx, cy).isDarkened()){
                        minDst = Math.min(minDst, Math.abs(cx - tile.x) + Math.abs(cy - tile.y));
                    }
                }
            }

            return (byte)Math.max((minDst - 1), 0);
        }
        return 0;
    }

    public void checkMapArea(){
        for(var build : Groups.build){
            //reset map-area-based disabled blocks.
            if(build.allowUpdate() && !build.enabled){
                build.enabled = true;
            }
        }
    }

    //TODO optimize; this is very slow and called too often!
    public float getDarkness(int x, int y){
        float dark = 0;

        if(Vars.state.rules.borderDarkness){
            int edgeBlend = 2;
            int edgeDst;

            if(!state.rules.limitMapArea){
                edgeDst = Math.min(x, Math.min(y, Math.min(-(x - (tiles.width - 1)), -(y - (tiles.height - 1)))));
            }else{
                edgeDst =
                    Math.min(x - state.rules.limitX,
                    Math.min(y - state.rules.limitY,
                    Math.min(-(x - (state.rules.limitX + state.rules.limitWidth - 1)), -(y - (state.rules.limitY + state.rules.limitHeight - 1)))));
            }

            if(edgeDst <= edgeBlend){
                dark = Math.max((edgeBlend - edgeDst) * (4f / edgeBlend), dark);
            }
        }

        if(state.hasSector() && state.getSector().preset == null){
            int circleBlend = 5;
            //quantized angle
            float offset = state.getSector().rect.rotation + 90;
            float angle = Angles.angle(x, y, tiles.width/2, tiles.height/2) + offset;
            //polygon sides, depends on sector
            int sides = state.getSector().tile.corners.length;
            float step = 360f / sides;
            //prev and next angles of poly
            float prev = Mathf.round(angle, step);
            float next = prev + step;
            //raw line length to be translated
            float length = state.getSector().getSize()/2f;
            float rawDst = Intersector.distanceLinePoint(Tmp.v1.trns(prev, length), Tmp.v2.trns(next, length), Tmp.v3.set(x - tiles.width/2, y - tiles.height/2).rotate(offset)) / Mathf.sqrt3 - 1;

            //noise
            rawDst += Noise.noise(x, y, 11f, 7f) + Noise.noise(x, y, 22f, 15f);

            int circleDst = (int)(rawDst - (length - circleBlend));
            if(circleDst > 0){
                dark = Math.max(circleDst, dark);
            }
        }

        Tile tile = tile(x, y);
        if(tile != null && tile.isDarkened()){
            dark = Math.max(dark, tile.data);
        }

        return dark;
    }

    public static void raycastEachWorld(float x0, float y0, float x1, float y1, Raycaster cons){
        raycastEach(toTile(x0), toTile(y0), toTile(x1), toTile(y1), cons);
    }

    public static void raycastEach(int x1, int y1, int x2, int y2, Raycaster cons){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(true){
            if(cons.accept(x, y)) break;
            if(x == x2 && y == y2) break;

            e2 = 2 * err;
            if(e2 > -dy){
                err -= dy;
                x += sx;
            }

            if(e2 < dx){
                err += dx;
                y += sy;
            }
        }
    }

    public static boolean raycast(int x1, int y1, int x2, int y2, Raycaster cons){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(true){
            if(cons.accept(x, y)) return true;
            if(x == x2 && y == y2) return false;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x = x + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y = y + sy;
            }
        }
    }

    private class Context implements WorldContext{

        Context(){}

        @Override
        public Tile tile(int index){
            return tiles.geti(index);
        }

        @Override
        public void resize(int width, int height){
            World.this.resize(width, height);
        }

        @Override
        public Tile create(int x, int y, int floorID, int overlayID, int wallID){
            Tile tile = new Tile(x, y, floorID, overlayID, wallID);
            tiles.set(x, y, tile);
            return tile;
        }

        @Override
        public boolean isGenerating(){
            return World.this.isGenerating();
        }

        @Override
        public void begin(){
            beginMapLoad();
        }

        @Override
        public void end(){
            endMapLoad();
        }
    }

    /** World context that applies filters after generation end. */
    public class FilterContext extends Context{
        final Map map;

        public FilterContext(Map map){
            this.map = map;
        }

        @Override
        public void end(){
            applyFilters();

            super.end();
        }

        public void applyFilters(){
            Seq<GenerateFilter> filters = map.filters();

            if(!filters.isEmpty()){
                //input for filter queries
                GenerateInput input = new GenerateInput();

                for(GenerateFilter filter : filters){
                    filter.randomize();
                    input.begin(width(), height(), (x, y) -> tiles.getn(x, y));
                    filter.apply(tiles, input);
                }
            }
        }
    }
}
