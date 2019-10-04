package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.maps.filters.*;
import io.anuke.mindustry.maps.filters.GenerateFilter.*;
import io.anuke.mindustry.maps.generators.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.*;

public class World{
    public final Context context = new Context();

    private Map currentMap;
    private Tile[][] tiles;

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
        return tiles == null ? 0 : tiles.length;
    }

    public int height(){
        return tiles == null ? 0 : tiles[0].length;
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
        if(tiles == null){
            return null;
        }
        if(!Structs.inBounds(x, y, tiles)) return null;
        return tiles[x][y];
    }

    public @Nullable Tile ltile(int x, int y){
        Tile tile = tile(x, y);
        if(tile == null) return null;
        return tile.block().linked(tile);
    }

    public Tile rawTile(int x, int y){
        return tiles[x][y];
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

    public Tile[][] getTiles(){
        return tiles;
    }

    private void clearTileEntities(){
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                if(tiles[x][y] != null && tiles[x][y].entity != null){
                    tiles[x][y].entity.remove();
                }
            }
        }
    }

    /**
     * Resizes the tile array to the specified size and returns the resulting tile array.
     * Only use for loading saves!
     */
    public Tile[][] createTiles(int width, int height){
        if(tiles != null){
            clearTileEntities();

            if(tiles.length != width || tiles[0].length != height){
                tiles = new Tile[width][height];
            }
        }else{
            tiles = new Tile[width][height];
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

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];
                tile.updateOcclusion();

                if(tile.entity != null){
                    tile.entity.updateProximity();
                }
            }
        }

        if(!headless){
            addDarkness(tiles);
        }

        entities.all().each(group -> group.resize(-finalWorldBounds, -finalWorldBounds, tiles.length * tilesize + finalWorldBounds * 2, tiles[0].length * tilesize + finalWorldBounds * 2));

        generating = false;
        Events.fire(new WorldLoadEvent());
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

    public void loadGenerator(Generator generator){
        beginMapLoad();

        createTiles(generator.width, generator.height);
        generator.generate(tiles);

        endMapLoad();
    }

    public void loadMap(Map map){
        loadMap(map, new Rules());
    }

    public void loadMap(Map map, Rules checkRules){
        try{
            SaveIO.load(map.file, new FilterContext(map));
        }catch(Exception e){
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
            if(state.teams.get(defaultTeam).cores.size == 0 && !checkRules.pvp){
                ui.showErrorMessage("$map.nospawn");
                invalidMap = true;
            }else if(checkRules.pvp){ //pvp maps need two cores to be valid
                int teams = 0;
                for(Team team : Team.all){
                    if(state.teams.get(team).cores.size != 0){
                        teams ++;
                    }
                }
                if(teams < 2){
                    invalidMap = true;
                    ui.showErrorMessage("$map.nospawn.pvp");
                }
            }else if(checkRules.attackMode){ //attack maps need two cores to be valid
                invalidMap = state.teams.get(waveTeam).cores.isEmpty();
                if(invalidMap){
                    ui.showErrorMessage("$map.nospawn.attack");
                }
            }
        }else{
            invalidMap = true;
            for(Team team : Team.all){
                if(state.teams.get(team).cores.size != 0){
                    invalidMap = false;
                }
            }

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

    public void removeBlock(Tile tile){
        tile.link().getLinkedTiles(other -> other.setBlock(Blocks.air));
    }

    public void setBlock(Tile tile, Block block, Team team){
        setBlock(tile, block, team, 0);
    }

    public void setBlock(Tile tile, Block block, Team team, int rotation){
        tile.setBlock(block, team, rotation);
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    int worldx = dx + offsetx + tile.x;
                    int worldy = dy + offsety + tile.y;
                    if(!(worldx == tile.x && worldy == tile.y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null){
                            toplace.setBlock(BlockPart.get(dx + offsetx, dy + offsety), team);
                        }
                    }
                }
            }
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

    public void addDarkness(Tile[][] tiles){
        byte[][] dark = new byte[tiles.length][tiles[0].length];
        byte[][] writeBuffer = new byte[tiles.length][tiles[0].length];

        byte darkIterations = 4;
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];
                if(tile.isDarkened()){
                    dark[x][y] = darkIterations;
                }
            }
        }

        for(int i = 0; i < darkIterations; i++){
            for(int x = 0; x < tiles.length; x++){
                for(int y = 0; y < tiles[0].length; y++){
                    boolean min = false;
                    for(Point2 point : Geometry.d4){
                        int newX = x + point.x, newY = y + point.y;
                        if(Structs.inBounds(newX, newY, tiles) && dark[newX][newY] < dark[x][y]){
                            min = true;
                            break;
                        }
                    }
                    writeBuffer[x][y] = (byte)Math.max(0, dark[x][y] - Mathf.num(min));
                }
            }

            for(int x = 0; x < tiles.length; x++){
                System.arraycopy(writeBuffer[x], 0, dark[x], 0, tiles[0].length);
            }
        }

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];
                if(tile.isDarkened()){
                    tiles[x][y].rotation(dark[x][y]);
                }
                if(dark[x][y] == 4){
                    boolean full = true;
                    for(Point2 p : Geometry.d4){
                        int px = p.x + x, py = p.y + y;
                        if(Structs.inBounds(px, py, tiles) && !(tiles[px][py].isDarkened() && dark[px][py] == 4)){
                            full = false;
                            break;
                        }
                    }

                    if(full) tiles[x][y].rotation(5);
                }
            }
        }
    }

    /**
     * 'Prepares' a tile array by:<br>
     * - setting up multiblocks<br>
     * - updating occlusion<br>
     * Usually used before placing structures on a tile array.
     */
    public void prepareTiles(Tile[][] tiles){

        //find multiblocks
        IntArray multiblocks = new IntArray();

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];

                if(tile.block().isMultiblock()){
                    multiblocks.add(tile.pos());
                }
            }
        }

        //place multiblocks now
        for(int i = 0; i < multiblocks.size; i++){
            int pos = multiblocks.get(i);

            int x = Pos.x(pos);
            int y = Pos.y(pos);

            Block result = tiles[x][y].block();
            Team team = tiles[x][y].getTeam();

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
            return tiles[x][y];
        }

        @Override
        public void resize(int width, int height){
            createTiles(width, height);
        }

        @Override
        public Tile create(int x, int y, int floorID, int overlayID, int wallID){
            return (tiles[x][y] = new Tile(x, y, floorID, overlayID, wallID));
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
                    input.begin(filter, width(), height(), (x, y) -> tiles[x][y]);

                    //actually apply the filter
                    for(int x = 0; x < width(); x++){
                        for(int y = 0; y < height(); y++){
                            Tile tile = rawTile(x, y);
                            input.apply(x, y, tile.floor(), tile.block(), tile.overlay());
                            filter.apply(input);

                            tile.setFloor((Floor)input.floor);
                            tile.setOverlay(input.ore);

                            if(!tile.block().synthetic() && !input.block.synthetic()){
                                tile.setBlock(input.block);
                            }
                        }
                    }
                }
            }

            super.end();
        }
    }
}
