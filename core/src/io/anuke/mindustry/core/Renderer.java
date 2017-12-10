package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.effect.Shaders;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.StaticBlock;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.*;

public class Renderer extends RendererModule{
	private final static int chunksize = 32;
	private final static float shieldHitDuration = 18f;
	
	public Surface shadowSurface, shieldSurface, indicatorSurface;
	
	private int targetscale = baseCameraScale;
	private int[][][] cache;
	private FloatArray shieldHits = new FloatArray();
	private CacheBatch cbatch;

	public Renderer() {
		Core.cameraScale = baseCameraScale;
		Effects.setEffectProvider((name, color, x, y, rotation) -> {
			if(Settings.getBool("effects")){
				Rectangle view = Tmp.r1.setSize(camera.viewportWidth, camera.viewportHeight)
						.setCenter(camera.position.x, camera.position.y);
				Rectangle pos = Tmp.r2.setSize(name.size).setCenter(x, y);
				if(view.overlaps(pos)){
					new EffectEntity(name, color, rotation).set(x, y).add();
				}
			}
		});
	}

	@Override
	public void init(){
		pixelate = Settings.getBool("pixelate");
		int scale = Settings.getBool("pixelate") ? Core.cameraScale : 1;
		
		shadowSurface = Graphics.createSurface(scale);
		shieldSurface = Graphics.createSurface(scale);
		indicatorSurface = Graphics.createSurface(scale);
		pixelSurface = Graphics.createSurface(scale);
	}

	public void setPixelate(boolean pixelate){
		this.pixelate = pixelate;
	}

	@Override
	public void update(){

		if(Core.cameraScale != targetscale){
			float targetzoom = (float) Core.cameraScale / targetscale;
			camera.zoom = Mathf.lerp(camera.zoom, targetzoom, 0.2f * Timers.delta());

			if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
				camera.zoom = 1f;
				Graphics.setCameraScale(targetscale);

				AndroidInput.mousex = Gdx.graphics.getWidth() / 2;
				AndroidInput.mousey = Gdx.graphics.getHeight() / 2;
			}
		}

		if(GameState.is(State.menu)){
			clearScreen();
		}else{
			boolean smoothcam = Settings.getBool("smoothcam");

			if(control.core.block() == ProductionBlocks.core){

				if(!smoothcam){
					setCamera(player.x, player.y);
				}else{
					smoothCamera(player.x, player.y, android ? 0.3f : 0.14f);
				}
			}else{
				smoothCamera(control.core.worldx(), control.core.worldy(), 0.4f);
			}

			if(Settings.getBool("pixelate"))
				limitCamera(4f, player.x, player.y);

			float prex = camera.position.x, prey = camera.position.y;

			updateShake(0.75f);
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f + 1, world.width() * tilesize - tilesize / 2f, world.height() * tilesize - tilesize / 2f);

			float deltax = camera.position.x - prex, deltay = camera.position.y - prey;

			if(android){
				player.x += camera.position.x - prevx;
				player.y += camera.position.y - prevy;
			}

			float lastx = camera.position.x, lasty = camera.position.y;

			if(Vars.snapCamera && smoothcam && Settings.getBool("pixelate")){
				camera.position.set((int) camera.position.x, (int) camera.position.y, 0);
			}

			if(Gdx.graphics.getHeight() / Core.cameraScale % 2 == 1){
				camera.position.add(0, -0.5f, 0);
			}

			if(Gdx.graphics.getWidth() / Core.cameraScale % 2 == 1){
				camera.position.add(-0.5f, 0, 0);
			}

			Profiler.begin("draw");

			drawDefault();

			Profiler.end("draw");
			if(Profiler.updating())
				Profiler.getTimes().put("draw", Profiler.getTimes().get("draw") - Profiler.getTimes().get("blockDraw") - Profiler.getTimes().get("entityDraw"));

			if(Vars.debug && Vars.debugGL && Timers.get("profile", 60)){
				UCore.log("shaders: " + GLProfiler.shaderSwitches, "calls: " + GLProfiler.drawCalls, "bindings: " + GLProfiler.textureBindings, "vertices: " + GLProfiler.vertexCount.average);
			}

			camera.position.set(lastx - deltax, lasty - deltay, 0);

			if(Vars.debug){
				record();
			}
		}
	}

	@Override
	public void draw(){
		//clera shield surface
		Graphics.surface(shieldSurface);
		Graphics.surface();
		
		boolean optimize = false;

		Profiler.begin("blockDraw");
		drawFloor();
		drawBlocks(false, optimize);
		Profiler.end("blockDraw");

		Profiler.begin("entityDraw");

		Graphics.shader(Shaders.outline, false);
		Entities.draw(control.enemyGroup);
		Graphics.shader();

		Entities.draw(Entities.defaultGroup());
		Entities.draw(control.bulletGroup);

		Profiler.end("entityDraw");
		
		if(!optimize) drawBlocks(true, false);

		drawShield();

		drawOverlay();

		if(Settings.getBool("indicators")){
			drawEnemyMarkers();
		}
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);

		AndroidInput.mousex = Gdx.graphics.getWidth() / 2;
		AndroidInput.mousey = Gdx.graphics.getHeight() / 2;
		camera.position.set(player.x, player.y, 0);
	}

	void drawEnemyMarkers(){
		Graphics.surface(indicatorSurface);
		Draw.color(Color.RED);
		//Draw.alpha(0.6f);
		for(Enemy enemy : control.enemyGroup.all()){

			if(Tmp.r1.setSize(camera.viewportWidth, camera.viewportHeight).setCenter(camera.position.x, camera.position.y).overlaps(enemy.hitbox.getRect(enemy.x, enemy.y))){
				continue;
			}

			float angle = Angles.angle(camera.position.x, camera.position.y, enemy.x, enemy.y);
			Angles.translation(angle, Unit.dp.inPixels(20f));
			Draw.rect("enemyarrow", camera.position.x + Angles.x(), camera.position.y + Angles.y(), angle);
		}
		Draw.color();
		Draw.alpha(0.4f);
		Graphics.flushSurface();
		Draw.color();
	}

	void drawShield(){
		for(int i = 0; i < shieldHits.size / 3; i++){
			//float x = hits.get(i*3+0);
			//float y = hits.get(i*3+1);
			float time = shieldHits.get(i * 3 + 2);

			time += Timers.delta() / shieldHitDuration;
			shieldHits.set(i * 3 + 2, time);

			if(time >= 1f){
				shieldHits.removeRange(i * 3, i * 3 + 2);
				i--;
			}
		}

		Texture texture = shieldSurface.texture();
		Shaders.shield.color.set(Color.SKY);

		Tmp.tr2.setRegion(texture);
		Shaders.shield.region = Tmp.tr2;
		Shaders.shield.hits = shieldHits;

		Graphics.end();
		Graphics.shader(Shaders.shield);
		Graphics.setScreen();

		Core.batch.draw(texture, 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());

		Graphics.shader();
		Graphics.end();
		Graphics.beginCam();
	}

	public void addShieldHit(float x, float y){
		shieldHits.addAll(x, y, 0f);
	}

	void drawFloor(){
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

		Graphics.end();

		int crangex = Math.round(camera.viewportWidth * camera.zoom / (chunksize * tilesize));
		int crangey = Math.round(camera.viewportHeight * camera.zoom / (chunksize * tilesize));

		drawCache(0, crangex, crangey);

		Graphics.begin();

		Draw.reset();
		
		if(Vars.showPaths && Vars.debug){
			drawPaths();
		}

		if(Vars.debug && Vars.debugChunks){
			Draw.color(Color.YELLOW);
			Draw.thick(1f);
			for(int x = -crangex; x <= crangex; x++){
				for(int y = -crangey; y <= crangey; y++){
					int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
					int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

					if(!Mathf.inBounds(worldx, worldy, cache))
						continue;
					Draw.linerect(worldx * chunksize * tilesize, worldy * chunksize * tilesize, chunksize * tilesize, chunksize * tilesize);
				}
			}
			Draw.reset();
		}
	}
	
	void drawBlocks(boolean top, boolean optimize){
		int crangex = (int) (camera.viewportWidth / (chunksize * tilesize)) + 1;
		int crangey = (int) (camera.viewportHeight / (chunksize * tilesize)) + 1;
		
		int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2)+2;
		int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2)+2;

		boolean noshadows = Settings.getBool("noshadows");

		boolean drawTiles = Settings.getBool("drawblocks");
		
		if(!drawTiles) return;
		
		Layer[] layers = Layer.values();
		
		int start = optimize ? (noshadows ? 1 : 0) : (top ? 4 : (noshadows ? 1 : 0));
		int end = optimize ? 4 : (top ? 4 + layers.length-1 : 4);

		//0 = shadows
		//1 = cache blocks
		//2 = normal blocks
		//3+ = layers
		for(int l = start; l < end; l++){
			if(l == 0){
				Graphics.surface(shadowSurface);
			}
			
			Layer layer = l >= 3 ? layers[l - 3] : null;
			
			boolean expand = layer == Layer.power;
			int expandr = (expand ? 3 : 0);

			if(l == 1){
				Graphics.end();
				drawCache(1, crangex, crangey);
				Graphics.begin();
			}else{
				for(int x = -rangex - expandr; x <= rangex + expandr; x++){
					for(int y = -rangey - expandr; y <= rangey + expandr; y++){
						int worldx = Mathf.scl(camera.position.x, tilesize) + x;
						int worldy = Mathf.scl(camera.position.y, tilesize) + y;
						boolean expanded = (x < -rangex || x > rangex || y < -rangey || y > rangey);
						
						if(world.tile(worldx, worldy) != null){
							Tile tile = world.tile(worldx, worldy);
							if(l == 0 && !expanded){
								if(tile.block() != Blocks.air && world.isAccessible(worldx, worldy)){
									tile.block().drawShadow(tile);
								}
							}else if(!(tile.block() instanceof StaticBlock) &&
									(!expanded || tile.block().expanded)){
								if(l == 2){
									tile.block().draw(tile);
								}else if(!optimize){
									if(tile.block().layer == layer)
										 tile.block().drawLayer(tile);
									
									if(tile.block().layer2 == layer)
										 tile.block().drawLayer2(tile);
								}else if(l == 3){
									tile.block().drawLayer(tile);
									tile.block().drawLayer2(tile);
								}
							}
						}
					}
				}
			}

			if(l == 0){
				Draw.color(0, 0, 0, 0.15f);
				Graphics.flushSurface();
				Draw.color();
			}
		}
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
		cbatch.begin();
		Graphics.useBatch(cbatch);

		for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
			for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
				Tile tile = world.tile(tilex, tiley);
				if(floor){
					tile.floor().draw(tile);
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
		if(cbatch != null)
			cbatch.dispose();
		cbatch = new CacheBatch(256 * 256 * 3);
	}
	
	void drawPaths(){
		Draw.color(Color.RED);
		for(SpawnPoint point : control.spawnpoints){
			if(point.pathTiles != null){
				for(int i = 1; i < point.pathTiles.length; i ++){
					Draw.line(point.pathTiles[i-1].worldx(), point.pathTiles[i-1].worldy(), 
							point.pathTiles[i].worldx(), point.pathTiles[i].worldy());
					Draw.circle(point.pathTiles[i-1].worldx(), point.pathTiles[i-1].worldy(), 6f);
				}
			}
		}
		Draw.reset();
	}

	void drawOverlay(){

		//draw tutorial placement point
		if(Vars.control.tutorial.showBlock()){
			int x = control.core.x + Vars.control.tutorial.getPlacePoint().x;
			int y = control.core.y + Vars.control.tutorial.getPlacePoint().y;
			int rot = Vars.control.tutorial.getPlaceRotation();

			Draw.thick(1f);
			Draw.color(Color.YELLOW);
			Draw.square(x * tilesize, y * tilesize, tilesize / 2f + Mathf.sin(Timers.time(), 4f, 1f));

			Draw.color(Color.ORANGE);
			Draw.thick(2f);
			if(rot != -1){
				Draw.lineAngle(x * tilesize, y * tilesize, rot * 90, 6);
			}
			Draw.reset();
		}

		//draw placement box
		if(player.recipe != null && Vars.control.hasItems(player.recipe.requirements) && (!ui.hasMouse() || android) && AndroidInput.mode == PlaceMode.cursor){
			float x = 0;
			float y = 0;

			int tilex = 0;
			int tiley = 0;

			if(android){
				Vector2 vec = Graphics.world(AndroidInput.mousex, AndroidInput.mousey);
				tilex = Mathf.scl2(vec.x, tilesize);
				tiley = Mathf.scl2(vec.y, tilesize);
			}else{
				tilex = Input.tilex();
				tiley = Input.tiley();
			}

			x = tilex * tilesize;
			y = tiley * tilesize;

			boolean valid = world.validPlace(tilex, tiley, player.recipe.result) && (android || Input.cursorNear());

			Vector2 offset = player.recipe.result.getPlaceOffset();

			float si = MathUtils.sin(Timers.time() / 6f) + 1;

			Draw.color(valid ? Color.PURPLE : Color.SCARLET);
			Draw.thickness(2f);
			Draw.linecrect(x + offset.x, y + offset.y, tilesize * player.recipe.result.width + si, tilesize * player.recipe.result.height + si);

			player.recipe.result.drawPlace(tilex, tiley, player.rotation, valid);
			Draw.thickness(2f);

			if(player.recipe.result.rotate){
				Draw.color("orange");
				Tmp.v1.set(7, 0).rotate(player.rotation * 90);
				Draw.line(x, y, x + Tmp.v1.x, y + Tmp.v1.y);
			}

			Draw.thickness(1f);
			Draw.color("scarlet");
			for(SpawnPoint spawn : control.getSpawnPoints()){
				Draw.dashCircle(spawn.start.worldx(), spawn.start.worldy(), enemyspawnspace);
			}

			if(valid)
				Cursors.setHand();
			else
				Cursors.restoreCursor();

			Draw.reset();
		}

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && world.validBreak(Input.tilex(), Input.tiley())){
			Tile tile = world.tile(Input.tilex(), Input.tiley());
			if(tile.isLinked())
				tile = tile.getLinked();
			Vector2 offset = tile.block().getPlaceOffset();

			Draw.color(Color.YELLOW, Color.SCARLET, player.breaktime / tile.getBreakTime());
			Draw.linecrect(tile.worldx() + offset.x, tile.worldy() + offset.y, tile.block().width * Vars.tilesize, tile.block().height * Vars.tilesize);
			Draw.reset();
		}else if(android && player.breaktime > 0){ //android block breaking
			Vector2 vec = Graphics.world(Gdx.input.getX(0), Gdx.input.getY(0));

			if(world.validBreak(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize))){
				Tile tile = world.tile(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize));

				float fract = player.breaktime / tile.getBreakTime();
				Draw.color(Color.YELLOW, Color.SCARLET, fract);
				Draw.circle(tile.worldx(), tile.worldy(), 4 + (1f - fract) * 26);
				Draw.reset();
			}
		}

		//draw selected block health
		if(player.recipe == null && !ui.hasMouse()){
			Tile tile = world.tile(Input.tilex(), Input.tiley());

			if(tile != null && tile.block() != Blocks.air){
				Tile target = tile;
				if(tile.isLinked())
					target = tile.getLinked();

				Vector2 offset = target.block().getPlaceOffset();

				if(target.entity != null)
					drawHealth(target.entity.x + offset.x, target.entity.y - 3f - target.block().height / 2f * Vars.tilesize + offset.y, target.entity.health, target.entity.maxhealth);

				target.block().drawSelect(target);
			}
		}

		//draw entity health bars
		for(Enemy entity : control.enemyGroup.all()){
			drawHealth(entity);
		}

		if(!Vars.android && Vars.showPlayer)
			drawHealth(player);
	}

	void drawHealth(DestructibleEntity dest){
		if(dest instanceof Player && Vars.snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate")){
			drawHealth((int) dest.x, (int) dest.y - 7f, dest.health, dest.maxhealth);
		}else{
			drawHealth(dest.x, dest.y - 7f, dest.health, dest.maxhealth);
		}
	}

	void drawHealth(float x, float y, float health, float maxhealth){
		drawBar(Color.RED, x, y, health / maxhealth);
	}
	
	//TODO optimize!
	public void drawBar(Color color, float x, float y, float fraction){
		float len = 3;

		float w = (int) (len * 2 * fraction) + 0.5f;

		x -= 0.5f;
		y += 0.5f;

		Draw.thickness(3f);
		Draw.color(Color.SLATE);
		Draw.line(x - len + 1, y, x + len + 1.5f, y);
		Draw.thickness(1f);
		Draw.color(Color.BLACK);
		Draw.line(x - len + 1, y, x + len + 0.5f, y);
		Draw.color(color);
		if(w >= 1)
			Draw.line(x - len + 1, y, x - len + w, y);
		Draw.reset();
	}

	public void setCameraScale(int amount){
		targetscale = amount;
		clampScale();
		//scale up all surfaces in preparation for the zoom
		if(Settings.getBool("pixelate")){
			for(Surface surface : Graphics.getSurfaces()){
				surface.setScale(targetscale);
			}
		}
	}

	public void scaleCamera(int amount){
		setCameraScale(targetscale + amount);
	}

	public void clampScale(){
		targetscale = Mathf.clamp(targetscale, Math.round(Unit.dp.inPixels(2)), Math.round(Unit.dp.inPixels((5))));
	}

}
