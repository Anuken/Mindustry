package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.enemies.*;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.GestureHandler;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.*;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Profiler;

public class Control extends Module{
	int targetscale = baseCameraScale;
	
	public Tutorial tutorial = new Tutorial();
	boolean hiscore = false;
	
	final Array<Weapon> weapons = new Array<>();
	final ObjectMap<Item, Integer> items = new ObjectMap<>();
	
	final EntityGroup<Enemy> enemyGroup = Entities.addGroup(Enemy.class);
	final EntityGroup<TileEntity> tileGroup = Entities.addGroup(TileEntity.class, false);
	final EntityGroup<Bullet> bulletGroup = Entities.addGroup(Bullet.class);
	
	Array<EnemySpawn> spawns = new Array<>();
	int wave = 1;
	float wavetime;
	float extrawavetime;
	int enemies = 0;
	
	float respawntime;
	
	public Control(){
		if(Mindustry.args.contains("-debug", false)){
			Vars.debug = true;
		}
		
		UCore.log("Total blocks loaded: " + Block.getAllBlocks().size);
		
		for(Block block : Block.getAllBlocks()){
			block.postInit();
		}
		
		Draw.setCircleVertices(14);
		
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
		
		Musics.load("1.mp3", "2.mp3", "3.mp3", "4.mp3");
		
		World.loadMaps();
		
		KeyBinds.defaults(
			"up", Keys.W,
			"left", Keys.A,
			"down", Keys.S,
			"right", Keys.D,
			"rotate", Keys.R,
			"rotate_back", Keys.E,
			"menu", Gdx.app.getType() == ApplicationType.Android ? Keys.BACK : Keys.ESCAPE,
			"pause", Keys.SPACE
		);
		
		Settings.loadAll("io.anuke.moment");
		
		for(Map map : Map.values()){
			Settings.defaults("hiscore" + map.name(), 0);
		}
		
		player = new Player();
		
		spawns = Array.with(
			new EnemySpawn(TitanEnemy.class){{
				after = 4;
				spacing = 2;
				scaling = 5;
			}},
			new EnemySpawn(HealerEnemy.class){{
				scaling = 3;
				spacing = 2;
				after = 8;
			}},
			new EnemySpawn(Enemy.class){{
				scaling = 3;
				tierscaleback = 3;
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
				spacing = 3;
				scaling = 3;
			}},
			new EnemySpawn(EmpEnemy.class){{
				after = 19;
				spacing = 3;
				scaling = 5;
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
	}
	
	public void reset(){
		weapons.clear();
		Vars.renderer.clearTiles();
		
		weapons.add(Weapon.blaster);
		player.weapon = weapons.first();
		
		wave = 1;
		extrawavetime = maxwavespace;
		wavetime = waveSpacing();
		Entities.clear();
		enemies = 0;
		
		if(!android)
			player.add();
		
		player.heal();
		clearItems();
		World.spawnpoints.clear();
		respawntime = -1;
		hiscore = false;
		
		ui.updateItems();
		ui.updateWeapons();
	}
	
	public void play(){
		Vars.renderer.clearTiles();
		
		player.x = World.core.worldx();
		player.y = World.core.worldy() - Vars.tilesize*2 - ((int)(Gdx.graphics.getWidth() / (float)Core.cameraScale * 2) % 2 == 0 ? 0.5f : 0);
		
		Core.camera.position.set(player.x, player.y, 0);
		
		//multiplying by 2 so you start with more time in the beginning
		wavetime = waveSpacing()*2;
		
		GameState.set(State.playing);
	}
	
	public void playMap(Map map){
		Vars.ui.showLoading();
		
		Timers.run(16, ()->{
			Vars.control.reset();
			World.loadMap(map);
			Vars.control.play();
		});
		
		Timers.run(18, ()->{
			Vars.ui.hideLoading();
		});
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
		this.extrawavetime = maxwavespace;
	}
	
	public void runWave(){
		Sounds.play("spawn");
		
		Pathfind.updatePath();
		
		for(EnemySpawn spawn : spawns){
			for(int lane = 0; lane < World.spawnpoints.size; lane ++){
				int fl = lane;
				Tile tile = World.spawnpoints.get(lane);
				int spawnamount = spawn.evaluate(wave, lane);
				
				for(int i = 0; i < spawnamount; i ++){
					int index = i;
					float range = 12f;
					
					Timers.run(index*50f, ()->{
						try{
							Constructor c = ClassReflection.getConstructor(spawn.type, int.class);
							Enemy enemy = (Enemy)c.newInstance(fl);
							enemy.set(tile.worldx() + Mathf.range(range), tile.worldy() + Mathf.range(range));
							enemy.tier = spawn.tier(wave, fl);
							Effects.effect(Fx.spawn, enemy);
							enemy.add(enemyGroup);
							
							enemies ++;
						}catch (Exception e){
							throw new RuntimeException(e);
						}
					});
				}
			}
		}
		
		wave ++;
		
		int last = Settings.getInt("hiscore" + World.getMap().name());
		
		if(wave > last){
			Settings.putInt("hiscore" + World.getMap().name(), wave);
			Settings.save();
			hiscore = true;
		}
		
		wavetime = waveSpacing();
		extrawavetime = maxwavespace;
	}
	
	void printEnemies(int wave){
		int total = 0;
		for(EnemySpawn spawn : spawns){
			int spawnamount = spawn.evaluate(wave, 0);
			total += spawnamount;
			
			if(spawnamount > 0){
				UCore.log(ClassReflection.getSimpleName(spawn.type) + " t" + spawn.tier(wave, 0) + " x" + spawnamount);
			}
		}
		
		UCore.log("Total: " + total);
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
				Effects.effect(Fx.explosion, core.worldx()+Mathf.range(40), core.worldy()+Mathf.range(40));
			});
		}
		Effects.effect(Fx.coreexplosion, core.worldx(), core.worldy());
		
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
	
	public Tutorial getTutorial(){
		return tutorial;
	}
	
	public void clearItems(){
		items.clear();
		
		items.put(Item.stone, 40);
		
		if(debug){
			for(Item item : Item.values())
				items.put(item, 2000000);
		}
	}
	
	public  int getAmount(Item item){
		return items.get(item, 0);
	}
	
	public void addItem(Item item, int amount){
		items.put(item, items.get(item, 0)+amount);
		ui.updateItems();
	}
	
	public boolean hasItems(ItemStack[] items){
		for(ItemStack stack : items)
			if(!hasItem(stack))
				return false;
		return true;
	}
	
	public boolean hasItem(ItemStack req){
		return items.get(req.item, 0) >= req.amount; 
	}
	
	public void removeItem(ItemStack req){
		items.put(req.item, items.get(req.item, 0)-req.amount);
		ui.updateItems();
	}
	
	public void removeItems(ItemStack... reqs){
		for(ItemStack req : reqs)
		items.put(req.item, items.get(req.item, 0)-req.amount);
		ui.updateItems();
	}
	
	public ObjectMap<Item, Integer> getItems(){
		return items;
	}
	
	@Override
	public void init(){
		Musics.shuffleAll();
		
		Entities.initPhysics();
		
		Entities.setCollider(tilesize, (x, y)->{
			return World.solid(x, y);
		});
	}
	
	@Override
	public void update(){
		
		if(debug){
			if(Inputs.keyUp(Keys.P)){
				Effects.effect(Fx.shellsmoke, player);
				Effects.effect(Fx.shellexplosion, player);
			}
			
			if(Inputs.keyUp(Keys.C)){
				enemyGroup.clear();
			}
			
			if(Inputs.keyUp(Keys.O)){
				Vars.noclip = !Vars.noclip;
			}
			
			if(Inputs.keyUp(Keys.Y)){
				if(Inputs.keyDown(Keys.SHIFT_LEFT)){
					new HealerEnemy(0).set(player.x, player.y).add();
				}else{
					new TitanEnemy(0).set(player.x, player.y).add();
				}
			}
		}
		
		if(!GameState.is(State.menu)){
			
			if(Inputs.keyUp("pause") && (GameState.is(State.paused) || GameState.is(State.playing))){
				GameState.set(GameState.is(State.playing) ? State.paused : State.playing);
			}
			
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
						player.set(World.core.worldx(), World.core.worldy()-Vars.tilesize*2);
						player.heal();
						player.add();
						Effects.sound("respawn");
						ui.fadeRespawn(false);
					}
				}
				
				if(!tutorial.active()){
					extrawavetime -= delta();
				
					if(enemies <= 0){
						wavetime -= delta();
					}
				}else{
					tutorial.update();
				}
			
				if(wavetime <= 0 || (debug && Inputs.keyUp(Keys.F)) || extrawavetime <= 0){
					runWave();
				}
			
				Profiler.begin("entityUpdate");
				
				//TODO
				Entities.update(Entities.defaultGroup());
				Entities.update(bulletGroup);
				Entities.update(enemyGroup);
				Entities.update(tileGroup);
				
				Entities.collideGroups(enemyGroup, bulletGroup);
				Entities.collideGroups(Entities.defaultGroup(), bulletGroup);
				
				Profiler.end("entityUpdate");
			}
			
			if(!android){
				Input.doInput();
			}else{
				AndroidInput.doInput();
			}
		}
	}
	
	@Override
	public void dispose(){
		World.disposeMaps();
	}

}
