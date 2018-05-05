package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.graphics.BlockRenderer;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.*;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
	private final static float shieldHitDuration = 18f;
	
	public Surface shadowSurface, shieldSurface, indicatorSurface;
	
	private int targetscale = baseCameraScale;
	private Texture background = new Texture("sprites/background.png");
	private FloatArray shieldHits = new FloatArray();
	private Array<Callable> shieldDraws = new Array<>();
	private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
	private BlockRenderer blocks = new BlockRenderer();

	public Renderer() {
		Lines.setCircleVertices(14);

		Core.cameraScale = baseCameraScale;
		Effects.setEffectProvider((name, color, x, y, rotation) -> {
			if(Settings.getBool("effects")){
				Rectangle view = rect.setSize(camera.viewportWidth, camera.viewportHeight)
						.setCenter(camera.position.x, camera.position.y);
				Rectangle pos = rect2.setSize(name.size).setCenter(x, y);
				if(view.overlaps(pos)){
					new EffectEntity(name, color, rotation).set(x, y).add(effectGroup);
				}
			}
		});

		Cursors.cursorScaling = 3;
		Cursors.outlineColor = Color.valueOf("444444");
		Cursors.arrow = Cursors.loadCursor("cursor");
		Cursors.hand = Cursors.loadCursor("hand");
		Cursors.ibeam = Cursors.loadCursor("ibar");

		clearColor = Hue.lightness(0.4f);
		clearColor.a = 1f;

		background.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
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
			camera.zoom = Mathf.lerpDelta(camera.zoom, targetzoom, 0.2f);

			if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
				camera.zoom = 1f;
				Graphics.setCameraScale(targetscale);
				control.input().resetCursor();
			}
		}else{
			camera.zoom = Mathf.lerpDelta(camera.zoom, 1f, 0.2f);
		}

		if(state.is(State.menu)){
			clearScreen();
		}else{
			boolean smoothcam = Settings.getBool("smoothcam");

			if(world.getCore() == null || world.getCore().block() == ProductionBlocks.core){
				if(!smoothcam){
					setCamera(player.x, player.y);
				}else{
					smoothCamera(player.x, player.y, mobile ? 0.3f : 0.14f);
				}
			}else{
				smoothCamera(world.getCore().worldx(), world.getCore().worldy(), 0.4f);
			}

			if(Settings.getBool("pixelate"))
				limitCamera(4f, player.x, player.y);

			float prex = camera.position.x, prey = camera.position.y;
			updateShake(0.75f);
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f + 1, world.width() * tilesize - tilesize / 2f, world.height() * tilesize - tilesize / 2f);

			float deltax = camera.position.x - prex, deltay = camera.position.y - prey;

			if(mobile){
				player.x += camera.position.x - prevx;
				player.y += camera.position.y - prevy;
			}

			float lastx = camera.position.x, lasty = camera.position.y;
			
			if(snapCamera && smoothcam && Settings.getBool("pixelate")){
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

			if(debug && !ui.chatfrag.chatOpen())
				record(); //this only does something if GdxGifRecorder is on the class path, which it usually isn't
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

		drawPadding();
		
		blocks.drawFloor();
		blocks.processBlocks();
		blocks.drawBlocks(false);

		Graphics.shader(Shaders.outline, false);
		Entities.draw(enemyGroup);
		Entities.draw(playerGroup, p -> !p.isAndroid);
		Graphics.shader();

		Entities.draw(Entities.defaultGroup());

		blocks.drawBlocks(true);

		Graphics.shader(Shaders.outline, false);
		Entities.draw(playerGroup, p -> p.isAndroid);
		Graphics.shader();

		Entities.draw(bulletGroup);
        Entities.draw(effectGroup);

		drawShield();

		drawOverlay();

		if(Settings.getBool("indicators") && showUI){
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
		control.input().resetCursor();
		camera.position.set(player.x, player.y, 0);
	}

	@Override
	public void dispose() {
		background.dispose();
	}

	public void clearTiles(){
		blocks.clearTiles();
	}

	void drawPadding(){
		float vw = world.width() * tilesize;
		float cw = camera.viewportWidth * camera.zoom;
		float ch = camera.viewportHeight * camera.zoom;
		if(vw < cw){
			batch.draw(background,
					camera.position.x + vw/2,
					Mathf.round(camera.position.y - ch/2, tilesize),
					(cw - vw) /2,
					ch + tilesize,
					0, 0,
					((cw - vw) / 2 / tilesize), -ch / tilesize + 1);

			batch.draw(background,
					camera.position.x - vw/2,
					Mathf.round(camera.position.y - ch/2, tilesize),
					-(cw - vw) /2,
					ch + tilesize,
					0, 0,
					-((cw - vw) / 2 / tilesize), -ch / tilesize + 1);
		}
	}

	void drawPlayerNames(){
		GlyphLayout layout = Pools.obtain(GlyphLayout.class);

        Draw.tscl(0.25f/2);
	    for(Player player : playerGroup.all()){
	       if(!player.isLocal && !player.isDead()){
	        	layout.setText(Core.font, player.name);
				Draw.color(0f, 0f, 0f, 0.3f);
				Draw.rect("blank", player.getDrawPosition().x, player.getDrawPosition().y + 8 - layout.height/2, layout.width + 2, layout.height + 2);
				Draw.color();
				Draw.tcolor(player.getColor());
	            Draw.text(player.name, player.getDrawPosition().x, player.getDrawPosition().y + 8);

	            if(player.isAdmin){
	            	Draw.color(player.getColor());
	            	float s = 3f;
					Draw.rect("icon-admin-small", player.getDrawPosition().x + layout.width/2f + 2 + 1, player.getDrawPosition().y + 7f, s, s);
				}
				Draw.reset();
           }
        }
		Pools.free(layout);
        Draw.tscl(fontscale);
    }

	void drawEnemyMarkers(){
		Graphics.surface(indicatorSurface);
		Draw.color(Color.RED);

		for(Enemy enemy : enemyGroup.all()) {

			if (rect.setSize(camera.viewportWidth, camera.viewportHeight).setCenter(camera.position.x, camera.position.y)
					.overlaps(enemy.hitbox.getRect(enemy.x, enemy.y))) {
				continue;
			}

			float angle = Angles.angle(camera.position.x, camera.position.y, enemy.x, enemy.y);
			float tx = Angles.trnsx(angle, Unit.dp.scl(20f));
			float ty = Angles.trnsy(angle, Unit.dp.scl(20f));
			Draw.rect("enemyarrow", camera.position.x + tx, camera.position.y + ty, angle);
		}

		Draw.color();
		Draw.alpha(0.4f);
		Graphics.flushSurface();
		Draw.color();
	}

	void drawShield(){
		if(shieldGroup.size() == 0 && shieldDraws.size == 0) return;
		
		Graphics.surface(renderer.shieldSurface, false);
		Draw.color(Color.ROYAL);
		Entities.draw(shieldGroup);
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
		if(world.getMap().name.equals("tutorial") && control.tutorial().showBlock()){
			int x = world.getCore().x + control.tutorial().getPlacePoint().x;
			int y = world.getCore().y + control.tutorial().getPlacePoint().y;
			int rot = control.tutorial().getPlaceRotation();

			Lines.stroke(1f);
			Draw.color(Color.YELLOW);
			Lines.square(x * tilesize, y * tilesize, tilesize / 2f + Mathf.sin(Timers.time(), 4f, 1f));

			Draw.color(Color.ORANGE);
			Lines.stroke(2f);
			if(rot != -1){
				Lines.lineAngle(x * tilesize, y * tilesize, rot * 90, 6);
			}
			Draw.reset();
		}

		//draw config selected block
		if(ui.configfrag.isShown()){
			Tile tile = ui.configfrag.getSelectedTile();
			Draw.color(Colors.get("accent"));
			Lines.stroke(1f);
			Lines.square(tile.drawx(), tile.drawy(),
					tile.block().width * tilesize / 2f + 1f);
			Draw.reset();
		}
		
		int tilex = control.input().getBlockX();
		int tiley = control.input().getBlockY();
		
		if(mobile){
			Vector2 vec = Graphics.world(Gdx.input.getX(0), Gdx.input.getY(0));
			tilex = Mathf.scl2(vec.x, tilesize);
			tiley = Mathf.scl2(vec.y, tilesize);
		}

		InputHandler input = control.input();

		//draw placement box
		if((input.recipe != null && state.inventory.hasItems(input.recipe.requirements) && (!ui.hasMouse() || mobile)
				&& control.input().drawPlace())){

			input.placeMode.draw(control.input().getBlockX(), control.input().getBlockY(),
					control.input().getBlockEndX(), control.input().getBlockEndY());

			Lines.stroke(1f);
			Draw.color(Color.SCARLET);
			for(SpawnPoint spawn : world.getSpawns()){
				Lines.dashCircle(spawn.start.worldx(), spawn.start.worldy(), enemyspawnspace);
			}

			if(world.getCore() != null) {
				Draw.color(Color.LIME);
				Lines.poly(world.getSpawnX(), world.getSpawnY(), 4, 6f, Timers.time() * 2f);
			}
			
			if(input.breakMode == PlaceMode.holdDelete)
				input.breakMode.draw(tilex, tiley, 0, 0);
			
		}else if(input.breakMode.delete && control.input().drawPlace()
				&& (input.recipe == null || !state.inventory.hasItems(input.recipe.requirements))
				&& (input.placeMode.delete || input.breakMode.both || !mobile)){

            if(input.breakMode == PlaceMode.holdDelete)
                input.breakMode.draw(tilex, tiley, 0, 0);
            else
				input.breakMode.draw(control.input().getBlockX(), control.input().getBlockY(),
						control.input().getBlockEndX(), control.input().getBlockEndY());
		}

		if(ui.toolfrag.confirming){
			ToolFragment t = ui.toolfrag;
			PlaceMode.areaDelete.draw(t.px, t.py, t.px2, t.py2);
		}
		
		Draw.reset();

		//draw selected block bars and info
		if(input.recipe == null && !ui.hasMouse()){
			Tile tile = world.tileWorld(Graphics.mouseWorld().x, Graphics.mouseWorld().y);

			if(tile != null && tile.block() != Blocks.air){
				Tile target = tile;
				if(tile.isLinked())
					target = tile.getLinked();

				if(showBlockDebug && target.entity != null){
					Draw.color(Color.RED);
					Lines.crect(target.drawx(), target.drawy(), target.block().width * tilesize, target.block().height * tilesize);
					Vector2 v = new Vector2();



					Draw.tcolor(Color.YELLOW);
					Draw.tscl(0.25f);
					Array<Object> arr = target.block().getDebugInfo(target);
					StringBuilder result = new StringBuilder();
					for(int i = 0; i < arr.size/2; i ++){
						result.append(arr.get(i*2));
						result.append(": ");
						result.append(arr.get(i*2 + 1));
						result.append("\n");
					}
					Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
					Draw.color(0f, 0f, 0f, 0.5f);
					Fill.rect(target.drawx(), target.drawy(), v.x, v.y);
					Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
					Draw.tscl(fontscale);
					Draw.reset();
				}

				if(Inputs.keyDown("block_info") && target.block().fullDescription != null){
					Draw.color(Colors.get("accent"));
					Lines.crect(target.drawx(), target.drawy(), target.block().width * tilesize, target.block().height * tilesize);
					Draw.color();
				}

				if(target.entity != null) {
					int bot = 0, top = 0;
					for (BlockBar bar : target.block().bars) {
						float offset = Mathf.sign(bar.top) * (target.block().height / 2f * tilesize + 3f + 4f * ((bar.top ? top : bot))) +
								(bar.top ? -1f : 0f);

						float value = bar.value.get(target);

						if(MathUtils.isEqual(value, -1f)) continue;

						drawBar(bar.color, target.drawx(), target.drawy() + offset, value);

						if (bar.top)
							top++;
						else
							bot++;
					}
				}

				target.block().drawSelect(target);
			}
		}
		
		if((!debug || showUI) && Settings.getBool("healthbars")){

			//draw entity health bars
			for(Enemy entity : enemyGroup.all()){
				drawHealth(entity);
			}

			for(Player player : playerGroup.all()){
				if(!player.isDead() && !player.isAndroid) drawHealth(player);
			}
		}
	}

	void drawHealth(SyncEntity dest){
		float x = dest.getDrawPosition().x;
		float y = dest.getDrawPosition().y;
		if(dest instanceof Player && snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate")){
			drawHealth((int) x, (int) y - 7f, dest.health, dest.maxhealth);
		}else{
			drawHealth(x, y - 7f, dest.health, dest.maxhealth);
		}
	}

	void drawHealth(float x, float y, float health, float maxhealth){
		drawBar(Color.RED, x, y, health / maxhealth);
	}
	
	//TODO optimize!
	public void drawBar(Color color, float x, float y, float finion){
		finion = Mathf.clamp(finion);

		if(finion > 0) finion = Mathf.clamp(finion + 0.2f, 0.24f, 1f);

		float len = 3;

		float w = (int) (len * 2 * finion) + 0.5f;

		x -= 0.5f;
		y += 0.5f;

		Lines.stroke(3f);
		Draw.color(Color.SLATE);
		Lines.line(x - len + 1, y, x + len + 1.5f, y);
		Lines.stroke(1f);
		Draw.color(Color.BLACK);
		Lines.line(x - len + 1, y, x + len + 0.5f, y);
		Draw.color(color);
		if(w >= 1)
			Lines.line(x - len + 1, y, x - len + w, y);
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
