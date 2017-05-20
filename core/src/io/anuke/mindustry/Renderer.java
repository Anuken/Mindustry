package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Cache;
import io.anuke.ucore.graphics.Caches;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class Renderer{
	private static int chunksize = 32;
	private static GridMap<Cache> caches = new GridMap<>();
	
	public static void renderTiles(){
		int chunksx = World.width()/chunksize, chunksy = World.height()/chunksize;
		
		//render the entire map
		if(caches.size() == 0){
			
			for(int cx = 0; cx < chunksx; cx ++){
				for(int cy = 0; cy < chunksy; cy ++){
					Caches.begin(1600);
					
					for(int tilex = cx*chunksize; tilex < (cx+1)*chunksize; tilex++){
						for(int tiley = cy*chunksize; tiley < (cy+1)*chunksize; tiley++){
							World.tile(tilex, tiley).floor().drawCache(World.tile(tilex, tiley));
						}
					}
					
					caches.put(cx, cy, Caches.end());
				}
			}
		}
		
		OrthographicCamera camera = control.camera;
		
		Draw.end();
		
		int crangex = (int)(camera.viewportWidth/(chunksize*tilesize));
		int crangey = (int)(camera.viewportHeight/(chunksize*tilesize))+1;
		
		for(int x = -crangex; x <= crangex; x++){
			for(int y = -crangey; y <= crangey; y++){
				int worldx = Mathf.scl(camera.position.x, chunksize*tilesize) + x;
				int worldy = Mathf.scl(camera.position.y, chunksize*tilesize) + y;
				
				if(caches.containsKey(worldx, worldy))
					caches.get(worldx, worldy).render();
			}
		}
		
		Draw.begin();
		
		Draw.reset();
		int rangex = control.rangex, rangey = control.rangey;
		
		for(int l = 0; l < 4; l++){
			if(l == 0){
				Draw.surface("shadow");
			}
			
			for(int x = -rangex; x <= rangex; x++){
				for(int y = -rangey; y <= rangey; y++){
					int worldx = Mathf.scl(camera.position.x, tilesize) + x;
					int worldy = Mathf.scl(camera.position.y, tilesize) + y;

					if(Mathf.inBounds(worldx, worldy, tiles)){
						Tile tile = tiles[worldx][worldy];
						if(l == 0){
							if(tile.block() != Blocks.air)
								Draw.rect(tile.block().shadow, worldx * tilesize, worldy * tilesize);
						}else if(l == 1){
							tile.block().draw(tile);
						}else{
							tile.block().drawOver(tile);
						}
					}
				}
			}

			if(l == 0){
				Draw.color(0, 0, 0, 0.15f);
				Draw.flushSurface();
				Draw.color();
			}
		}
	}
	
	public static void renderPixelOverlay(){
		
		if(recipe != null && !ui.hasMouse()){
			float x = Mathf.round2(Graphics.mouseWorld().x, tilesize);
			float y = Mathf.round2(Graphics.mouseWorld().y, tilesize);

			boolean valid = World.validPlace(World.tilex(), World.tiley(), recipe.result);

			Draw.color(valid ? Color.PURPLE : Color.SCARLET);
			Draw.thickness(2f);
			Draw.square(x, y, tilesize / 2 + MathUtils.sin(Timers.time() / 6f) + 1);

			if(recipe.result.rotate){
				Draw.color("orange");
				vector.set(7, 0).rotate(rotation * 90);
				Draw.line(x, y, x + vector.x, y + vector.y);
			}
			
			Draw.thickness(1f);
			Draw.color("scarlet");
			for(Tile spawn : spawnpoints){
				Draw.dashcircle(spawn.worldx(), spawn.worldy(), enemyspawnspace);
			}

			if(valid)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
			
			Draw.reset();
		}

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && World.cursorNear()){
			Tile tile = World.cursorTile();
			if(tile.breakable() && tile.block() != ProductionBlocks.core){
				Draw.color(Color.YELLOW, Color.SCARLET, breaktime / tile.block().breaktime);
				Draw.square(tile.worldx(), tile.worldy(), 4);
				Draw.reset();
			}
		}

		if(recipe == null && !ui.hasMouse()){
			Tile tile = World.cursorTile();

			if(tile != null && tile.block() != Blocks.air){
				if(tile.entity != null)
				drawHealth(tile.entity.x, tile.entity.y, tile.entity.health, tile.entity.maxhealth);
				
				tile.block().drawPixelOverlay(tile);
			}
		}

		for(Entity entity : Entities.all()){
			if(entity instanceof DestructibleEntity && !(entity instanceof TileEntity)){
				DestructibleEntity dest = ((DestructibleEntity) entity);

				drawHealth(dest.x, dest.y, dest.health, dest.maxhealth);
			}
		}
	}
	
	public static void renderOverlay(){
		
		Tile tile = World.cursorTile();
		
		if(tile != null && tile.block() != Blocks.air){
			tile.block().drawOverlay(tile);
		}
		
	}
	
	public static void drawHealth(float x, float y, float health, float maxhealth){
		float len = 3;
		float offset = 7;
		
		float fraction = Mathf.clamp((float) health / maxhealth);
		float w = (len * 2 * fraction);
		
		Draw.thickness(3f);
		Draw.color(Color.GRAY);
		Draw.line(x - len + 1, y - offset, x + len + 1, y - offset);
		Draw.thickness(1f);
		Draw.color(Color.BLACK);
		Draw.line(x - len + 1, y - offset, x + len, y - offset);
		Draw.color(Color.RED);
		if(w >= 1)
		Draw.line(x - len + 1, y - offset, x - len + w, y - offset);
		Draw.reset();
	}
}
