package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.Nullable;
import io.anuke.arc.*;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.*;
import io.anuke.mindustry.ai.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.maps.generators.Generator;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.BlockPart;

import static io.anuke.mindustry.Vars.*;

public class World implements ApplicationListener{
    public final Maps maps = new Maps();
    public final BlockIndexer indexer = new BlockIndexer();
    public final WaveSpawner spawner = new WaveSpawner();
    public final Pathfinder pathfinder = new Pathfinder();
    public final Context context = new Context();

    private Map currentMap;
    private Tile[][] tiles;

    private boolean generating, invalidMap;

    public World(){
        maps.load();
    }

    @Override
    public void init(){
        maps.loadLegacyMaps();
    }

    @Override
    public void dispose(){
        maps.dispose();
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

    public @Nullable Tile tile(int pos){
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

    /** Call to signal the beginning of loading the map with a custom set of tiles. */
    public void beginMapLoad(Tile[][] tiles){
        this.tiles = tiles;
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

        addDarkness(tiles);

        Entities.getAllGroups().each(group -> group.resize(-finalWorldBounds, -finalWorldBounds, tiles.length * tilesize + finalWorldBounds * 2, tiles[0].length * tilesize + finalWorldBounds * 2));

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

        try{
            MapIO.loadMap(map);
        }catch(Exception e){
            Log.err(e);
            if(!headless){
                ui.showError("$map.invalid");
                Core.app.post(() -> state.set(State.menu));
                invalidMap = true;
            }
            generating = false;
            return;
        }

        this.currentMap = map;

        invalidMap = false;

        if(!headless){
            if(state.teams.get(player.getTeam()).cores.size == 0){
                ui.showError("$map.nospawn");
                invalidMap = true;
            }else if(state.rules.pvp){ //pvp maps need two cores to be valid
                invalidMap = true;
                for(Team team : Team.all){
                    if(state.teams.get(team).cores.size != 0 && team != player.getTeam()){
                        invalidMap = false;
                    }
                }
                if(invalidMap){
                    ui.showError("$map.nospawn.pvp");
                }
            }else if(state.rules.attackMode){ //pvp maps need two cores to be valid
                invalidMap = state.teams.get(waveTeam).cores.isEmpty();
                if(invalidMap){
                    ui.showError("$map.nospawn.attack");
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

    /**
     * Raycast, but with world coordinates.
     */
    public Point2 raycastWorld(float x, float y, float x2, float y2){
        return raycast(Math.round(x / tilesize), Math.round(y / tilesize),
        Math.round(x2 / tilesize), Math.round(y2 / tilesize));
    }

    /**
     * Input is in block coordinates, not world coordinates.
     * @return null if no collisions found, block position otherwise.
     */
    public Point2 raycast(int x0f, int y0f, int x1, int y1){
        int x0 = x0f;
        int y0 = y0f;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;
        while(true){

            if(!passable(x0, y0)){
                return Tmp.g1.set(x0, y0);
            }
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
        return null;
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
                if(tile.block().solid && !tile.block().synthetic() && tile.block().fillsTile){
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
                if(tile.block().solid && !tile.block().synthetic()){
                    tiles[x][y].rotation(dark[x][y]);
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

    class Context implements WorldContext{
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
}
