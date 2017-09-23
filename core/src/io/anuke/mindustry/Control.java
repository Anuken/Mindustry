package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.EnemySpawn;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.TestEnemy;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.GestureHandler;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.ControlModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class Control extends ControlModule{
	int targetscale = baseCameraScale;
	
	boolean showedTutorial;
	boolean hiscore = false;
	
	final Array<Weapon> weapons = new Array<>();
	
	Array<EnemySpawn> spawns = new Array<>();
	int wave = 1;
	float wavetime;
	int enemies = 0;
	
	float respawntime;
	
	public Control(){
		String[] args = Mindustry.args;
		
		if(args.length > 0 && args[0].equals("-debug")){
			Vars.debug = true;
		}
		
		Core.cameraScale = baseCameraScale;
		pixelate();
		
		Gdx.input.setCatchBackKey(true);
		
		if(android){
			Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, new GestureHandler()));
			Inputs.addProcessor(new AndroidInput());
		}
		
		Effects.setShakeFalloff(10000f);
		
		Draw.addSurface("shadow", Core.cameraScale);
		
		atlas = new Atlas("sprites.atlas");
		
		Sounds.load("shoot.wav", "place.wav", "explosion.wav", "enemyshoot.wav", 
				"corexplode.wav", "break.wav", "spawn.wav", "flame.wav", "die.wav", 
				"respawn.wav", "purchase.wav", "flame2.wav");
		
		Sounds.setFalloff(9000f);
		
		Musics.load("1.mp3", "2.mp3", "3.mp3");
		
		World.loadMaps();
		
		KeyBinds.defaults(
			"up", Keys.W,
			"left", Keys.A,
			"down", Keys.S,
			"right", Keys.D,
			"rotate", Keys.R,
			"menu", Gdx.app.getType() == ApplicationType.Android ? Keys.BACK : Keys.ESCAPE
		);
			
		Settings.loadAll("io.anuke.moment");
		
		for(String map : maps){
			Settings.defaults("hiscore"+map, 0);
		}
		
		player = new Player();
	}
	/*
	public void setCameraScale(int scale){
		Core.cameraScale = scale;
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		setCamera(player.x, player.y);
		Draw.getSurface("pixel").setScale(Core.cameraScale);
		Draw.getSurface("shadow").setScale(Core.cameraScale);
	}
	*/
	public void reset(){
		weapons.clear();
		Renderer.clearTiles();
		
		weapons.add(Weapon.blaster);
		player.weapon = weapons.first();
		
		wave = 1;
		wavetime = waveSpacing();
		Entities.clear();
		enemies = 0;
		
		if(!android)
			player.add();
		
		player.heal();
		Inventory.clearItems();
		World.spawnpoints.clear();
		respawntime = -1;
		hiscore = false;
		
		ui.updateItems();
		ui.updateWeapons();
	}
	
	public void play(){
		Renderer.clearTiles();
		
		player.x = World.core.worldx();
		player.y = World.core.worldy() - 8f - ((int)(Gdx.graphics.getWidth() / (float)Core.cameraScale * 2) % 2 == 0 ? 0.5f : 0);
		
		control.camera.position.set(player.x, player.y, 0);
		
		//multiplying by 2 so you start with more time in the beginning
		wavetime = waveSpacing()*2;
		
		if(showedTutorial || !Settings.getBool("tutorial")){
			GameState.set(State.playing);
		}else{
			GameState.set(State.paused);
			ui.showTutorial();
			showedTutorial = true;
		}
	}
	
	public boolean hasWeapon(Weapon weapon){
		return weapons.contains(weapon, true);
	}
	
	public void addWeapon(Weapon weapon){
		weapons.add(weapon);
	}
	
	public Array<Weapon> getWeapons(){
		return weapons;
	}
	
	public void setWaveData(int enemies, int wave, float wavetime){
		this.wave = wave;
		this.wavetime = wavetime;
		this.enemies = enemies;
	}
	
	void runWave(){
		Sounds.play("spawn");
		
		Pathfind.updatePath();
		
		for(EnemySpawn spawn : spawns){
			for(int lane = 0; lane < World.spawnpoints.size; lane ++){
				Tile tile = World.spawnpoints.get(lane);
				int spawnamount = spawn.evaluate(wave, lane);
				
				for(int i = 0; i < spawnamount; i ++){
					int index = i;
					
					Timers.run(index*30f, ()->{
						try{
							Enemy enemy = (Enemy)ClassReflection.newInstance(spawn.type);
							enemy.set(tile.worldx(), tile.worldy());
							Effects.effect("spawn", enemy);
							enemy.add();
							
							enemies ++;
						}catch (Exception e){
							throw new RuntimeException(e);
						}
					});
				}
			}
		}
		
		wave ++;
		
		int last = Settings.getInt("hiscore"+maps[World.getMap()]);
		
		if(wave > last){
			Settings.putInt("hiscore"+maps[World.getMap()], wave);
			Settings.save();
			hiscore = true;
		}
		
		wavetime = waveSpacing();
	}
	
	public void enemyDeath(){
		enemies --;
	}
	
	public void coreDestroyed(){
		Effects.shake(5, 6, camera.position.x, camera.position.y);
		Sounds.play("corexplode");
		Tile core = World.core;
		for(int i = 0; i < 16; i ++){
			Timers.run(i*2, ()->{
				Effects.effect("explosion", core.worldx()+Mathf.range(40), core.worldy()+Mathf.range(40));
			});
		}
		Effects.effect("coreexplosion", core.worldx(), core.worldy());
		
		Timers.run(60, ()->{
			ui.showRestart();
		});
	}
	
	float waveSpacing(){
		int scale = Settings.getInt("difficulty");
		float out = (scale == 0 ? 2f : scale == 1f ? 1f : 0.5f);
		return wavespace*out;
	}
	
	public boolean isHighScore(){
		return hiscore;
	}
	
	public int getEnemiesRemaining(){
		return enemies;
	}
	
	public float getWaveCountdown(){
		return wavetime;
	}
	
	public float getRespawnTime(){
		return respawntime;
	}
	
	public void setRespawnTime(float respawntime){
		this.respawntime = respawntime;
	}
	
	public int getWave(){
		return wave;
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
	
	@Override
	public void init(){
		Musics.shuffleAll();
		
		Entities.initPhysics();
		
		Entities.setCollider(tilesize, (x, y)->{
			return World.solid(x, y);
		});

		EffectLoader.create();
	}
	
	@Override
	public void update(){
		
		if(debug){
			
			if(Inputs.keyUp(Keys.PLUS)){
				scaleCamera(1);
			}
			
			if(Inputs.keyUp(Keys.MINUS)){
				scaleCamera(-1);
			}
			
			if(Inputs.keyUp(Keys.SPACE))
				Effects.sound("shoot", World.core.worldx(), World.core.worldy());
			
			if(Inputs.keyUp(Keys.O)){
				Timers.mark();
				SaveIO.write(Gdx.files.local("mapsave.mins"));
				log("Save time taken: " + Timers.elapsed());
			}
			
			if(Inputs.keyUp(Keys.P)){
				Timers.mark();
				SaveIO.load(Gdx.files.local("mapsave.mins"));
				log("Load time taken: " + Timers.elapsed());
				Renderer.clearTiles();
			}
			
			if(Inputs.keyUp(Keys.C)){
				for(Entity entity : Entities.all()){
					if(entity instanceof Enemy)
						entity.remove();
				}
			}
			
			if(Inputs.keyDown(Keys.SPACE)){
				Effects.shake(6, 4, Graphics.mouseWorld().x, Graphics.mouseWorld().y);
			}
			
			if(Inputs.keyDown(Keys.Y)){
				new TestEnemy(0).set(player.x, player.y).add();
			}
		}
		
		if(Core.cameraScale != targetscale){
			float targetzoom = (float)Core.cameraScale / targetscale;
			camera.zoom = Mathf.lerp(camera.zoom, targetzoom, 0.2f*Timers.delta());
			
			if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
				camera.zoom = 1f;
				Core.cameraScale = targetscale;
				//super.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				camera.viewportWidth = Gdx.graphics.getWidth() / Core.cameraScale;
				camera.viewportHeight = Gdx.graphics.getHeight() / Core.cameraScale;
				
				AndroidInput.mousex = Gdx.graphics.getWidth()/2;
				AndroidInput.mousey = Gdx.graphics.getHeight()/2;
			}
		}
		
		if(GameState.is(State.menu)){
			clearScreen();
		}else{
			
			if(Inputs.keyUp("menu")){
				if(GameState.is(State.paused)){
					ui.hideMenu();
					GameState.set(State.playing);
				}else{
					ui.showMenu();
					GameState.set(State.paused);
				}
			}
		
			if(!GameState.is(State.paused)){
				
				if(respawntime > 0){
					
					respawntime -= delta();
					
					if(respawntime <= 0){
						player.set(World.core.worldx(), World.core.worldy()-8);
						player.heal();
						player.add();
						Effects.sound("respawn");
					}
				}
				
				if(enemies <= 0)
					wavetime -= delta();
			
				if(wavetime <= 0 || (debug && Inputs.keyUp(Keys.F))){
					runWave();
				}
			
				Entities.update();
				
				if(!android){
					Input.doInput();
				}else{
					AndroidInput.doInput();
				}
				
			}
			
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
		
		if(!GameState.is(State.paused)){
			Inputs.update();
			Timers.update();
		}
	}
	
	@Override
	public void draw(){
		Renderer.renderTiles();
		Entities.draw();
		Renderer.renderPixelOverlay();
	}
	
	@Override
	public void dispose(){
		super.dispose();
		World.disposeMaps();
	}
	
	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		
		AndroidInput.mousex = Gdx.graphics.getWidth()/2;
		AndroidInput.mousey = Gdx.graphics.getHeight()/2;
		camera.position.set(player.x, player.y, 0);
	}

}
