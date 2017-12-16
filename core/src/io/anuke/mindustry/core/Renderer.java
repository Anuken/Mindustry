package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.effect.Shaders;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.StaticBlock;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

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
		
		clearColor = Hue.lightness(0.4f);
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
				control.input.resetCursor();
			}
		}

		if(GameState.is(State.menu)){
			clearScreen();
		}else{
			boolean smoothcam = Settings.getBool("smoothcam");
			
			//TODO identify the source of this bug
			if(control.core == null){
				ui.showGameError();
				GameState.set(State.menu);
				return;
			}

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
			
			draw();

			camera.position.set(lastx - deltax, lasty - deltay, 0);

			record(); //this only does something if GdxGifRecorder is on the class path, which it usually isn't
		}
	}
	FrameBuffer buffer = new FrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
	
	void drawTest(){
		camera.update();
		
		clearScreen(clearColor);
		
		Core.batch.setProjectionMatrix(camera.combined);
		
		Graphics.surface(pixelSurface, false);
		
		Draw.color(1f, 1f, 1f, Mathf.absin(Timers.time(), 10f, 1f));
		Draw.rect("blank", camera.position.x, camera.position.y, camera.viewportWidth, camera.viewportHeight);
		Draw.color();
		
		Graphics.surface(shadowSurface);
		Draw.color(Color.RED);
		Draw.alpha(0.5f);
		Draw.rect("blank", camera.position.x, camera.position.y, 100, 100);
		Draw.color();
		Graphics.flushSurface();
		
		Graphics.flushSurface();
		Graphics.end();
	}
	
	void drawTest2(){
		camera.update();
		
		clearScreen(clearColor);
		Core.batch.setProjectionMatrix(camera.combined);

		Graphics.surface(pixelSurface, false);
		
		Draw.color(1f, 1f, 1f, 0.3f);
		Draw.rect("blank", camera.position.x, camera.position.y, camera.viewportWidth, camera.viewportHeight);
		Draw.color();
		
		Graphics.flushSurface();
	}

	@Override
	public void draw(){
		camera.update();
		
		clearScreen(clearColor);
		
		batch.setProjectionMatrix(camera.combined);
		
		if(pixelate) 
			Graphics.surface(pixelSurface, false);
		else
			batch.begin();
		
		//clears shield surface
		Graphics.surface(shieldSurface);
		Graphics.surface();
		
		boolean optimize = false;
		
		drawFloor();
		drawBlocks(false, optimize);

		Graphics.shader(Shaders.outline, false);
		Entities.draw(control.enemyGroup);
		Graphics.shader();

		Entities.draw(Entities.defaultGroup());
		
		if(!optimize) drawBlocks(true, false);
		
		Entities.draw(control.bulletGroup);

		drawShield();

		drawOverlay();

		if(Settings.getBool("indicators") && Vars.showUI){
			drawEnemyMarkers();
		}
		

		if(pixelate)
			Graphics.flushSurface();
		
		batch.end();
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		control.input.resetCursor();
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
		if(control.shieldGroup.amount() == 0) return;
		
		Graphics.surface(Vars.renderer.shieldSurface, false);
		Draw.color(Color.ROYAL);
		Entities.draw(control.shieldGroup);
		Draw.reset();
		Graphics.surface();
		
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
		
		if(Shaders.shield.isFallback){
			Draw.color(1f, 1f, 1f, 0.3f);
			Shaders.outline.color = Color.SKY;
			Shaders.outline.region = Tmp.tr2;
		}

		Graphics.end();
		Graphics.shader(Shaders.shield.isFallback ? Shaders.outline : Shaders.shield);
		Graphics.setScreen();

		Core.batch.draw(texture, 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());

		Graphics.shader();
		Graphics.end();
		Graphics.beginCam();
		
		Draw.color();
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

		int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
		int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

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
		
		int tilex = control.input.getBlockX();
		int tiley = control.input.getBlockY();
		
		if(Vars.android){
			Vector2 vec = Graphics.world(Gdx.input.getX(0), Gdx.input.getY(0));
			tilex = Mathf.scl2(vec.x, tilesize);
			tiley = Mathf.scl2(vec.y, tilesize);
		}

		//draw placement box
		if((player.recipe != null && Vars.control.hasItems(player.recipe.requirements) && (!ui.hasMouse() || android) 
				&& control.input.drawPlace()) || (player.placeMode.delete && Inputs.keyDown("area_delete_mode"))){

			player.placeMode.draw(control.input.getBlockX(), control.input.getBlockY(), control.input.getBlockEndX(), control.input.getBlockEndY());
			
			Draw.thickness(1f);
			Draw.color(Color.SCARLET);
			for(SpawnPoint spawn : control.getSpawnPoints()){
				Draw.dashCircle(spawn.start.worldx(), spawn.start.worldy(), enemyspawnspace);
			}
			
		}else if(player.breakMode.delete && control.input.drawPlace()){
			player.breakMode.draw(control.input.getBlockX(), control.input.getBlockY(), 
					control.input.getBlockEndX(), control.input.getBlockEndY());
		}
		
		Draw.reset();

		//draw selected block health
		if(player.recipe == null && !ui.hasMouse()){
			Tile tile = world.tile(tilex, tiley);

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
		
		if(!Vars.debug || Vars.showUI){

			//draw entity health bars
			for(Enemy entity : control.enemyGroup.all()){
				drawHealth(entity);
			}

			if(!Vars.android && Vars.showPlayer)
				drawHealth(player);
		}
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
