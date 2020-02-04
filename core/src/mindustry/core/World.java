package mindustry.core;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class World{
    public final Context context = new Context();

    private Map currentMap;
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

    public Map getMap(){
        return currentMap;
    }

    public void setMap(Map map){
        this.currentMap = map;
    }

    public int width(){
        return tiles.width();
    }

    public int height(){
        return tiles.height();
    }

    public int unitWidth(){
        return width()*tilesize;
    }

    public int unitHeight(){
        return height()*tilesize;
    }

    public @Nullable
    Tile tile(int pos){
        return tiles == null ? null : tile(Pos.x(pos), Pos.y(pos));
    }

    public @Nullable Tile tile(int x, int y){
        return tiles.get(x, y);
    }

    public @Nullable Tile ltile(int x, int y){
        Tile tile = tile(x, y);
        if(tile == null) return null;
        return tile.block().linked(tile);
    }

    public Tile rawTile(int x, int y){
        return tiles.getn(x, y);
    }

    public @Nullable Tile tileWorld(float x, float y){
        return tile(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    public @Nullable Tile ltileWorld(float x, float y){
        return ltile(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    public int toTile(float coord){
        return Math.round(coord / tilesize);
    }

    private void clearTileEntities(){
        for(Tile tile : tiles){
            if(tile != null && tile.entity != null){
                tile.entity.remove();
            }
        }
    }

    /**
     * Resizes the tile array to the specified size and returns the resulting tile array.
     * Only use for loading saves!
     */
    public Tiles resize(int width, int height){
        clearTileEntities();

        if(tiles.width() != width || tiles.height() != height){
            tiles = new Tiles(width, height);
        }

        return tiles;
    }

    /**
     * Call to signify the beginning of map loading.
     * TileChangeEvents will not be fired until endMapLoad().
     */
    public void beginMapLoad(){
        generating = true;
    }

    /**
     * Call to signify the end of map loading. Updates tile occlusions and sets up physics for the world.
     * A WorldLoadEvent will be fire.
     */
    public void endMapLoad(){
        prepareTiles(tiles);

        for(Tile tile : tiles){
            tile.updateOcclusion();

            if(tile.entity != null){
                tile.entity.updateProximity();
            }
        }

        if(!headless){
            addDarkness(tiles);
        }

        entities.all().each(group -> group.resize(-finalWorldBounds, -finalWorldBounds, tiles.width() * tilesize + finalWorldBounds * 2, tiles.height() * tilesize + finalWorldBounds * 2));

        generating = false;
        Events.fire(new WorldLoadEvent());
    }

    public void setGenerating(boolean gen){
        this.generating = gen;
    }

    public boolean isGenerating(){
        return generating;
    }

    public boolean isZone(){
        return getZone() != null;
    }

    public Zone getZone(){
        return state.rules.zone;
    }

    public void loadGenerator(int width, int height, Cons<Tiles> generator){
        beginMapLoad();

        resize(width, height);
        generator.get(tiles);

        endMapLoad();
    }

    public void loadSector(Sector sector){
        int size = (int)(sector.rect.radius * 2500);

        loadGenerator(size, size, tiles -> {
            TileGen gen = new TileGen();
            tiles.each((x, y) -> {
                gen.reset();
                Vec3 position = sector.rect.project(x / (float)size, y / (float)size);

                sector.planet.generator.generate(position, gen);
                tiles.set(x, y, new Tile(x, y, gen.floor, gen.overlay, gen.block));
            });

            tiles.get(size/2, size/2).setBlock(Blocks.coreShard, Team.sharded);
        });
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
                ui.showErrorMessage("$map.invalid");
                Core.app.post(() -> state.set(State.menu));
                invalidMap = true;
            }
            generating = false;
            return;
        }

        this.currentMap = map;

        invalidMap = false;

        if(!headless){
            if(state.teams.playerCores().size == 0 && !checkRules.pvp){
                ui.showErrorMessage("$map.nospawn");
                invalidMap = true;
            }else if(checkRules.pvp){ //pvp maps need two cores to be valid
                if(state.teams.getActive().count(TeamData::hasCore) < 2){
                    invalidMap = true;
                    ui.showErrorMessage("$map.nospawn.pvp");
                }
            }else if(checkRules.attackMode){ //attack maps need two cores to be valid
                invalidMap = state.teams.get(state.rules.waveTeam).noCores();
                if(invalidMap){
                    ui.showErrorMessage("$map.nospawn.attack");
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
            Core.app.post(() -> Events.fire(new TileChangeEvent(tile)));
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

    public void addDarkness(Tiles tiles){
        byte[] dark = new byte[tiles.width() * tiles.height()];
        byte[] writeBuffer = new byte[tiles.width() * tiles.height()];

        byte darkIterations = 4;

        for(int i = 0; i < dark.length; i++){
            Tile tile = tiles.geti(i);
            if(tile.isDarkened()){
                dark[i] = darkIterations;
            }
        }

        for(int i = 0; i < darkIterations; i++){
            for(Tile tile : tiles){
                int idx = tile.y * tiles.width() + tile.x;
                boolean min = false;
                for(Point2 point : Geometry.d4){
                    int newX = tile.x + point.x, newY = tile.y + point.y;
                    int nidx = newY * tiles.width() + newX;
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
            int idx = tile.y * tiles.width() + tile.x;

            if(tile.isDarkened()){
                tile.rotation(dark[idx]);
            }

            if(dark[idx] == 4){
                boolean full = true;
                for(Point2 p : Geometry.d4){
                    int px = p.x + tile.x, py = p.y + tile.y;
                    int nidx = py * tiles.width() + px;
                    if(tiles.in(px, py) && !(tile.isDarkened() && dark[nidx] == 4)){
                        full = false;
                        break;
                    }
                }

                if(full) tile.rotation(5);
            }
        }
    }

    /**
     * 'Prepares' a tile array by:<br>
     * - setting up multiblocks<br>
     * - updating occlusion<br>
     * Usually used before placing structures on a tile array.
     */
    public void prepareTiles(Tiles tiles){

        //find multiblocks
        IntArray multiblocks = new IntArray();
        for(Tile tile : tiles){
            if(tile.block().isMultiblock()){
                multiblocks.add(tile.pos());
            }
        }

        //place multiblocks now
        for(int i = 0; i < multiblocks.size; i++){
            int pos = multiblocks.get(i);

            int x = Pos.x(pos);
            int y = Pos.y(pos);
            Tile tile = tiles.getn(x, y);

            Block result = tile.block();
            Team team = tile.getTeam();

            int offsetx = -(result.size - 1) / 2;
            int offsety = -(result.size - 1) / 2;

            for(int dx = 0; dx < result.size; dx++){
                for(int dy = 0; dy < result.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!(worldx == x && worldy == y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null){
                            toplace.setBlock(BlockPart.get(dx + offsetx, dy + offsety), team);
                        }
                    }
                }
            }
        }
    }

    public interface Raycaster{
        boolean accept(int x, int y);
    }

    private class Context implements WorldContext{
        @Override
        public Tile tile(int x, int y){
            return tiles.get(x, y);
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
            Array<GenerateFilter> filters = map.filters();
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
