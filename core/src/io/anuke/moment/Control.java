package io.anuke.moment;

import static io.anuke.moment.world.TileType.tilesize;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import io.anuke.moment.ai.Pathfind;
import io.anuke.moment.entities.FlameEnemy;
import io.anuke.moment.entities.TileEntity;
import io.anuke.moment.resource.ItemStack;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.style.Styles;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class Control extends RendererModule<Moment>{
	//GifRecorder recorder = new GifRecorder(batch);
	int rangex = 10, rangey = 10;
	float breaktime = 0;
	float breakdur = 50;

	public Control() {
		atlas = new Atlas("moment.atlas");
		cameraScale = 4f;
		setPixelation();
		buffers.add("shadow", (int) (Gdx.graphics.getWidth() / cameraScale), (int) (Gdx.graphics.getHeight() / cameraScale));
	}

	@Override
	public void init(){
		DrawContext.font = Styles.styles.font();

		Entities.initPhysics(0, 0, main.pixsize, main.pixsize);

		Effect.addDraw("place", 16, e -> {
			Draw.thickness(3f - e.ifract() * 2f);
			Draw.square(e.x, e.y, TileType.tilesize / 2 + e.ifract() * 3f);
			Draw.clear();
		});

		Effect.addDraw("spark", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.GRAY, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 8);
			Draw.clear();
		});
		
		Effect.addDraw("smelt", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.YELLOW, Color.RED, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 8);
			Draw.clear();
		});

		Effect.addDraw("break", 12, e -> {
			Draw.thickness(2f);
			Draw.color(Color.WHITE, Color.GRAY, e.ifract());
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 5);
			Draw.clear();
		});

		Effect.addDraw("hit", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.ORANGE, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 3f, 2, 8);
			Draw.clear();
		});

		Effect.addDraw("explosion", 15, e -> {
			Draw.thickness(2f);
			Draw.color(Hue.mix(Color.ORANGE, Color.GRAY, e.ifract()));
			Draw.spikes(e.x, e.y, 2f + e.ifract() * 3f, 4, 6);
			Draw.circle(e.x, e.y, 3f + e.ifract() * 3f);
			Draw.clear();
		});
		
		Effect.addDraw("coreexplosion", 13, e -> {
			Draw.thickness(3f-e.ifract()*2f);
			Draw.color(Hue.mix(Color.ORANGE, Color.WHITE, e.ifract()));
			Draw.spikes(e.x, e.y, 5f + e.ifract() * 40f, 6, 6);
			Draw.circle(e.x, e.y, 4f + e.ifract() * 40f);
			Draw.clear();
		});
		
		Effect.addDraw("spawn", 23, e -> {
			Draw.thickness(2f);
			Draw.color(Hue.mix(Color.DARK_GRAY, Color.SCARLET, e.ifract()));
			Draw.circle(e.x, e.y, 7f - e.ifract() * 6f);
			Draw.clear();
		});

		Effect.addDraw("ind", 100, e -> {
			Draw.thickness(3f);
			Draw.color("royal");
			Draw.circle(e.x, e.y, 3);
			Draw.clear();
		});
		
		Effect.addDraw("respawn", main.respawntime, e -> {
			Draw.tcolor(Color.SCARLET);
			Draw.tscl(0.25f);
			Draw.text("Respawning in " + (int)((e.lifetime-e.time)/60), e.x, e.y);
			Draw.tscl(0.5f);
			Draw.clear();
		});

		Pathfind.updatePath();
	}

	public void tryMove(SolidEntity e, float x, float y){
		e.getBoundingBox(Rectangle.tmp);
		Rectangle.tmp.setSize(4);

		if(!overlaps(Rectangle.tmp, e.x + x, e.y)){
			e.x += x;
		}

		if(!overlaps(Rectangle.tmp, e.x, e.y + y)){
			e.y += y;
		}
	}

	boolean overlaps(Rectangle rect, float x, float y){
		int r = 1;
		rect.setCenter(x, y);
		int tilex = Mathf.scl2(x, tilesize);
		int tiley = Mathf.scl2(y, tilesize);

		for(int dx = -r; dx <= r; dx++){
			for(int dy = -r; dy <= r; dy++){
				Tile tile = main.tile(tilex + dx, tiley + dy);
				if(tile != null && tile.block().solid && Rectangle.tmp2.setSize(tilesize).setCenter(tile.worldx(), tile.worldy()).overlaps(rect)){
					return true;
				}
			}
		}
		return false;
	}

	Rectangle getRect(int x, int y){
		return Rectangle.tmp2.setSize(tilesize).setCenter(x * tilesize, y * tilesize);
	}

	void input(){

		if(UInput.keyUp("rotate"))
			main.rotation++;

		main.rotation %= 4;

		if(main.recipe != null && !main.hasItems(main.recipe.requirements)){
			main.recipe = null;
			Cursors.restoreCursor();
		}
		
		//TODO
		if(UInput.keyUp(Keys.G))
			new FlameEnemy(0).set(main.player.x, main.player.y).add();

		if(UInput.buttonUp(Buttons.LEFT) && main.recipe != null && validPlace(tilex(), tiley(), main.recipe.result) && !get(UI.class).hasMouse()){
			Tile tile = main.tile(tilex(), tiley());
			if(tile == null)
				return; //just in ase

			tile.setBlock(main.recipe.result);
			tile.rotation = main.rotation;

			Pathfind.updatePath();

			Effects.effect("place", roundx(), roundy());
			Effects.shake(2f, 2f);

			for(ItemStack stack : main.recipe.requirements){
				main.removeItem(stack);
			}

			if(!main.hasItems(main.recipe.requirements)){
				main.recipe = null;
				Cursors.restoreCursor();
			}
		}

		if(main.recipe != null && UInput.buttonUp(Buttons.RIGHT)){
			main.recipe = null;
			Cursors.restoreCursor();
		}

		//block breaking
		if(UInput.buttonDown(Buttons.RIGHT) && cursorNear() && main.tile(tilex(), tiley()).artifical()
				&& main.tile(tilex(), tiley()).block() != TileType.core){
			Tile tile = main.tile(tilex(), tiley());
			breaktime += delta();
			if(breaktime >= breakdur){
				Effects.effect("break", tile.entity);
				Effects.shake(3f, 1f);
				tile.setBlock(TileType.air);
				Pathfind.updatePath();
				breaktime = 0f;
			}
		}else{
			breaktime = 0f;
		}

	}

	float roundx(){
		return Mathf.round2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	float roundy(){
		return Mathf.round2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}

	int tilex(){
		return Mathf.scl2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	int tiley(){
		return Mathf.scl2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}

	boolean validPlace(int x, int y, TileType type){

		if(!cursorNear())
			return false;
		
		for(Tile spawn : main.spawnpoints){
			if(Vector2.dst(x * tilesize, y * tilesize, spawn.worldx(), spawn.worldy()) < main.spawnspace){
				return false;
			}
		}

		for(SolidEntity e : Entities.getNearby(x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle.tmp.setSize(e.hitsize);
			Rectangle.tmp.setCenter(e.x, e.y);

			if(getRect(x, y).overlaps(Rectangle.tmp)){
				return false;
			}
		}
		return main.tile(x, y).block() == TileType.air;
	}

	boolean cursorNear(){
		return Vector2.dst(main.player.x, main.player.y, tilex() * tilesize, tiley() * tilesize) <= main.placerange;
	}

	@Override
	public void update(){
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE) && Gdx.app.getType() == ApplicationType.Desktop)
			Gdx.app.exit();
		
		if(!main.playing){
			clearScreen();
			return;
		}
		
		if(!main.paused)
		Entities.update();

		input();
		if(main.core.block() == TileType.core)
			camera.position.set(main.player.x, main.player.y, 0f);
		else
			camera.position.set(main.core.worldx(), main.core.worldy(), 0f);
		clampCamera(-tilesize / 2f, -tilesize / 2f, main.pixsize - tilesize / 2f, main.pixsize - tilesize / 2f);

		drawDefault();

		//recorder.update();
	}

	@Override
	public void draw(){
		Draw.clear();

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

					if(Mathf.inBounds(worldx, worldy, main.tiles)){
						Tile tile = main.tiles[worldx][worldy];
						if(l == 1){
							if(tile.block() != TileType.air)
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

				drawFull("shadow");
				batch.setColor(Color.WHITE);
				batch.setProjectionMatrix(camera.combined);

				batch.begin();
			}
		}

		Entities.draw();

		if(main.recipe != null && !get(UI.class).hasMouse()){
			float x = Mathf.round2(UGraphics.mouseWorldPos().x, tilesize);
			float y = Mathf.round2(UGraphics.mouseWorldPos().y, tilesize);

			boolean valid = validPlace(tilex(), tiley(), main.recipe.result);

			Draw.color(valid ? Color.PURPLE : Color.SCARLET);
			Draw.thickness(2f);
			Draw.square(x, y, TileType.tilesize / 2 + MathUtils.sin(Timers.time() / 6f) + 1);

			if(main.recipe.result.rotate){
				Draw.color("orange");
				vector.set(7, 0).rotate(main.rotation * 90);
				Draw.line(x, y, x + vector.x, y + vector.y);
			}
			
			Draw.thickness(1f);
			Draw.color("scarlet");
			for(Tile spawn : main.spawnpoints){
				Draw.dashcircle(spawn.worldx(), spawn.worldy(), main.spawnspace);
			}

			if(valid)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
			
			Draw.clear();
		}

		//block breaking
		if(UInput.buttonDown(Buttons.RIGHT) && cursorNear()){
			Tile tile = main.tile(tilex(), tiley());
			if(tile.artifical() && tile.block() != TileType.core){
				Draw.color(Color.YELLOW, Color.SCARLET, breaktime / breakdur);
				Draw.square(tile.worldx(), tile.worldy(), 4);
				Draw.clear();
			}
		}

		if(main.recipe == null && !get(UI.class).hasMouse()){
			Tile tile = main.tiles[tilex()][tiley()];

			if(tile.block() != TileType.air){
				if(tile.block().name().contains("turret")){
					Draw.color("green");
					Draw.dashcircle(tile.worldx(), tile.worldy(), tile.block().range);
					Draw.clear();
				}
				if(tile.entity != null)
					drawHealth(tile.entity);
			}
		}

		for(Entity entity : Entities.all()){
			if(entity instanceof DestructibleEntity && !(entity instanceof TileEntity)){
				DestructibleEntity dest = ((DestructibleEntity) entity);

				drawHealth(dest);
			}
		}

		//Draw.text(Gdx.graphics.getFramesPerSecond() + " FPS", main.player.x, main.player.y);
	}

	void drawHealth(DestructibleEntity dest){
		float len = 3;
		float offset = 7;
		
		Draw.thickness(3f);
		Draw.color(Color.GRAY);
		Draw.line(dest.x - len + 1, dest.y - offset, dest.x + len + 1, dest.y - offset);
		Draw.thickness(1f);
		Draw.color(Color.BLACK);
		Draw.line(dest.x - len + 1, dest.y - offset, dest.x + len, dest.y - offset);
		Draw.color(Color.RED);
		Draw.line(dest.x - len + 1, dest.y - offset, dest.x - len + (int)(len * 2 * ((float) dest.health / dest.maxhealth)), dest.y - offset);
		Draw.clear();
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);

		buffers.remove("shadow");
		buffers.add("shadow", (int) (Gdx.graphics.getWidth() / cameraScale), (int) (Gdx.graphics.getHeight() / cameraScale));

		rangex = (int) (width / tilesize / cameraScale/2)+1;
		rangey = (int) (height / tilesize / cameraScale/2)+1;
	}
}
