package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.BelowLiquidEffect;
import io.anuke.mindustry.entities.effect.GroundEffectEntity;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
	private final static float shieldHitDuration = 18f;
	
	public Surface effectSurface;
	
	private int targetscale = baseCameraScale;
	private Texture background = new Texture("sprites/background.png");
	private FloatArray shieldHits = new FloatArray();
	private Array<Callable> shieldDraws = new Array<>();
	private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
	private Vector2 avgPosition = new Vector2();
	private BlockRenderer blocks = new BlockRenderer();
	private MinimapRenderer minimap = new MinimapRenderer();
	private OverlayRenderer overlays = new OverlayRenderer();

	public Renderer() {
		Lines.setCircleVertices(14);

		Shaders.init();

		Core.cameraScale = baseCameraScale;
		Effects.setEffectProvider((effect, color, x, y, rotation, data) -> {
			if(effect == Fx.none) return;
			if(Settings.getBool("effects")){
				Rectangle view = rect.setSize(camera.viewportWidth, camera.viewportHeight)
						.setCenter(camera.position.x, camera.position.y);
				Rectangle pos = rect2.setSize(effect.size).setCenter(x, y);

				if(view.overlaps(pos)){

					if(!(effect instanceof GroundEffect)) {
						EffectEntity entity = Pools.obtain(EffectEntity.class);
						entity.effect = effect;
						entity.color = color;
						entity.rotation = rotation;
						entity.lifetime = effect.lifetime;
						entity.data = data;
						entity.id ++;
						entity.set(x, y).add(effectGroup);

						if(data instanceof Entity){
							entity.setParent((Entity)data);
						}
					}else{
						GroundEffectEntity entity = Pools.obtain(GroundEffectEntity.class);
						entity.effect = effect;
						entity.color = color;
						entity.rotation = rotation;
						entity.lifetime = effect.lifetime;
						entity.id ++;
						entity.data = data;
						entity.set(x, y).add(groundEffectGroup);
					}
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

		for(Block block : Block.getAllBlocks()){
			block.load();
		}
	}

	@Override
	public void init(){
		pixelate = Settings.getBool("pixelate");
		int scale = Settings.getBool("pixelate") ? Core.cameraScale : 1;

        effectSurface = Graphics.createSurface(scale);
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
				for(Player player : players) {
                    control.input(player.playerIndex).resetCursor();
                }
			}
		}else{
			camera.zoom = Mathf.lerpDelta(camera.zoom, 1f, 0.2f);
		}

		if(state.is(State.menu)){
			Graphics.clear(Color.BLACK);
		}else{
            Vector2 position = averagePosition();

            setCamera(position.x, position.y);

			float prex = camera.position.x, prey = camera.position.y;
			updateShake(0.75f);
			clampCamera(-tilesize / 2f, -tilesize / 2f + 1, world.width() * tilesize - tilesize / 2f, world.height() * tilesize - tilesize / 2f);

			float deltax = camera.position.x - prex, deltay = camera.position.y - prey;

			float lastx = camera.position.x, lasty = camera.position.y;
			
			if(snapCamera){
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
		}

		if(debug && !ui.chatfrag.chatOpen()) {
			renderer.record(); //this only does something if GdxGifRecorder is on the class path, which it usually isn't
		}
	}

	@Override
	public void draw(){
		camera.update();

		Graphics.clear(clearColor);
		
		batch.setProjectionMatrix(camera.combined);
		
		if(pixelate) 
			Graphics.surface(pixelSurface, false);
		else
			batch.begin();

		drawPadding();
		
		blocks.drawFloor();

		Entities.draw(groundEffectGroup, e -> e instanceof BelowLiquidEffect);
		Entities.draw(puddleGroup);
		Entities.draw(groundEffectGroup, e -> !(e instanceof BelowLiquidEffect));

		blocks.processBlocks();
		blocks.drawBlocks(Layer.block);

		Graphics.shader(Shaders.blockbuild, false);
        blocks.drawBlocks(Layer.placement);
        Graphics.shader();

        blocks.drawBlocks(Layer.overlay);

        drawAllTeams(false);

		blocks.skipLayer(Layer.turret);
		blocks.drawBlocks(Layer.laser);

		overlays.drawBottom();

		Entities.drawWith(playerGroup, p -> true, Player::drawBuildRequests);

		drawAllTeams(true);

		Entities.draw(bulletGroup);
		Entities.draw(airItemGroup);
        Entities.draw(effectGroup);

		overlays.drawTop();

		if(pixelate)
			Graphics.flushSurface();

		if(showPaths) drawDebug();

		Entities.drawWith(playerGroup, p -> !p.isLocal && !p.isDead(), Player::drawName);
		
		batch.end();
	}

	private void drawAllTeams(boolean flying){
		for(Team team : Team.values()){
			EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];
			if(group.count(p -> p.isFlying() == flying) +
					playerGroup.count(p -> p.isFlying() == flying && p.team == team) == 0 && flying) continue;

			Entities.drawWith(unitGroups[team.ordinal()], u -> u.isFlying() == flying, Unit::drawUnder);
			Entities.drawWith(playerGroup, p -> p.isFlying() == flying && p.team == team, Unit::drawUnder);

			Shaders.outline.color.set(team.color);

			Graphics.beginShaders(Shaders.outline);
			Graphics.shader(Shaders.mix, true);
			Entities.draw(unitGroups[team.ordinal()], u -> u.isFlying() == flying);
			Entities.draw(playerGroup, p -> p.isFlying() == flying && p.team == team);
			Graphics.shader();
			blocks.drawTeamBlocks(Layer.turret, team);
			Graphics.endShaders();

			Entities.drawWith(unitGroups[team.ordinal()], u -> u.isFlying() == flying, Unit::drawOver);
			Entities.drawWith(playerGroup, p -> p.isFlying() == flying && p.team == team, Unit::drawOver);
		}
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		for(Player player : players) {
            control.input(player.playerIndex).resetCursor();
        }
		camera.position.set(players[0].x, players[0].y, 0);
	}

	@Override
	public void dispose() {
		background.dispose();
	}

	public Vector2 averagePosition(){
	    avgPosition.setZero();
	    for(Player player : players){
	        avgPosition.add(player.x, player.y);
        }
        avgPosition.scl(1f / players.length);
        return avgPosition;
    }

	public MinimapRenderer minimap() {
		return minimap;
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

	void drawDebug(){
		int rangex = (int)(Core.camera.viewportWidth/tilesize/2), rangey = (int)(Core.camera.viewportHeight/tilesize/2);
		Draw.tscl(0.125f);

		for(int x = -rangex; x <= rangex; x++) {
			for (int y = -rangey; y <= rangey; y++) {
				int worldx = Mathf.scl(camera.position.x, tilesize) + x;
				int worldy = Mathf.scl(camera.position.y, tilesize) + y;

				if(world.tile(worldx, worldy) == null) continue;

				float value = world.pathfinder().getDebugValue(worldx, worldy);
				Draw.color(Color.PURPLE);
				Draw.alpha((value % 10f) / 10f);
				Lines.square(worldx * tilesize, worldy*tilesize, 4f);
			}
		}

		Draw.color();
	}

	void drawShield(){
		if(shieldGroup.size() == 0 && shieldDraws.size == 0) return;
		
		Graphics.surface(renderer.effectSurface, false);
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

		Texture texture = effectSurface.texture();
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
		float s = io.anuke.ucore.scene.ui.layout.Unit.dp.scl(1f);
		targetscale = Mathf.clamp(targetscale, Math.round(s*2), Math.round(s*5));
	}

}
