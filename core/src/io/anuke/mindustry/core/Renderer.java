package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.BlockRenderer;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
	private final static int chunksize = 32;
	private final static float shieldHitDuration = 18f;
	
	public Surface shadowSurface, shieldSurface, indicatorSurface;
	
	private int targetscale = baseCameraScale;
	private FloatArray shieldHits = new FloatArray();
	private Array<Callable> shieldDraws = new Array<>();
	private BlockRenderer blocks = new BlockRenderer();

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
		clearColor.a = 1f;
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

			if(Vars.debug) record(); //this only does something if GdxGifRecorder is on the class path, which it usually isn't
		}
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
		
		blocks.drawFloor();
		blocks.processBlocks();
		blocks.drawBlocks(false);

		Graphics.shader(Shaders.outline, false);
		Entities.draw(control.enemyGroup);
		Graphics.shader();

		Entities.draw(control.playerGroup);
		Entities.draw(Entities.defaultGroup());

		blocks.drawBlocks(true);
		
		Entities.draw(control.bulletGroup);

		drawShield();

		drawOverlay();

		if(Settings.getBool("indicators") && Vars.showUI){
			drawEnemyMarkers();
		}

		if(pixelate)
			Graphics.flushSurface();

		drawPlayerNames();
		
		batch.end();
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		control.input.resetCursor();
		camera.position.set(player.x, player.y, 0);
	}
	
	public void clearTiles(){
		blocks.clearTiles();
	}

	void drawPlayerNames(){
        Draw.tscl(0.25f/2);
	    for(Player player : Vars.control.playerGroup.all()){
	        if(!player.isLocal){
	            Draw.text(player.name, player.x, player.y + 6);
            }
        }
        Draw.tscl(Vars.fontscale);
    }

	void drawEnemyMarkers(){
		Graphics.surface(indicatorSurface);
		Draw.color(Color.RED);
		for(Enemy enemy : control.enemyGroup.all()){

			if(Tmp.r1.setSize(camera.viewportWidth, camera.viewportHeight).setCenter(camera.position.x, camera.position.y).overlaps(enemy.hitbox.getRect(enemy.x, enemy.y))){
				continue;
			}

			float angle = Angles.angle(camera.position.x, camera.position.y, enemy.x, enemy.y);
			Angles.translation(angle, Unit.dp.scl(20f));
			Draw.rect("enemyarrow", camera.position.x + Angles.x(), camera.position.y + Angles.y(), angle);
		}
		Draw.color();
		Draw.alpha(0.4f);
		Graphics.flushSurface();
		Draw.color();
	}

	void drawShield(){
		if(control.shieldGroup.amount() == 0 && shieldDraws.size == 0) return;
		
		Graphics.surface(Vars.renderer.shieldSurface, false);
		Draw.color(Color.ROYAL);
		Entities.draw(control.shieldGroup);
		for(Callable c : shieldDraws){
			c.run();
		}
		Draw.reset();
		Graphics.surface();
		
		for(int i = 0; i < shieldHits.size / 3; i++){
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
		shieldDraws.clear();
	}

	public BlockRenderer getBlocks() {
		return blocks;
	}

	public void addShieldHit(float x, float y){
		shieldHits.addAll(x, y, 0f);
	}

	public void addShield(Callable call){
		shieldDraws.add(call);
	}

	void drawOverlay(){

		//draw tutorial placement point
		if(Vars.world.getMap().name.equals("tutorial") && Vars.control.tutorial.showBlock()){
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

		InputHandler input = control.getInput();

		//draw placement box
		if((input.recipe != null && Vars.control.hasItems(input.recipe.requirements) && (!ui.hasMouse() || android)
				&& control.input.drawPlace())){

			input.placeMode.draw(control.input.getBlockX(), control.input.getBlockY(), control.input.getBlockEndX(), control.input.getBlockEndY());

			Draw.thickness(1f);
			Draw.color(Color.SCARLET);
			for(SpawnPoint spawn : control.getSpawnPoints()){
				Draw.dashCircle(spawn.start.worldx(), spawn.start.worldy(), enemyspawnspace);
			}
			
			if(input.breakMode == PlaceMode.holdDelete)
				input.breakMode.draw(tilex, tiley, 0, 0);
			
		}else if(input.breakMode.delete && control.input.drawPlace() && input.recipe == null){ //TODO test!
			input.breakMode.draw(control.input.getBlockX(), control.input.getBlockY(),
					control.input.getBlockEndX(), control.input.getBlockEndY());
		}

		if(Vars.ui.getTools().confirming){
			ToolFragment t = Vars.ui.getTools();
			PlaceMode.areaDelete.draw(t.px, t.py, t.px2, t.py2);
		}
		
		Draw.reset();

		//draw selected block health
		if(input.recipe == null && !ui.hasMouse()){
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
		
		if((!Vars.debug || Vars.showUI) && Settings.getBool("healthbars")){

			//draw entity health bars
			for(Enemy entity : control.enemyGroup.all()){
				drawHealth(entity);
			}

			for(Player player : Vars.control.playerGroup.all()){
				if(!player.isDead() && !player.isAndroid) drawHealth(player);
			}
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
		targetscale = Mathf.clamp(targetscale, Math.round(Unit.dp.scl(2)), Math.round(Unit.dp.scl((5))));
	}

}
