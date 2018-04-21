package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.StaticBlock;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

public class BlockRenderer{
	private final static int initialRequests = 32*32;

	private FloorRenderer floorRenderer;
	
	private Array<BlockRequest> requests = new Array<BlockRequest>(initialRequests);
	private Layer lastLayer;
	private int requestidx = 0;
	private int iterateidx = 0;

	private float storeX, storeY;
	
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
						if(block != Blocks.air){
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

		//TODO this actually isn't necessary
		Draw.color(0, 0, 0, 0.15f);
		Graphics.flushSurface();
		Draw.color();

		Graphics.end();
		floorRenderer.beginDraw();
		floorRenderer.drawLayer(DrawLayer.walls);
		floorRenderer.endDraw();
		Graphics.begin();
		
		Arrays.sort(requests.items, 0, requestidx);
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
			Block block = req.tile.block();

			if(req.layer != lastLayer){
				if(lastLayer != null) layerEnds(lastLayer);
				layerBegins(req.layer);
			}

			if(req.layer == Layer.block){
				block.draw(req.tile);
			}else if(req.layer == block.layer){
				block.drawLayer(req.tile);
			}else if(req.layer == block.layer2){
				block.drawLayer2(req.tile);
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
			Block block = req.tile.block();

			if(req.layer == block.layer){
				block.drawLayer(req.tile);
			}else if(req.layer == block.layer2){
				block.drawLayer2(req.tile);
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

	public void clearTiles(){
		floorRenderer.clearTiles();
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

    public void drawPreview(Block block, float drawx, float drawy, float rotation, float opacity) {
        Draw.alpha(opacity);
        Draw.rect(block.name(), drawx, drawy, rotation);
    }

    public void handlePreview(Block block, float rotation, float drawx, float drawy, int tilex, int tiley) {

        if(control.input().recipe != null && state.inventory.hasItems(control.input().recipe.requirements)
                && control.input().validPlace(tilex, tiley, block) && (android || control.input().cursorNear())) {

            if(block.isMultiblock()) {
                float halfBlockWidth = (block.size * tilesize) / 2;
                float halfBlockHeight = (block.size * tilesize) / 2;
                if((storeX == 0 && storeY == 0)) {
                    storeX = drawx;
                    storeY = drawy;
                }
                if((storeX == drawx - halfBlockWidth || storeX == drawx + halfBlockWidth || storeY == drawy - halfBlockHeight || storeY == drawy + halfBlockHeight) &&
                        ((tiley - control.input().getBlockY()) % block.size != 0 || (tilex - control.input().getBlockX()) % block.size != 0)) {
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
                    Draw.rect("block-" + block.size + "x" + block.size, drawx, drawy);
                } else {
                    Draw.rect("block", drawx, drawy);
                }
            }

            drawPreview(block, drawx, drawy, rotation, opacity);

            Draw.reset();
        }
    }
}
