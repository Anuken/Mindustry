package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.EnemySpawn;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.*;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.GestureHandler;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;

public class Control extends Module{
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
		
		Gdx.input.setCatchBackKey(true);
		
		if(android){
			Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, new GestureHandler()));
			Inputs.addProcessor(new AndroidInput());
		}
		
		Effects.setShakeFalloff(10000f);
		
		Core.atlas = new Atlas("sprites.atlas");
		
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
		
		spawns = Array.with(
				
			new EnemySpawn(Enemy.class){{
				scaling = 2;
				tierscaleback = 4;
			}},
			new EnemySpawn(FastEnemy.class){{
				after = 2;
				scaling = 3;
			}},
			new EnemySpawn(FlamerEnemy.class){{
				after = 14;
				spacing = 5;
				scaling = 2;
			}},
			new EnemySpawn(BlastEnemy.class){{
				after = 12;
				spacing = 2;
				scaling = 3;
			}},
			new EnemySpawn(RapidEnemy.class){{
				after = 7;
				spacing = 5;
			}},
			new EnemySpawn(TankEnemy.class){{
				after = 4;
				spacing = 2;
				scaling = 3;
			}},
			new EnemySpawn(MortarEnemy.class){{
				after = 20;
				spacing = 3;
				scaling = 5;
			}}
			
		);
		
		for(int i = 1; i < 60; i ++){
			UCore.log("\n\n--WAVE " + i);
			printEnemies(i);
		}
		
		
	}
	
	public void reset(){
		weapons.clear();
		Vars.renderer.clearTiles();
		
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
		Vars.renderer.clearTiles();
		
		player.x = World.core.worldx();
		player.y = World.core.worldy() - 8f - ((int)(Gdx.graphics.getWidth() / (float)Core.cameraScale * 2) % 2 == 0 ? 0.5f : 0);
		
		Core.camera.position.set(player.x, player.y, 0);
		
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
				int fl = lane;
				Tile tile = World.spawnpoints.get(lane);
				int spawnamount = spawn.evaluate(wave, lane);
				
				for(int i = 0; i < spawnamount; i ++){
					int index = i;
					
					Timers.run(index*50f, ()->{
						try{
							Constructor c = ClassReflection.getConstructor(spawn.type, int.class);
							Enemy enemy = (Enemy)c.newInstance(fl);
							enemy.set(tile.worldx(), tile.worldy());
							enemy.tier = spawn.tier(wave, fl);
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
	
	void printEnemies(int wave){
		for(EnemySpawn spawn : spawns){
			int spawnamount = spawn.evaluate(wave, 0);
			
			if(spawnamount > 0){
				UCore.log(ClassReflection.getSimpleName(spawn.type) + " t" + spawn.tier(wave, 0) + " x" + spawnamount);
			}
		}
	}
	
	public void enemyDeath(){
		enemies --;
	}
	
	public void coreDestroyed(){
		Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
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
				Vars.renderer.clearTiles();
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
		
		
		if(!GameState.is(State.menu)){
			
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
						ui.fadeRespawn(false);
					}
				}
				
				if(enemies <= 0){
					wavetime -= delta();
				}
			
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
		}
	}
	
	@Override
	public void dispose(){
		World.disposeMaps();
	}

}
