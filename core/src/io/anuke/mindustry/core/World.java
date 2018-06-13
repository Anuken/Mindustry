package io.anuke.mindustry.core;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.ai.BlockIndexer;
import io.anuke.mindustry.ai.Pathfinder;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.io.Map;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.io.Maps;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.WorldGenerator;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.ThreadArray;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.threads;
import static io.anuke.mindustry.Vars.tilesize;

public class World extends Module{
	private int seed;
	
	private Map currentMap;
	private Tile[][] tiles;
	private Pathfinder pathfinder = new Pathfinder();
	private BlockIndexer indexer = new BlockIndexer();
	private Maps maps = new Maps();

	private Array<Tile> tempTiles = new ThreadArray<>();
	private boolean generating;
	
	public World(){
		maps.load();
	}
	
	@Override
	public void dispose(){
		maps.dispose();
	}
	
	public Maps maps(){
		return maps;
	}

	public BlockIndexer indexer() {
		return indexer;
	}

	public Pathfinder pathfinder(){
		return pathfinder;
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
		return !wallSolid(x, y-1) || !wallSolid(x, y+1) || !wallSolid(x-1, y) ||!wallSolid(x+1, y);
	}
	
	public boolean blends(Block block, int x, int y){
		return !floorBlends(x, y-1, block) || !floorBlends(x, y+1, block) 
				|| !floorBlends(x-1, y, block) ||!floorBlends(x+1, y, block);
	}
	
	public boolean floorBlends(int x, int y, Block block){
		Tile tile = tile(x, y);
		return tile == null || tile.floor().id <= block.id;
	}
	
	public Map getMap(){
		return currentMap;
	}
	
	public int width(){
		return currentMap.meta.width;
	}
	
	public int height(){
		return currentMap.meta.height;
	}

	public Tile tile(int packed){
		return tile(packed % width(), packed / width());
	}
	
	public Tile tile(int x, int y){
		if(tiles == null){
			return null;
		}
		if(!Mathf.inBounds(x, y, tiles)) return null;
		return tiles[x][y];
	}
	
	public Tile tileWorld(float x, float y){
		return tile(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize));
	}

	public int toTile(float coord){
		return Mathf.scl2(coord, tilesize);
	}
	
	public Tile[][] getTiles(){
		return tiles;
	}
	
	private void clearTileEntities(){
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles[0].length; y ++){
				if(tiles[x][y] != null && tiles[x][y].entity != null){
					tiles[x][y].entity.remove();
				}
			}
		}
	}

	/**Resizes the tile array to the specified size and returns the resulting tile array.
     * Only use for loading saves!*/
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

	/**Call to signify the beginning of map loading.
	 * TileChangeEvents will not be fired until endMapLoad().*/
	public void beginMapLoad(){
    	generating = true;
	}

	/**Call to signify the end of map loading. Updates tile occlusions and sets up physics for the world.
	 * A WorldLoadEvent will be fire.*/
	public void endMapLoad(){
		for(int x = 0; x < tiles.length; x ++) {
			for (int y = 0; y < tiles[0].length; y++) {
				tiles[x][y].updateOcclusion();
			}
		}

		EntityPhysics.resizeTree(0, 0, tiles.length * tilesize, tiles[0].length * tilesize);

    	generating = false;
		Events.fire(WorldLoadEvent.class);
	}

    public void setMap(Map map){
    	this.currentMap = map;
	}
	
	public void loadMap(Map map){
		loadMap(map, MathUtils.random(0, 999999));
	}
	
	public void loadMap(Map map, int seed){
    	beginMapLoad();
		this.currentMap = map;
		this.seed = seed;

		int width = map.meta.width, height = map.meta.height;

		createTiles(width, height);
		
		EntityPhysics.resizeTree(0, 0, width * tilesize, height * tilesize);

		WorldGenerator.generate(tiles, MapIO.readTileData(map, true));

		endMapLoad();
	}

	public int getSeed(){
		return seed;
	}

	public void notifyChanged(Tile tile){
    	if(!generating){
    		threads.runDelay(() -> Events.fire(TileChangeEvent.class, tile));
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

	/**Raycast, but with world coordinates.*/
	public GridPoint2 raycastWorld(float x, float y, float x2, float y2){
		return raycast(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize),
				Mathf.scl2(x2, tilesize), Mathf.scl2(y2, tilesize));
	}
	
	/**Input is in block coordinates, not world coordinates.
	 * @return null if no collisions found, block position otherwise.*/
	public GridPoint2 raycast(int x0f, int y0f, int x1, int y1){
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

	public interface Raycaster{
		boolean accept(int x, int y);
	}
}
