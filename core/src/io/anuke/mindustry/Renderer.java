package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
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
import io.anuke.ucore.graphics.FrameBufferMap;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class Renderer{
	
	public static void renderTiles(){
		Draw.clear();
		Batch batch = control.batch;
		FrameBufferMap buffers = control.buffers;
		OrthographicCamera camera = control.camera;
		int rangex = control.rangex, rangey = control.rangey;
		
		for(int l = 0; l < 4; l++){
			if(l == 1){
				batch.end();
				buffers.end("pixel");

				buffers.begin("shadow");

				batch.begin();
				Gdx.gl.glClearColor(0, 0, 0, 0);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			}
			
			for(int x = -rangex; x <= rangex; x++){
				for(int y = -rangey; y <= rangey; y++){
					int worldx = Mathf.scl(camera.position.x, tilesize) + x;
					int worldy = Mathf.scl(camera.position.y, tilesize) + y;

					if(Mathf.inBounds(worldx, worldy, tiles)){
						Tile tile = tiles[worldx][worldy];
						if(l == 1){
							if(tile.block() != Blocks.air)
								Draw.rect("shadow", worldx * tilesize, worldy * tilesize);
						}else if(l == 0 || l == 2){
							(l == 0 ? tile.floor() : tile.block()).draw(tile);
						}else{
							tile.block().drawOver(tile);
						}
					}
				}
			}

			if(l == 1){
				batch.end();
				buffers.end("shadow");
				batch.setColor(0, 0, 0, 0.15f);

				buffers.begin("pixel");

				control.drawFull("shadow");
				batch.setColor(Color.WHITE);
				batch.setProjectionMatrix(camera.combined);

				batch.begin();
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
			
			Draw.clear();
		}

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && World.cursorNear()){
			Tile tile = World.cursorTile();
			if(tile.artifical() && tile.block() != ProductionBlocks.core){
				Draw.color(Color.YELLOW, Color.SCARLET, breaktime / breakduration);
				Draw.square(tile.worldx(), tile.worldy(), 4);
				Draw.clear();
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
		
		Draw.thickness(3f);
		Draw.color(Color.GRAY);
		Draw.line(x - len + 1, y - offset, x + len + 1, y - offset);
		Draw.thickness(1f);
		Draw.color(Color.BLACK);
		Draw.line(x - len + 1, y - offset, x + len, y - offset);
		Draw.color(Color.RED);
		Draw.line(x - len + 1, y - offset, x - len + (int)(len * 2 * fraction), y - offset);
		Draw.clear();
	}
}
