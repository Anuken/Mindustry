package io.anuke.mindustry.graphics;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Sort;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.StaticBlock;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

public class BlockRenderer{
	private final static int initialRequests = 32*32;

	private FloorRenderer floorRenderer;
	
	private Array<BlockRequest> requests = new Array<>(initialRequests);
	private Layer lastLayer;
	private int requestidx = 0;
	private int iterateidx = 0;

	public BlockRenderer(){
		floorRenderer = new FloorRenderer();

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
		lastLayer = null;
		
		int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2)+2;
		int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2)+2;

		int expandr = 4;
		
		Graphics.surface(renderer.effectSurface);

		for(int x = -rangex - expandr; x <= rangex + expandr; x++){
			for(int y = -rangey - expandr; y <= rangey + expandr; y++){
				int worldx = Mathf.scl(camera.position.x, tilesize) + x;
				int worldy = Mathf.scl(camera.position.y, tilesize) + y;
				boolean expanded = (x < -rangex || x > rangex || y < -rangey || y > rangey);

				synchronized (Tile.tileSetLock) {
					Tile tile = world.tile(worldx, worldy);

					if (tile != null) {
						Block block = tile.block();

						if (!expanded && block != Blocks.air && world.isAccessible(worldx, worldy)) {
							tile.block().drawShadow(tile);
						}

						if (!(block instanceof StaticBlock)) {
							if (block != Blocks.air) {
								if (!expanded) {
									addRequest(tile, Layer.block);
								}

								if (block.expanded || !expanded) {
									if (block.layer != null && block.isLayer(tile)) {
										addRequest(tile, block.layer);
									}

									if (block.layer2 != null && block.isLayer2(tile)) {
										addRequest(tile, block.layer2);
									}
								}
							}
						}
					}
				}
			}
		}

		//TODO this actually isn't necessary
		Draw.color(0, 0, 0, 0.15f);
		Graphics.flushSurface();
		Draw.color();

		Graphics.end();
		floorRenderer.beginDraw();
		floorRenderer.drawLayer(CacheLayer.walls);
		floorRenderer.endDraw();
		Graphics.begin();

		Sort.instance().sort(requests.items, 0, requestidx);
		iterateidx = 0;
	}

	public int getRequests(){
		return requestidx;
	}
	
	public void drawBlocks(Layer stopAt){
		
		for(; iterateidx < requestidx; iterateidx ++){

			if(iterateidx < requests.size && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
				break;
			}
			
			BlockRequest req = requests.get(iterateidx);

			if(req.layer != lastLayer){
				if(lastLayer != null) layerEnds(lastLayer);
				layerBegins(req.layer);
			}

			synchronized (Tile.tileSetLock) {
				Block block = req.tile.block();

				if (req.layer == Layer.block) {
					block.draw(req.tile);
				} else if (req.layer == block.layer) {
					block.drawLayer(req.tile);
				} else if (req.layer == block.layer2) {
					block.drawLayer2(req.tile);
				}
			}

			lastLayer = req.layer;
		}
	}

	public void drawTeamBlocks(Layer layer, Team team){
		int index = this.iterateidx;

		for(; index < requestidx; index ++){

			if(index < requests.size && requests.get(index).layer.ordinal() > layer.ordinal()){
				break;
			}

			BlockRequest req = requests.get(index);
			if(req.tile.getTeam() != team) continue;

			synchronized (Tile.tileSetLock) {
				Block block = req.tile.block();

				if (req.layer == block.layer) {
					block.drawLayer(req.tile);
				} else if (req.layer == block.layer2) {
					block.drawLayer2(req.tile);
				}
			}
		}
	}

	public void skipLayer(Layer stopAt){

		for(; iterateidx < requestidx; iterateidx ++){
			if(iterateidx < requests.size && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
				break;
			}
		}
	}

	public void beginFloor(){
		floorRenderer.beginDraw();
	}

	public void endFloor(){
		floorRenderer.endDraw();
	}

	public void drawFloor(){
		floorRenderer.drawFloor();
	}

	private void layerBegins(Layer layer){}

	private void layerEnds(Layer layer){}

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
}
