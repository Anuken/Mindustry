package io.anuke.mindustry.core;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.io.Maps;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Generator;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.DistributionBlocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;

public class World extends Module{
	private int seed;
	
	private Map currentMap;
	private Tile[][] tiles;
	private Tile[] temptiles = new Tile[4];
	private Pathfind pathfind = new Pathfind();
	private io.anuke.mindustry.io.Maps maps = new io.anuke.mindustry.io.Maps();
	
	public World(){
		maps.loadMaps();
		currentMap = maps.getMap(0);
	}
	
	@Override
	public void update(){
		if(!(Net.active() && Net.client()))
			pathfind.update();
	}
	
	@Override
	public void dispose(){
		maps.dispose();
	}
	
	public Maps maps(){
		return maps;
	}
	
	public Pathfind pathfinder(){
		return pathfind;
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
		return currentMap.getWidth();
	}
	
	public int height(){
		return currentMap.getHeight();
	}
	
	public Tile tile(int x, int y){
		if(tiles == null){
			ui.showGameError();
			GameState.set(State.menu);
			return null;
		}
		if(!Mathf.inBounds(x, y, tiles)) return null;
		return tiles[x][y];
	}
	
	public Tile tileWorld(float x, float y){
		return tile(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize));
	}
	
	public Tile[][] getTiles(){
		return tiles;
	}
	
	public Tile[] getNearby(int x, int y){
		return getNearby(x, y, temptiles);
	}

	public Tile[] getNearby(int x, int y, Tile[] temptiles){
		temptiles[0] = tile(x+1, y);
		temptiles[1] = tile(x, y+1);
		temptiles[2] = tile(x-1, y);
		temptiles[3] = tile(x, y-1);
		return temptiles;
	}
	
	private void createTiles(){
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles[0].length; y ++){
				if(tiles[x][y] == null){
					tiles[x][y] = new Tile(x, y, Blocks.stone);
				}
			}
		}
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
	
	public void loadMap(Map map){
		loadMap(map, MathUtils.random(0, 99999));
	}
	
	public void loadMap(Map map, int seed){
		currentMap = map;
		
		if(tiles != null){
			clearTileEntities();
			
			if(tiles.length != map.getWidth() || tiles[0].length != map.getHeight()){
				tiles = new Tile[map.getWidth()][map.getHeight()];
			}
			
			createTiles();
		}else{
			tiles = new Tile[map.getWidth()][map.getHeight()];
			
			createTiles();
		}
		
		Vars.control.getSpawnPoints().clear();
		
		Entities.resizeTree(0, 0, map.getWidth() * tilesize, map.getHeight() * tilesize);
		
		this.seed = seed;
		Generator.generate(map.pixmap, tiles);
		
		if(control.getCore() == null) return;
		
		//TODO multiblock core
		control.getInput().placeBlock(control.getCore().x, control.getCore().y, ProductionBlocks.core, 0, false, false);
		
		if(!map.name.equals("tutorial")){
			setDefaultBlocks();
		}else{
			Vars.control.getTutorial().setDefaultBlocks(control.getCore().x, control.getCore().y);
		}
		
		pathfind.updatePath();
	}
	
	void setDefaultBlocks(){
		int x = control.getCore().x, y = control.getCore().y;
		int flip = Mathf.sign(!currentMap.flipBase);
		int fr = currentMap.flipBase ? 2 : 0;
		
		set(x, y-2*flip, DistributionBlocks.conveyor, 1 + fr);
		set(x, y-3*flip, DistributionBlocks.conveyor, 1 + fr);
		
		for(int i = 0; i < 2; i ++){
			int d = Mathf.sign(i-0.5f);
			
			set(x+2*d, y-2*flip, ProductionBlocks.stonedrill, d);
			set(x+2*d, y-1*flip, DistributionBlocks.conveyor, 1 + fr);
			set(x+2*d, y, DistributionBlocks.conveyor, 1 + fr);
			set(x+2*d, y+1*flip, WeaponBlocks.doubleturret, 0 + fr);
			
			set(x+1*d, y-3*flip, DistributionBlocks.conveyor, 2*d);
			set(x+2*d, y-3*flip, DistributionBlocks.conveyor, 2*d);
			set(x+2*d, y-4*flip, DistributionBlocks.conveyor, 1 + fr);
			set(x+2*d, y-5*flip, DistributionBlocks.conveyor, 1 + fr);
			
			set(x+3*d, y-5*flip, ProductionBlocks.stonedrill, 0 + fr);
			set(x+3*d, y-4*flip, ProductionBlocks.stonedrill, 0 + fr);
			set(x+3*d, y-3*flip, ProductionBlocks.stonedrill, 0 + fr);
		}
	}
	
	void set(int x, int y, Block type, int rot){
		if(!Mathf.inBounds(x, y, tiles)){
			return;
		}
		if(type == ProductionBlocks.stonedrill){
			tiles[x][y].setFloor(Blocks.stone);
		}
		tiles[x][y].setBlock(type, rot);
	}
	
	public int getSeed(){
		return seed;
	}
	
	public void removeBlock(Tile tile){
		if(!tile.block().isMultiblock() && !tile.isLinked()){
			tile.setBlock(Blocks.air);
		}else{
			Tile target = tile.isLinked() ? tile.getLinked() : tile;
			Array<Tile> removals = target.getLinkedTiles();
			for(Tile toremove : removals){
				//note that setting a new block automatically unlinks it
				toremove.setBlock(Blocks.air);
			}
		}
	}
	
	public TileEntity findTileTarget(float x, float y, Tile tile, float range, boolean damaged){
		Entity closest = null;
		float dst = 0;
		
		int rad = (int)(range/tilesize)+1;
		int tilex = Mathf.scl2(x, tilesize);
		int tiley = Mathf.scl2(y, tilesize);
		
		for(int rx = -rad; rx <= rad; rx ++){
			for(int ry = -rad; ry <= rad; ry ++){
				Tile other = tile(rx+tilex, ry+tiley);
				
				if(other != null && other.getLinked() != null){
					other = other.getLinked();
				}
				
				if(other == null || other.entity == null || (tile != null && other.entity == tile.entity)) continue;
				
				TileEntity e = other.entity;
				
				if(damaged && e.health >= e.tile.block().health)
					continue;
				
				float ndst = Vector2.dst(x, y, e.x, e.y);
				if(ndst < range && (closest == null || ndst < dst)){
					dst = ndst;
					closest = e;
				}
			}
		}

		return (TileEntity) closest;
	}
	
	public GridPoint2 raycastWorld(float x, float y, float x2, float y2){
		return raycast(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize),
				Mathf.scl2(x2, tilesize), Mathf.scl2(y2, tilesize));
	}
	
	/**
	 * Input is in block coordinates, not world coordinates.
	 * @return null if no collisions found, block position otherwise.
	 */
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
}
