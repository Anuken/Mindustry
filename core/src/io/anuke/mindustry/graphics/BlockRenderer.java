package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.StaticBlock;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

public class BlockRenderer{
	private final static int chunksize = 32;
	private final static int initialRequests = 32*32;
	private static float storeX = 0;
	private static float storeY = 0;
	
	private int[][][] cache;
	private CacheBatch cbatch;
	
	private Array<BlockRequest> requests = new Array<BlockRequest>(initialRequests);
	private int requestidx = 0;
	private int iterateidx = 0;
	
	public BlockRenderer(){
		for(int i = 0; i < requests.size; i ++){
			requests.set(i, new BlockRequest());
		}
	}
	
	private class BlockRequest implements Comparable<BlockRequest>{
		Tile tile;
		Layer layer;
		
		@Override
		public int compareTo(BlockRequest other){
			return layer.compareTo(other.layer);
		}
		
		@Override
		public String toString(){
			return tile.block().name + ":" + layer.toString();
		}
	}
	
	/**Process all blocks to draw, simultaneously drawing block shadows and static blocks.*/
	public void processBlocks(){
		requestidx = 0;
		
		int crangex = (int) (camera.viewportWidth / (chunksize * tilesize)) + 1;
		int crangey = (int) (camera.viewportHeight / (chunksize * tilesize)) + 1;
		
		int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2)+2;
		int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2)+2;
		
		int expandr = 3;
		
		Graphics.surface(renderer.shadowSurface);
		
		for(int x = -rangex - expandr; x <= rangex + expandr; x++){
			for(int y = -rangey - expandr; y <= rangey + expandr; y++){
				int worldx = Mathf.scl(camera.position.x, tilesize) + x;
				int worldy = Mathf.scl(camera.position.y, tilesize) + y;
				boolean expanded = (x < -rangex || x > rangex || y < -rangey || y > rangey);
				
				Tile tile = world.tile(worldx, worldy);
				
				if(tile != null){
					Block block = tile.block();
					
					if(!expanded && block != Blocks.air && world.isAccessible(worldx, worldy)){
						block.drawShadow(tile);
					}
					
					if(!(block instanceof StaticBlock)){
						if(block == Blocks.air){
							if(!state.is(State.paused)) tile.floor().update(tile);
						}else{
							
							if(!expanded){
								addRequest(tile, Layer.block);
							}
							
							if(block.expanded || !expanded){
								if(block.layer != null && block.isLayer(tile)){
									addRequest(tile, block.layer);
								}
								
								if(block.layer2 != null && block.isLayer2(tile)){
									addRequest(tile, block.layer2);
								}
							}
						}
					}
				}
			}
		}
		
		Draw.color(0, 0, 0, 0.15f);
		Graphics.flushSurface();
		Draw.color();
		
		Graphics.end();
		drawCache(1, crangex, crangey);
		Graphics.begin();
		
		Arrays.sort(requests.items, 0, requestidx);
		iterateidx = 0;
	}
	
	public int getRequests(){
		return requestidx;
	}
	
	public void drawBlocks(boolean top){
		Layer stopAt = top ? Layer.laser : Layer.overlay;
		
		for(; iterateidx < requestidx; iterateidx ++){
			
			if(iterateidx < requests.size - 1 && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
				break;
			}
			
			BlockRequest req = requests.get(iterateidx);
			Block block = req.tile.block();
			
			if(req.layer == Layer.block){
				block.draw(req.tile);
			}else if(req.layer == block.layer){
				block.drawLayer(req.tile);
			}else if(req.layer == block.layer2){
				block.drawLayer2(req.tile);
			}
		}
	}
	
	private void addRequest(Tile tile, Layer layer){
		if(requestidx >= requests.size){
			requests.add(new BlockRequest());
		}
		BlockRequest r = requests.get(requestidx);
		if(r == null){
			requests.set(requestidx, r = new BlockRequest());
		}
		r.tile = tile;
		r.layer = layer;
		requestidx ++;
	}
	
	public void drawFloor(){
		int chunksx = world.width() / chunksize, chunksy = world.height() / chunksize;
		
		//render the entire map
		if(cache == null || cache.length != chunksx || cache[0].length != chunksy){
			cache = new int[chunksx][chunksy][2];
			
			for(int x = 0; x < chunksx; x++){
				for(int y = 0; y < chunksy; y++){
					cacheChunk(x, y, true);
					cacheChunk(x, y, false);
				}
			}
		}
		
		OrthographicCamera camera = Core.camera;
		
		if(Graphics.drawing()) Graphics.end();
		
		int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
		int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;
		
		drawCache(0, crangex, crangey);
		
		Graphics.begin();
		
		Draw.reset();
		
		if(showPaths && debug){
			drawPaths();
		}
		
		if(debug && debugChunks){
			Draw.color(Color.YELLOW);
			Lines.stroke(1f);
			for(int x = -crangex; x <= crangex; x++){
				for(int y = -crangey; y <= crangey; y++){
					int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
					int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;
					
					if(!Mathf.inBounds(worldx, worldy, cache))
						continue;
					Lines.rect(worldx * chunksize * tilesize, worldy * chunksize * tilesize, chunksize * tilesize, chunksize * tilesize);
				}
			}
			Draw.reset();
		}
	}
	
	void drawPaths(){
		Draw.color(Color.RED);
		for(SpawnPoint point : world.getSpawns()){
			if(point.pathTiles != null){
				for(int i = 1; i < point.pathTiles.length; i ++){
					Lines.line(point.pathTiles[i-1].worldx(), point.pathTiles[i-1].worldy(),
							   point.pathTiles[i].worldx(), point.pathTiles[i].worldy());
					Lines.circle(point.pathTiles[i-1].worldx(), point.pathTiles[i-1].worldy(), 6f);
				}
			}
		}
		Draw.reset();
	}
	
	
	void drawCache(int layer, int crangex, int crangey){
		Gdx.gl.glEnable(GL20.GL_BLEND);
		
		cbatch.setProjectionMatrix(Core.camera.combined);
		cbatch.beginDraw();
		for(int x = -crangex; x <= crangex; x++){
			for(int y = -crangey; y <= crangey; y++){
				int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
				int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;
				
				if(!Mathf.inBounds(worldx, worldy, cache))
					continue;
				
				cbatch.drawCache(cache[worldx][worldy][layer]);
			}
		}
		
		cbatch.endDraw();
	}
	
	void cacheChunk(int cx, int cy, boolean floor){
		if(cbatch == null){
			createBatch();
		}
		
		cbatch.begin();
		Graphics.useBatch(cbatch);
		
		for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
			for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
				Tile tile = world.tile(tilex, tiley);
				if(tile == null) continue;
				if(floor){
					if(!(tile.block() instanceof StaticBlock)){
						tile.floor().draw(tile);
					}
				}else if(tile.block() instanceof StaticBlock){
					tile.block().draw(tile);
				}
			}
		}
		Graphics.popBatch();
		cbatch.end();
		cache[cx][cy][floor ? 0 : 1] = cbatch.getLastCache();
	}
	
	public void clearTiles(){
		cache = null;
		createBatch();
	}
	
	private void createBatch(){
		if(cbatch != null)
			cbatch.dispose();
		cbatch = new CacheBatch(world.width() * world.height() * 4);
	}
	
	 public void drawPreview(Block block, float drawx, float drawy, float rotation, float opacity) {
		Draw.alpha(opacity);
		Draw.rect(block.name(), drawx, drawy, rotation);
	}
	
	public void handlePreview(Block block, float rotation, float drawx, float drawy, int tilex, int tiley) {
		
		if(control.input().recipe != null && state.inventory.hasItems(control.input().recipe.requirements)
		   && control.input().validPlace(tilex, tiley, block) && (mobile || control.input().cursorNear())) {
			
			 if(block.isMultiblock()) {
				 float halfBlockWidth = (block.width * tilesize) / 2;
				 float halfBlockHeight = (block.height * tilesize) / 2;
			 	if((storeX == 0 && storeY == 0)) {
			 		storeX = drawx;
			 		storeY = drawy;
				}
				if((storeX == drawx - halfBlockWidth || storeX == drawx + halfBlockWidth || storeY == drawy - halfBlockHeight || storeY == drawy + halfBlockHeight) &&
				   ((tiley - control.input().getBlockY()) % block.height != 0 || (tilex - control.input().getBlockX()) % block.width != 0)) {
			 		return;
			 	}
			 	else {
					storeX = drawx;
					storeY = drawy;
				}
			 }
			
			float opacity = (float) Settings.getInt("previewopacity") / 100f;
			Draw.color(Color.WHITE);
			Draw.alpha(opacity);
			
			if(block instanceof Turret) {
				if (block.isMultiblock()) {
					Draw.rect("block-" + block.width + "x" + block.height, drawx, drawy);
				} else {
					Draw.rect("block", drawx, drawy);
				}
			}
			
			drawPreview(block, drawx, drawy, rotation, opacity);
			
			Draw.reset();
		}
	}
}