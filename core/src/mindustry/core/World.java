package mindustry.core;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.type.Weather.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;

import static mindustry.Vars.*;

public class World{
    public final Context context = new Context();

    public @NonNull Tiles tiles = new Tiles(0, 0);

    private boolean generating, invalidMap;

    public World(){

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

    @NonNull
    public Floor floor(int x, int y){
        Tile tile = tile(x, y);
        return tile == null ? Blocks.air.asFloor() : tile.floor();
    }

    @NonNull
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
        if(tile.build != null) return tile.build.tile();
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

    @NonNull
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

    public int toTile(float coord){
        return Math.round(coord / tilesize);
    }

    private void clearTileEntities(){
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
        clearTileEntities();

        if(tiles.width != width || tiles.height != height){
            tiles = new Tiles(width, height);
        }

        return tiles;
    }

    /**
     * Call to signify the beginning of map loading.
     * BuildinghangeEvents will not be fired until endMapLoad().
     */
    public void beginMapLoad(){
        generating = true;
    }

    /**
     * Call to signify the end of map loading. Updates tile occlusions and sets up physics for the world.
     * A WorldLoadEvent will be fire.
     */
    public void endMapLoad(){

        for(Tile tile : tiles){
            //remove legacy blocks; they need to stop existing
            if(tile.block() instanceof LegacyBlock){
                tile.remove();
                continue;
            }

            tile.updateOcclusion();

            if(tile.build != null){
                tile.build.updateProximity();
            }
        }

        if(!headless){
            addDarkness(tiles);
        }

        Groups.resize(-finalWorldBounds, -finalWorldBounds, tiles.width * tilesize + finalWorldBounds * 2, tiles.height * tilesize + finalWorldBounds * 2);

        generating = false;
        Events.fire(new WorldLoadEvent());
    }

    public Rect getQuadBounds(Rect in){
        return in.set(-finalWorldBounds, -finalWorldBounds, world.width() * tilesize + finalWorldBounds * 2, world.height() * tilesize + finalWorldBounds * 2);
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
        setSectorRules(sector);

        int size = sector.getSize();
        loadGenerator(size, size, tiles -> {
            if(sector.preset != null){
                sector.preset.generator.generate(tiles);
                sector.preset.rules.get(state.rules); //apply extra rules
            }else{
                sector.planet.generator.generate(tiles, sector);
            }
            //just in case
            state.rules.sector = sector;
        });

        //postgenerate for bases
        if(sector.preset == null){
            sector.planet.generator.postGenerate(tiles);
        }

        //reset rules
        setSectorRules(sector);

        if(state.rules.defaultTeam.core() != null){
            sector.setSpawnPosition(state.rules.defaultTeam.core().pos());
        }
    }

    private void setSectorRules(Sector sector){
        state.map = new Map(StringMap.of("name", sector.planet.localizedName + "; Sector " + sector.id));
        state.rules.sector = sector;

        state.rules.weather.clear();

        if(sector.is(SectorAttribute.rainy)) state.rules.weather.add(new WeatherEntry(Weathers.rain));
        if(sector.is(SectorAttribute.snowy)) state.rules.weather.add(new WeatherEntry(Weathers.snow));
        if(sector.is(SectorAttribute.desert)) state.rules.weather.add(new WeatherEntry(Weathers.sandstorm));

    }

    public Context filterContext(Map map){
        return new FilterContext(map);
    }

    public void loadMap(Map map){
        loadMap(map, new Rules());
    }

    public void loadMap(Map map, Rules checkRules){
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
            if(state.teams.playerCores().size == 0 && !checkRules.pvp){
                ui.showErrorMessage("@map.nospawn");
                invalidMap = true;
            }else if(checkRules.pvp){ //pvp maps need two cores to be valid
                if(state.teams.getActive().count(TeamData::hasCore) < 2){
                    invalidMap = true;
                    ui.showErrorMessage("@map.nospawn.pvp");
                }
            }else if(checkRules.attackMode){ //attack maps need two cores to be valid
                invalidMap = state.teams.get(state.rules.waveTeam).noCores();
                if(invalidMap){
                    ui.showErrorMessage("@map.nospawn.attack");
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

    public void notifyChanged(Tile tile){
        if(!generating){
            Core.app.post(() -> Events.fire(new BuildinghangeEvent(tile)));
        }
    }

    public void raycastEachWorld(float x0, float y0, float x1, float y1, Raycaster cons){
        raycastEach(toTile(x0), toTile(y0), toTile(x1), toTile(y1), cons);
    }

    public void raycastEach(int x0f, int y0f, int x1, int y1, Raycaster cons){
        int x0 = x0f;
        int y0 = y0f;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;
        while(true){

            if(cons.accept(x0, y0)) break;
            if(x0 == x1 && y0 == y1) break;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x0 = x0 + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y0 = y0 + sy;
            }
        }
    }

    public boolean raycast(int x0f, int y0f, int x1, int y1, Raycaster cons){
        int x0 = x0f;
        int y0 = y0f;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;
        while(true){

            if(cons.accept(x0, y0)) return true;
            if(x0 == x1 && y0 == y1) return false;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x0 = x0 + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y0 = y0 + sy;
            }
        }
    }

    public void addDarkness(Tiles tiles){
        byte[] dark = new byte[tiles.width * tiles.height];
        byte[] writeBuffer = new byte[tiles.width * tiles.height];

        byte darkIterations = 4;

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

            if(dark[idx] == 4){
                boolean full = true;
                for(Point2 p : Geometry.d4){
                    int px = p.x + tile.x, py = p.y + tile.y;
                    int nidx = py * tiles.width + px;
                    if(tiles.in(px, py) && !(tile.isDarkened() && dark[nidx] == 4)){
                        full = false;
                        break;
                    }
                }

                if(full) tile.data = 5;
            }
        }
    }

    public float getDarkness(int x, int y){
        int edgeBlend = 2;

        float dark = 0;
        int edgeDst = Math.min(x, Math.min(y, Math.min(Math.abs(x - (tiles.width - 1)), Math.abs(y - (tiles.height - 1)))));
        if(edgeDst <= edgeBlend){
            dark = Math.max((edgeBlend - edgeDst) * (4f / edgeBlend), dark);
        }

        if(state.hasSector()){
            int circleBlend = 14;
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
                dark = Math.max(circleDst / 1f, dark);
            }
        }

        Tile tile = world.tile(x, y);
        if(tile != null && tile.block().solid && tile.block().fillsTile && !tile.block().synthetic()){
            dark = Math.max(dark, tile.data);
        }

        return dark;
    }

    public interface Raycaster{
        boolean accept(int x, int y);
    }

    private class Context implements WorldContext{

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
    private class FilterContext extends Context{
        final Map map;

        FilterContext(Map map){
            this.map = map;
        }

        @Override
        public void end(){
            Seq<GenerateFilter> filters = map.filters();

            if(!filters.isEmpty()){
                //input for filter queries
                GenerateInput input = new GenerateInput();

                for(GenerateFilter filter : filters){
                    input.begin(filter, width(), height(), (x, y) -> tiles.getn(x, y));
                    filter.apply(tiles, input);
                }
            }

            super.end();
        }
    }
}
