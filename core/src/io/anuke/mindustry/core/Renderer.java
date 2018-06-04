package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.traits.BelowLiquidTrait;
import io.anuke.mindustry.entities.effect.GroundEffectEntity;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.entities.EntityDraw;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.EffectEntity;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
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
		pixelate = true;
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
						entity.set(x, y);
						effectGroup.add(entity);

						if(data instanceof BaseEntity){
							entity.setParent((BaseEntity)data);
						}
					}else{
						GroundEffectEntity entity = Pools.obtain(GroundEffectEntity.class);
						entity.effect = effect;
						entity.color = color;
						entity.rotation = rotation;
						entity.lifetime = effect.lifetime;
						entity.id ++;
						entity.data = data;
						entity.set(x, y);
						groundEffectGroup.add(entity);
					}
				}
			}
		});

		Cursors.cursorScaling = 3;
		Cursors.outlineColor = Color.valueOf("444444");

		Cursors.arrow = Cursors.loadCursor("cursor");
		Cursors.hand = Cursors.loadCursor("hand");
		Cursors.ibeam = Cursors.loadCursor("ibar");
		Cursors.loadCustom("drill");
		Cursors.loadCustom("unload");

		clearColor = Hue.lightness(0.4f);
		clearColor.a = 1f;

		background.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

		for(Block block : Block.getAllBlocks()){
			block.load();
		}
	}

	@Override
	public void init(){
		int scale = Core.cameraScale;

        effectSurface = Graphics.createSurface(scale);
		pixelSurface = Graphics.createSurface(scale);
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
            boolean flying = players[0].isFlying();

            if(!flying){
            	setCamera(position.x, position.y);
			}

			clampCamera(-tilesize / 2f, -tilesize / 2f + 1, world.width() * tilesize - tilesize / 2f, world.height() * tilesize - tilesize / 2f);

			float prex = camera.position.x, prey = camera.position.y;
			updateShake(0.75f);

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

		EntityDraw.draw(groundEffectGroup, e -> e instanceof BelowLiquidTrait);
		EntityDraw.draw(puddleGroup);
		EntityDraw.draw(groundEffectGroup, e -> !(e instanceof BelowLiquidTrait));

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

		EntityDraw.drawWith(playerGroup, p -> true, Player::drawBuildRequests);

		drawAllTeams(true);

		EntityDraw.draw(bulletGroup);
		EntityDraw.draw(airItemGroup);
		EntityDraw.draw(effectGroup);

		overlays.drawTop();

		if(pixelate)
			Graphics.flushSurface();

		if(showPaths && debug) drawDebug();

		EntityDraw.drawWith(playerGroup, p -> !p.isLocal && !p.isDead(), Player::drawName);
		
		batch.end();
	}

	private void drawAllTeams(boolean flying){
		for(Team team : Team.values()){
			EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];
			if(group.count(p -> p.isFlying() == flying) +
					playerGroup.count(p -> p.isFlying() == flying && p.getTeam() == team) == 0 && flying) continue;

			EntityDraw.drawWith(unitGroups[team.ordinal()], u -> u.isFlying() == flying, Unit::drawUnder);
			EntityDraw.drawWith(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawUnder);

			Shaders.outline.color.set(team.color);
			Shaders.mix.color.set(Color.WHITE);

			Graphics.beginShaders(Shaders.outline);
			Graphics.shader(Shaders.mix, true);
			EntityDraw.draw(unitGroups[team.ordinal()], u -> u.isFlying() == flying);
			EntityDraw.draw(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team);
			Graphics.shader();
			blocks.drawTeamBlocks(Layer.turret, team);
			Graphics.endShaders();

			EntityDraw.drawWith(unitGroups[team.ordinal()], u -> u.isFlying() == flying, Unit::drawOver);
			EntityDraw.drawWith(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawOver);
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

		Draw.color(Color.ORANGE);
		Draw.tcolor(Color.ORANGE);

		ObjectIntMap<Tile> seen = new ObjectIntMap<>();

		for(BlockFlag flag : BlockFlag.values()){
			for(Tile tile : world.indexer().getEnemy(Team.blue, flag)){
				int index = seen.getAndIncrement(tile, 0, 1);
				Draw.tscl(0.125f);
				Draw.text(flag.name(), tile.drawx(), tile.drawy() + tile.block().size * tilesize/2f + 4 + index * 3);
				Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize/2f);
			}
		}
		Draw.tscl(fontScale);
		Draw.tcolor();

		Draw.color();
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
		for(Surface surface : Graphics.getSurfaces()){
			surface.setScale(targetscale);
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
