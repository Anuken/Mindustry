package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Cache;
import io.anuke.ucore.graphics.Caches;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Renderer extends RendererModule{
	int targetscale = baseCameraScale;
	int chunksize = 32;
	GridMap<Cache> caches = new GridMap<>();
	
	public Renderer(){
		Core.cameraScale = baseCameraScale;
		pixelate();
		
		Draw.addSurface("shadow", Core.cameraScale);
		Shaders.create();
	}
	
	@Override
	public void update(){

		if(Core.cameraScale != targetscale){
			float targetzoom = (float)Core.cameraScale / targetscale;
			camera.zoom = Mathf.lerp(camera.zoom, targetzoom, 0.2f*Timers.delta());
			
			if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
				camera.zoom = 1f;
				Core.cameraScale = targetscale;
				camera.viewportWidth = Gdx.graphics.getWidth() / Core.cameraScale;
				camera.viewportHeight = Gdx.graphics.getHeight() / Core.cameraScale;
				
				AndroidInput.mousex = Gdx.graphics.getWidth()/2;
				AndroidInput.mousey = Gdx.graphics.getHeight()/2;
			}
		}
		
		if(GameState.is(State.menu)){
			clearScreen();
		}else{
			
			if(World.core.block() == ProductionBlocks.core){
				smoothCamera(player.x, player.y, android ? 0.3f : 0.14f);
			}else{
				smoothCamera(World.core.worldx(), World.core.worldy(), 0.4f);
			}
			
			float prex = camera.position.x, prey = camera.position.y;
			
			updateShake(0.75f);
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f, World.pixsize - tilesize / 2f, World.pixsize - tilesize / 2f);
			
			float deltax = camera.position.x - prex, deltay = camera.position.y - prey;
			
			if(android){
				player.x += camera.position.x-prevx;
				player.y += camera.position.y-prevy;
			}
			
			float lastx = camera.position.x, lasty = camera.position.y;
			
			if(android){
				camera.position.set((int)camera.position.x, (int)camera.position.y, 0);
				
				if(Gdx.graphics.getHeight()/Core.cameraScale % 2 == 1){
					camera.position.add(0, -0.5f, 0);
				}
			}
	
			drawDefault();
			
			camera.position.set(lastx - deltax, lasty - deltay, 0);
			
			if(Vars.debug){
				record();
			}
		}
	}
	
	@Override
	public void draw(){
		renderTiles();
		Entities.draw();
		renderPixelOverlay();
	}
	
	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		
		AndroidInput.mousex = Gdx.graphics.getWidth()/2;
		AndroidInput.mousey = Gdx.graphics.getHeight()/2;
		camera.position.set(player.x, player.y, 0);
	}
	
	void renderTiles(){
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
		
		OrthographicCamera camera = Core.camera;
		
		Draw.end();
		
		int crangex = (int)(camera.viewportWidth/(chunksize*tilesize))+1;
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
		int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2)+2;
		int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2)+2;
		
		boolean noshadows = Settings.getBool("noshadows");
		
		for(int l = (noshadows ? 1 : 0); l < 4; l++){
			if(l == 0){
				Draw.surface("shadow");
			}
			
			for(int x = -rangex; x <= rangex; x++){
				for(int y = -rangey; y <= rangey; y++){
					int worldx = Mathf.scl(camera.position.x, tilesize) + x;
					int worldy = Mathf.scl(camera.position.y, tilesize) + y;

					if( World.tile(worldx, worldy) != null){
						Tile tile = World.tile(worldx, worldy);
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
	
	public void clearTiles(){
		for(Cache cache : caches.values())
			cache.dispose();
		
		caches.clear();
	}
	
	void renderPixelOverlay(){
		
		if(player.recipe != null && Inventory.hasItems(player.recipe.requirements) && (!ui.hasMouse() || android)){
			float x = 0;
			float y = 0;
			
			int tilex = 0;
			int tiley = 0;
			
			if(android){
				Vector2 vec = Graphics.world(AndroidInput.mousex, AndroidInput.mousey);
				x = Mathf.round2(vec.x, tilesize);
				y = Mathf.round2(vec.y, tilesize);
				tilex = Mathf.scl2(vec.x, tilesize);
				tiley = Mathf.scl2(vec.y, tilesize);
			}else{
				x = Mathf.round2(Graphics.mouseWorld().x, tilesize);
				y = Mathf.round2(Graphics.mouseWorld().y, tilesize);
				tilex = World.tilex();
				tiley = World.tiley();
			}

			boolean valid = World.validPlace(tilex, tiley, player.recipe.result);

			Draw.color(valid ? Color.PURPLE : Color.SCARLET);
			Draw.thickness(2f);
			Draw.square(x, y, tilesize / 2 + MathUtils.sin(Timers.time() / 6f) + 1);
			
			player.recipe.result.drawPlace(tilex, tiley, valid);

			if(player.recipe.result.rotate){
				Draw.color("orange");
				Tmp.v1.set(7, 0).rotate(player.rotation * 90);
				Draw.line(x, y, x + Tmp.v1.x, y + Tmp.v1.y);
			}
			
			Draw.thickness(1f);
			Draw.color("scarlet");
			for(Tile spawn : World.spawnpoints){
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
				Draw.color(Color.YELLOW, Color.SCARLET, player.breaktime / tile.block().breaktime);
				Draw.square(tile.worldx(), tile.worldy(), 4);
				Draw.reset();
			}
		}
		
		if(android && player.breaktime > 0){
			Tile tile = AndroidInput.selected();
			if(tile.breakable() && tile.block() != ProductionBlocks.core){
				float fract = player.breaktime / tile.block().breaktime;
				Draw.color(Color.YELLOW, Color.SCARLET, fract);
				Draw.circle(tile.worldx(), tile.worldy(), 4 + (1f-fract)*26);
				Draw.reset();
			}
		}

		if(player.recipe == null && !ui.hasMouse()){
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
	
	void drawHealth(float x, float y, float health, float maxhealth){
		drawBar(Color.RED, x, y, health/maxhealth);
	}
	
	public void drawBar(Color color, float x, float y, float fraction){
		float len = 3;
		float offset = 7;
		
		float w = (int)(len * 2 * fraction) + 0.5f;
		
		x -= 0.5f;
		y += 0.5f;
		
		Draw.thickness(3f);
		Draw.color(Color.SLATE);
		Draw.line(x - len + 1, y - offset, x + len + 1.5f, y - offset);
		Draw.thickness(1f);
		Draw.color(Color.BLACK);
		Draw.line(x - len + 1, y - offset, x + len + 0.5f, y - offset);
		Draw.color(color);
		if(w >= 1)
			Draw.line(x - len + 1, y - offset, x - len + w, y - offset);
		Draw.reset();
	}
	
	public void setCameraScale(int amount){
		targetscale = amount;
		clampScale();
		Draw.getSurface("pixel").setScale(targetscale);
		Draw.getSurface("shadow").setScale(targetscale);
	}
	
	public void scaleCamera(int amount){
		setCameraScale(targetscale + amount);
	}
	
	public void clampScale(){
		targetscale = Mathf.clamp(targetscale, Math.round(Unit.dp.inPixels(3)), Math.round(Unit.dp.inPixels((5))));
	}
	
}
