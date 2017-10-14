package io.anuke.mindustry.world;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class World{
	public static int worldsize = 128;
	public static int pixsize = worldsize*tilesize;
	private static int seed;
	
	private static Pixmap[] mapPixmaps;
	private static Texture[] mapTextures;
	private static Map currentMap;
	private static Tile[][] tiles = new Tile[worldsize][worldsize];
	private static Tile[] temptiles = new Tile[4];
	
	public static Tile core;
	//TODO move this to control?
	public static Array<Tile> spawnpoints = new Array<Tile>();
	
	public static boolean solid(int x, int y){
		Tile tile = tile(x, y);
		
		return tile == null || tile.block().solid || (tile.floor().solid && (tile.block() == Blocks.air));
	}
	
	public static boolean wallSolid(int x, int y){
		Tile tile = tile(x, y);
		return tile == null || tile.block().solid;
	}
	
	public static boolean isAccessible(int x, int y){
		return !wallSolid(x, y-1) || !wallSolid(x, y+1) || !wallSolid(x-1, y) ||!wallSolid(x+1, y);
	}
	
	public static Map getMap(){
		return currentMap;
	}
	
	public static int width(){
		return mapPixmaps[currentMap.ordinal()].getWidth();
	}
	
	public static int height(){
		return mapPixmaps[currentMap.ordinal()].getHeight();
	}
	
	public static Tile tile(int x, int y){
		if(!Mathf.inBounds(x, y, tiles)) return null;
		return tiles[x][y];
	}
	
	public static Tile cursorTile(){
		return tile(tilex(), tiley());
	}
	
	public static Tile[] getNearby(int x, int y){
		temptiles[0] = tile(x+1, y);
		temptiles[1] = tile(x, y+1);
		temptiles[2] = tile(x-1, y);
		temptiles[3] = tile(x, y-1);
		return temptiles;
	}
	
	public static Texture getTexture(Map map){
		return mapTextures[map.ordinal()];
	}
	
	public static void loadMaps(){
		Map[] maps = Map.values();
		
		mapPixmaps = new Pixmap[maps.length];
		mapTextures = new Texture[maps.length];
		
		for(int i = 0; i < maps.length; i ++){
			Pixmap pix = new Pixmap(Gdx.files.internal("maps/"+maps[i]+".png"));
			mapPixmaps[i] = pix;
			mapTextures[i] = new Texture(pix);
		}
	}
	
	private static void createTiles(){
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles[0].length; y ++){
				if(tiles[x][y] == null){
					tiles[x][y] = new Tile(x, y, Blocks.stone);
				}
			}
		}
	}
	
	private static void clearTileEntities(){
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles[0].length; y ++){
				if(tiles[x][y] != null && tiles[x][y].entity != null){
					tiles[x][y].entity.remove();
				}
			}
		}
	}
	
	public static void loadMap(Map map){
		loadMap(map, MathUtils.random(0, 99999));
	}
	
	public static void loadMap(Map map, int seed){
		
		spawnpoints.clear();
		
		int size = mapPixmaps[map.ordinal()].getWidth();
		worldsize = size;
		pixsize = worldsize*tilesize;
		currentMap = map;
		
		if(tiles != null){
			clearTileEntities();
			
			if(tiles.length != worldsize || tiles[0].length != worldsize){
				tiles = new Tile[worldsize][worldsize];
			}
			
			createTiles();
		}else{
		
			tiles = new Tile[worldsize][worldsize];
			
			createTiles();
		}
		
		Entities.resizeTree(0, 0, pixsize, pixsize);
		
		World.seed = seed;
		Generator.generate(mapPixmaps[map.ordinal()]);
		
		Pathfind.reset();
		
		core.setBlock(ProductionBlocks.core);
		
		if(map != Map.tutorial){
			setDefaultBlocks();
		}else{
			Vars.control.getTutorial().setDefaultBlocks(core.x, core.y);
		}
		
		Pathfind.updatePath();
	}
	
	static void setDefaultBlocks(){
		int x = core.x, y = core.y;
		
		set(x, y-1, ProductionBlocks.conveyor, 1);
		set(x, y-2, ProductionBlocks.conveyor, 1);
		set(x, y-3, ProductionBlocks.conveyor, 1);
		set(x, y-4, ProductionBlocks.stonedrill, 0);
		//just in case
		tiles[x][y-4].setFloor(Blocks.stone);
		
		
		tiles[x+2][y-2].setFloor(Blocks.stone);
		set(x+2, y-2, ProductionBlocks.stonedrill, 0);
		set(x+2, y-1, ProductionBlocks.conveyor, 1);
		set(x+2, y, WeaponBlocks.turret, 0);
		
		tiles[x-2][y-2].setFloor(Blocks.stone);
		set(x-2, y-2, ProductionBlocks.stonedrill, 0);
		set(x-2, y-1, ProductionBlocks.conveyor, 1);
		set(x-2, y, WeaponBlocks.turret, 0);
	}
	
	static void set(int x, int y, Block type, int rot){
		tiles[x][y].setBlock(type);
		tiles[x][y].rotation = (byte)rot;
	}
	
	public static int getSeed(){
		return seed;
	}
	
	//TODO move this to control?
	public static boolean validPlace(int x, int y, Block type){

		if(!cursorNear() && !android)
			return false;
		
		for(Tile spawn : spawnpoints){
			if(Vector2.dst(x * tilesize, y * tilesize, spawn.worldx(), spawn.worldy()) < enemyspawnspace){
				return false;
			}
		}

		for(SolidEntity e : Entities.getNearby(x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle.tmp.setSize(e.hitsize);
			Rectangle.tmp.setCenter(e.x, e.y);

			if(getCollider(x, y).overlaps(Rectangle.tmp)){
				return false;
			}
		}
		
		Tile tile = tile(x, y);
		
		if(tile == null) return false;
		
		if(Vars.control.getTutorial().active() &&
				Vars.control.getTutorial().showBlock()){
			
			GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
			int rotation = Vars.control.getTutorial().getPlaceRotation();
			Block block = Vars.control.getTutorial().getPlaceBlock();
			
			if(type != block || point.x != x - core.x || point.y != y - core.y || (rotation != -1 && rotation != Vars.player.rotation)){
				return false;
			}
		}
		
		if(tile.block() != type && type.canReplace(tile.block())){
			return true;
		}
		
		return tile != null && tile.block() == Blocks.air;
	}
	
	public static boolean validBreak(int x, int y){
		Tile tile = tile(x, y);
		
		if(tile == null || tile.block() == ProductionBlocks.core) return false;
		
		if(Vars.control.getTutorial().active()){
			
			if(Vars.control.getTutorial().showBlock()){
				GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
				int rotation = Vars.control.getTutorial().getPlaceRotation();
				Block block = Vars.control.getTutorial().getPlaceBlock();
			
				if(block != Blocks.air || point.x != x - core.x || point.y != y - core.y || (rotation != -1 && rotation != Vars.player.rotation)){
					return false;
				}
			}else{
				return false;
			}
		}
		
		return tile.breakable();
	}
	
	public static boolean cursorNear(){
		return Vector2.dst(player.x, player.y, tilex() * tilesize, tiley() * tilesize) <= placerange;
	}
	
	public static Rectangle getCollider(int x, int y){
		return Rectangle.tmp2.setSize(tilesize).setCenter(x * tilesize, y * tilesize);
	}
	
	public static TileEntity findTileTarget(float x, float y, Tile tile, float range, boolean damaged){
		Entity closest = null;
		float dst = 0;
		
		int rad = (int)(range/tilesize)+1;
		int tilex = Mathf.scl2(x, tilesize);
		int tiley = Mathf.scl2(y, tilesize);
		
		for(int rx = -rad; rx <= rad; rx ++){
			for(int ry = -rad; ry <= rad; ry ++){
				Tile other = tile(rx+tilex, ry+tiley);
				
				if(other == null || other.entity == null ||(tile != null && other.entity == tile.entity)) continue;
				
				TileEntity e = other.entity;
				
				if(damaged && ((TileEntity) e).health >= ((TileEntity) e).tile.block().health)
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
	
	public static float roundx(){
		return Mathf.round2(Graphics.mouseWorld().x, tilesize);
	}

	public static float roundy(){
		return Mathf.round2(Graphics.mouseWorld().y, tilesize);
	}

	public static int tilex(){
		return Mathf.scl2(Graphics.mouseWorld().x, tilesize);
	}

	public static int tiley(){
		return Mathf.scl2(Graphics.mouseWorld().y, tilesize);
	}
	
	public static void disposeMaps(){
		for(Pixmap pixmap : mapPixmaps){
			pixmap.dispose();
		}
		
		for(Texture texture : mapTextures){
			texture.dispose();
		}
	}
	
	/**
	 * Input is in block coordinates, not world coordinates.
	 * @return null if no collisions found, block position otherwise.
	 */
	public static Vector2 raycast(int x0f, int y0f, int x1f, int y1f){
		int x0 = (int)x0f;
		int y0 = (int)y0f;
		int x1 = (int)x1f;
		int y1 = (int)y1f;
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);

		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;

		int err = dx - dy;
		int e2;
		while(true){

			if(solid(x0, y0)){
				return Tmp.v3.set(x0, y0);
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
