package io.anuke.mindustry.core;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.entities.EntityQuery;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.ai.BlockIndexer;
import io.anuke.mindustry.ai.Pathfinder;
import io.anuke.mindustry.ai.WaveSpawner;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.maps.Maps;
import io.anuke.mindustry.maps.generators.Generator;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class World implements ApplicationListener{
    public final Maps maps = new Maps();
    public final BlockIndexer indexer = new BlockIndexer();
    public final WaveSpawner spawner = new WaveSpawner();
    public final Pathfinder pathfinder = new Pathfinder();

    private Map currentMap;
    private Tile[][] tiles;

    private Array<Tile> tempTiles = new Array<>();
    private boolean generating, invalidMap;

    public World(){
        maps.load();
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

    public Tile tile(int pos){
        return tiles == null ? null : tile(Pos.x(pos), Pos.y(pos));
    }

    public Tile tile(int x, int y){
        if(tiles == null){
            return null;
        }
        if(!Structs.inBounds(x, y, tiles)) return null;
        return tiles[x][y];
    }

    public Tile rawTile(int x, int y){
        return tiles[x][y];
    }

    public Tile tileWorld(float x, float y){
        return tile(Math.round(x / tilesize), Math.round(y / tilesize));
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

    /**Call to signal the beginning of loading the map with a custom set of tiles.*/
    public void beginMapLoad(Tile[][] tiles){
        this.tiles = tiles;
        generating = true;
    }

    /**
     * Call to signify the end of map loading. Updates tile occlusions and sets up physics for the world.
     * A WorldLoadEvent will be fire.
     */
    public void endMapLoad(){
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];
                tile.updateOcclusion();

                if(tile.entity != null){
                    tile.entity.updateProximity();
                }
            }
        }

        EntityQuery.resizeTree(0, 0, tiles.length * tilesize, tiles[0].length * tilesize);

        generating = false;
        Events.fire(new WorldLoadEvent());
    }

    public boolean isGenerating(){
        return generating;
    }

    public void playZone(Zone zone){
        ui.loadAnd(() -> {
            logic.reset();
            state.rules = zone.rules.get();
            loadGenerator(zone.generator);
            for(Tile core : state.teams.get(defaultTeam).cores){
                for(ItemStack stack : zone.startingItems){
                    core.entity.items.add(stack.item, stack.amount);
                }
            }
            logic.play();
        });
    }

    public void loadGenerator(Generator generator){
        beginMapLoad();

        createTiles(generator.width, generator.height);
        generator.generate(tiles);
        prepareTiles(tiles);

        endMapLoad();
    }

    public void loadMap(Map map){
        beginMapLoad();
        this.currentMap = map;

        int width = map.meta.width, height = map.meta.height;

        createTiles(width, height);

        try{
            loadTileData(tiles, MapIO.readTileData(map, true));
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

        endMapLoad();

        invalidMap = false;

        if(!headless){
            if(state.teams.get(players[0].getTeam()).cores.size == 0){
                ui.showError("$map.nospawn");
                invalidMap = true;
            }else if(state.rules.pvp){ //pvp maps need two cores to  be valid
                invalidMap = true;
                for(Team team : Team.all){
                    if(state.teams.get(team).cores.size != 0 && team != players[0].getTeam()){
                        invalidMap = false;
                    }
                }
                if(invalidMap){
                    ui.showError("$map.nospawn.pvp");
                }
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
        if(!tile.block().isMultiblock() && !tile.isLinked()){
            tile.setBlock(Blocks.air);
        }else{
            Tile target = tile.target();
            Array<Tile> removals = target.getLinkedTiles(tempTiles);
            for(Tile toremove : removals){
                //note that setting a new block automatically unlinks it
                if(toremove != null) toremove.setBlock(Blocks.air);
            }
        }
    }

    public void setBlock(Tile tile, Block block, Team team){
        tile.setBlock(block, team);
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
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }
    }

    public int transform(int packed, int oldWidth, int oldHeight, int newWidth, int shiftX, int shiftY){
        int x = packed % oldWidth;
        int y = packed / oldWidth;
        if(!Structs.inBounds(x, y, oldWidth, oldHeight)) return -1;
        x += shiftX;
        y += shiftY;
        return y*newWidth + x;
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
     *
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

    /**Loads raw map tile data into a Tile[][] array, setting up multiblocks, cliffs and ores. */
    void loadTileData(Tile[][] tiles, MapTileData data){
        data.position(0, 0);
        TileDataMarker marker = data.newDataMarker();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);

                tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team);
            }
        }

        prepareTiles(tiles);
    }

    /**'Prepares' a tile array by:<br>
     * - setting up multiblocks<br>
     * - updating occlusion<br>
     * Usually used before placing structures on a tile array.*/
    public void prepareTiles(Tile[][] tiles){

        byte[][] dark = new byte[tiles.length][tiles[0].length];
        byte[][] writeBuffer = new byte[tiles.length][tiles[0].length];

        byte darkIterations = 4;
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];
                if(tile.block().solid && !tile.block().update){
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
                for(int y = 0; y < tiles[0].length; y++){
                    dark[x][y] = writeBuffer[x][y];
                }
            }
        }

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                tiles[x][y].setRotation(dark[x][y]);
            }
        }

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
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }

        //update cliffs, occlusion data
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = tiles[x][y];

                tile.updateOcclusion();
            }
        }
    }

    public interface Raycaster{
        boolean accept(int x, int y);
    }
}
