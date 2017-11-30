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
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Profiler;
import io.anuke.ucore.util.Tmp;

public class World extends Module{
	private int seed;
	
	private Pixmap[] mapPixmaps;
	private Texture[] mapTextures;
	private Map currentMap;
	private Tile[][] tiles;
	private Tile[] temptiles = new Tile[4];
	private Pathfind pathfind = new Pathfind();
	
	public World(){
		loadMaps();
	}
	
	@Override
	public void update(){
		Profiler.begin("pathfind");
		pathfind.update();
		Profiler.end("pathfind");
	}
	
	@Override
	public void dispose(){
		for(Texture texture : mapTextures){
			texture.dispose();
		}
		
		for(Pixmap pix : mapPixmaps){
			pix.dispose();
		}
	}
	
	public Pathfind pathfinder(){
		return pathfind;
	}
	
	public boolean solid(int x, int y){
		Tile tile = tile(x, y);
		
		return tile == null || tile.block().solid || (tile.floor().solid && (tile.block() == Blocks.air));
	}
	
	public boolean wallSolid(int x, int y){
		Tile tile = tile(x, y);
		return tile == null || tile.block().solid;
	}
	
	public boolean isAccessible(int x, int y){
		return !wallSolid(x, y-1) || !wallSolid(x, y+1) || !wallSolid(x-1, y) ||!wallSolid(x+1, y);
	}
	
	public Map getMap(){
		return currentMap;
	}
	
	public int width(){
		return currentMap.width;
	}
	
	public int height(){
		return currentMap.height;
	}
	
	public Tile tile(int x, int y){
		if(!Mathf.inBounds(x, y, tiles)) return null;
		return tiles[x][y];
	}
	
	public Tile tileWorld(float x, float y){
		return tile(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize));
	}
	
	public Tile[] getNearby(int x, int y){
		temptiles[0] = tile(x+1, y);
		temptiles[1] = tile(x, y+1);
		temptiles[2] = tile(x-1, y);
		temptiles[3] = tile(x, y-1);
		return temptiles;
	}
	
	public Texture getTexture(Map map){
		return mapTextures[map.ordinal()];
	}
	
	public void loadMaps(){
		Map[] maps = Map.values();
		
		mapPixmaps = new Pixmap[maps.length];
		mapTextures = new Texture[maps.length];
		
		for(int i = 0; i < maps.length; i ++){
			Pixmap pix = new Pixmap(Gdx.files.internal("maps/"+maps[i]+".png"));
			mapPixmaps[i] = pix;
			mapTextures[i] = new Texture(pix);
			maps[i].width = pix.getWidth();
			maps[i].height = pix.getHeight();
		}
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
			
			if(tiles.length != map.width || tiles[0].length != map.height){
				tiles = new Tile[map.width][map.height];
			}
			
			createTiles();
		}else{
			tiles = new Tile[map.width][map.height];
			
			createTiles();
		}
		
		Vars.control.getSpawnPoints().clear();
		
		Entities.resizeTree(0, 0, map.width * tilesize, map.height * tilesize);
		
		this.seed = seed;
		Generator.generate(mapPixmaps[map.ordinal()], tiles);
		
		//TODO multiblock core
		placeBlock(control.getCore().x, control.getCore().y, ProductionBlocks.core, 0);
		
		if(map != Map.tutorial){
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
		if(type == ProductionBlocks.stonedrill){
			tiles[x][y].setFloor(Blocks.stone);
		}
		tiles[x][y].setBlock(type, rot);
	}
	
	public int getSeed(){
		return seed;
	}
	
	//TODO move to control or player?
	public void placeBlock(int x, int y, Block result, int rotation){
		Tile tile = tile(x, y);
		
		//just in case
		if(tile == null)
			return;

		tile.setBlock(result, rotation);
		
		if(result.isMultiblock()){
			int offsetx = -(result.width-1)/2;
			int offsety = -(result.height-1)/2;
			
			for(int dx = 0; dx < result.width; dx ++){
				for(int dy = 0; dy < result.height; dy ++){
					int worldx = dx + offsetx + x;
					int worldy = dy + offsety + y;
					if(!(worldx == x && worldy == y)){
						Tile toplace = tile(worldx, worldy);
						toplace.setLinked((byte)(dx + offsetx), (byte)(dy + offsety));
					}
					
					Effects.effect(Fx.place, worldx * Vars.tilesize, worldy * Vars.tilesize);
				}
			}
		}else{
			Effects.effect(Fx.place, x * Vars.tilesize, y * Vars.tilesize);
		}
		
		Effects.shake(2f, 2f, player);
		Sounds.play("place");
	}
	
	//TODO move this to control?
	public boolean validPlace(int x, int y, Block type){
		
		for(SpawnPoint spawn : control.getSpawnPoints()){
			if(Vector2.dst(x * tilesize, y * tilesize, spawn.start.worldx(), spawn.start.worldy()) < enemyspawnspace){
				return false;
			}
		}
		
		Tmp.r2.setSize(type.width * Vars.tilesize, type.height * Vars.tilesize);
		Vector2 offset = type.getPlaceOffset();
		Tmp.r2.setCenter(offset.x + x * Vars.tilesize, offset.y + y * Vars.tilesize);

		for(SolidEntity e : Entities.getNearby(x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle rect = e.hitbox.getRect(e.x, e.y);

			if(Tmp.r2.overlaps(rect)){
				return false;
			}
		}
		
		Tile tile = tile(x, y);
		
		if(tile == null) return false;
		
		if(type.isMultiblock() && Vars.control.getTutorial().active() &&
				Vars.control.getTutorial().showBlock()){
			
			GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
			int rotation = Vars.control.getTutorial().getPlaceRotation();
			Block block = Vars.control.getTutorial().getPlaceBlock();
			
			if(type != block || point.x != x - control.getCore().x || point.y != y - control.getCore().y 
					|| (rotation != -1 && rotation != Vars.player.rotation)){
				return false;
			}
		}else if(Vars.control.getTutorial().active()){
			return false;
		}
		
		if(tile.block() != type && type.canReplace(tile.block())){
			return true;
		}
		
		if(type.isMultiblock()){
			int offsetx = -(type.width-1)/2;
			int offsety = -(type.height-1)/2;
			for(int dx = 0; dx < type.width; dx ++){
				for(int dy = 0; dy < type.height; dy ++){
					Tile other = tile(x + dx + offsetx, y + dy + offsety);
					if(other == null || other.block() != Blocks.air){
						return false;
					}
				}
			}
			return true;
		}else{
			return tile != null && tile.block() == Blocks.air;
		}
	}
	
	public void breakBlock(int x, int y){
		Tile tile = tile(x, y);
		
		if(tile == null) return;
		
		if(tile.block().drops != null){
			Vars.control.addItem(tile.block().drops.item, tile.block().drops.amount);
		}
		
		Effects.shake(3f, 1f, player);
		Sounds.play("break");
		
		if(!tile.block().isMultiblock() && !tile.isLinked()){
			tile.setBlock(Blocks.air);
			Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
		}else{
			Tile target = tile.isLinked() ? tile.getLinked() : tile;
			Array<Tile> removals = target.getLinkedTiles();
			for(Tile toremove : removals){
				//note that setting a new block automatically unlinks it
				toremove.setBlock(Blocks.air);
				Effects.effect(Fx.breakBlock, toremove.worldx(), toremove.worldy());
			}
		}
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
	
	public boolean validBreak(int x, int y){
		Tile tile = tile(x, y);
		
		if(tile == null || tile.block() == ProductionBlocks.core) return false;
		
		if(tile.isLinked() && tile.getLinked().block() == ProductionBlocks.core){
			return false;
		}
		
		if(Vars.control.getTutorial().active()){
			
			if(Vars.control.getTutorial().showBlock()){
				GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
				int rotation = Vars.control.getTutorial().getPlaceRotation();
				Block block = Vars.control.getTutorial().getPlaceBlock();
			
				if(block != Blocks.air || point.x != x - control.getCore().x || point.y != y - control.getCore().y 
						|| (rotation != -1 && rotation != Vars.player.rotation)){
					return false;
				}
			}else{
				return false;
			}
		}
		
		return tile.breakable();
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
	
	/**
	 * Input is in block coordinates, not world coordinates.
	 * @return null if no collisions found, block position otherwise.
	 */
	public Vector2 raycast(int x0f, int y0f, int x1f, int y1f){
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
