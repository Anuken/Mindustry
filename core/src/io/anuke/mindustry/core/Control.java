package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;

import java.util.Arrays;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.FortressEnemy;
import io.anuke.mindustry.entities.enemies.HealerEnemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.*;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.InputProxy;
import io.anuke.ucore.util.Mathf;

public class Control extends Module{
	Tutorial tutorial = new Tutorial();
	boolean hiscore = false;
	
	final Array<Weapon> weapons = new Array<>();
	final int[] items = new int[Item.getAllItems().size];
	
	public final EntityGroup<Enemy> enemyGroup = Entities.addGroup(Enemy.class);
	public final EntityGroup<TileEntity> tileGroup = Entities.addGroup(TileEntity.class, false);
	public final EntityGroup<Bullet> bulletGroup = Entities.addGroup(Bullet.class);
	public final EntityGroup<Shield> shieldGroup = Entities.addGroup(Shield.class);
	
	Array<EnemySpawn> spawns;
	int wave = 1;
	int lastUpdated = -1;
	float wavetime;
	float extrawavetime;
	int enemies = 0;
	GameMode mode = GameMode.waves;
	
	Tile core;
	Array<SpawnPoint> spawnpoints = new Array<>();
	boolean shouldUpdateItems = false;
	boolean wasPaused = false;
	
	float respawntime;
	InputHandler input;

    private InputProxy proxy;
    private float controlx, controly;
    private boolean controlling;
	
	public Control(){
		if(Mindustry.args.contains("-debug", false))
			Vars.debug = true;

		Inputs.useControllers(false);
		
		log("Total blocks loaded: " + Block.getAllBlocks().size);
		
		for(Block block : Block.getAllBlocks()){
			block.postInit();
		}
		
		Draw.setCircleVertices(14);
		
		Gdx.input.setCatchBackKey(true);
		
		if(android){
			input = new AndroidInput();
		}else{
			input = new DesktopInput();
		}

        proxy = new InputProxy(Gdx.input){
            @Override
            public int getY() {
                return controlling ? (int)controly : input.getY();
            }

            @Override
            public int getX() {
                return controlling ? (int)controlx : input.getX();
            }

			@Override
			public int getY(int pointer) {
				return pointer == 0 ? getY() : super.getY(pointer);
			}

			@Override
			public int getX(int pointer) {
				return pointer == 0 ? getX() : super.getX(pointer);
			}
        };
		
		Inputs.addProcessor(input);
		
		Effects.setShakeFalloff(10000f);
		
		Core.atlas = new Atlas("sprites.atlas");
		
		Sounds.load("shoot.ogg", "place.ogg", "explosion.ogg", "enemyshoot.ogg", 
				"corexplode.ogg", "break.ogg", "spawn.ogg", "flame.ogg", "die.ogg", 
				"respawn.ogg", "purchase.ogg", "flame2.ogg", "bigshot.ogg", "laser.ogg", "lasershot.ogg",
				"ping.ogg", "tesla.ogg", "waveend.ogg", "railgun.ogg", "blast.ogg", "bang2.ogg");
		
		Sounds.setFalloff(9000f);
		
		Musics.load("1.ogg", "2.ogg", "3.ogg", "4.ogg");
		
		KeyBinds.defaults(
				"move_x", new Axis(Input.A, Input.D),
				"move_y", new Axis(Input.S, Input.W),
				"select", Input.MOUSE_LEFT,
				"break", Input.MOUSE_RIGHT,
				"shoot", Input.MOUSE_LEFT,
				"zoom_hold", Input.CONTROL_LEFT,
				"zoom", new Axis(Input.SCROLL),
				"menu", Gdx.app.getType() == ApplicationType.Android ? Input.BACK : Input.ESCAPE,
				"pause", Input.SPACE,
				"dash", Input.SHIFT_LEFT,
				"rotate_alt", new Axis(Input.R, Input.E),
				"rotate", new Axis(Input.SCROLL),
				"weapon_1", Input.NUM_1,
				"weapon_2", Input.NUM_2,
				"weapon_3", Input.NUM_3,
				"weapon_4", Input.NUM_4,
				"weapon_5", Input.NUM_5,
				"weapon_6", Input.NUM_6
		);

		KeyBinds.defaults(
				DeviceType.controller,
				"move_x", new Axis(Input.CONTROLLER_L_STICK_HORIZONTAL_AXIS),
				"move_y", new Axis(Input.CONTROLLER_L_STICK_VERTICAL_AXIS),
				"cursor_x", new Axis(Input.CONTROLLER_R_STICK_HORIZONTAL_AXIS),
				"cursor_y", new Axis(Input.CONTROLLER_R_STICK_VERTICAL_AXIS),
				"select", Input.CONTROLLER_R_BUMPER,
				"break", Input.CONTROLLER_L_BUMPER,
				"shoot", Input.CONTROLLER_R_TRIGGER,
				"zoom_hold", Input.ANY_KEY,
				"zoom", new Axis(Input.CONTROLLER_DPAD_DOWN, Input.CONTROLLER_DPAD_UP),
				"menu", Input.CONTROLLER_X,
				"pause", Input.CONTROLLER_L_TRIGGER,
				"dash", Input.CONTROLLER_Y,
				"rotate_alt", new Axis(Input.UNSET),
				"rotate", new Axis(Input.CONTROLLER_A, Input.CONTROLLER_B),
				"weapon_1", Input.NUM_1,
				"weapon_2", Input.NUM_2,
				"weapon_3", Input.NUM_3,
				"weapon_4", Input.NUM_4,
				"weapon_5", Input.NUM_5,
				"weapon_6", Input.NUM_6
		);
		
		for(int i = 0; i < Vars.saveSlots; i ++){
			Settings.defaults("saveslot" + i, "empty");
		}
		
		Settings.loadAll("io.anuke.moment");
		
		for(Map map : Vars.world.maps().list()){
			Settings.defaults("hiscore" + map.name, 0);
		}
		
		player = new Player();
		
		spawns = WaveCreator.getSpawns();
		//WaveCreator.testWaves(1, 30);
	}

	public boolean showCursor(){
		return controlling;
	}
	
	public void reset(){
		weapons.clear();
		
		weapons.add(Weapon.blaster);
		player.weapon = weapons.first();
		
		lastUpdated = -1;
		wave = 1;
		extrawavetime = maxwavespace;
		wavetime = waveSpacing();
		Entities.clear();
		enemies = 0;
		
		if(!android)
			player.add();
		
		player.heal();
		clearItems();
		spawnpoints.clear();
		respawntime = -1;
		hiscore = false;
		
		for(Block block : Block.getAllBlocks()){
			block.onReset();
		}
		
		ui.updateItems();
		ui.updateWeapons();
	}
	
	public void play(){
		if(core == null) return;
		renderer.clearTiles();
		
		player.x = core.worldx();
		player.y = core.worldy() - Vars.tilesize*2;
		
		Core.camera.position.set(player.x, player.y, 0);
		
		//multiplying by 2 so you start with more time in the beginning
		wavetime = waveSpacing()*2;
		
		if(mode.infiniteResources){
			Arrays.fill(items, 999999999);
		}
		
		ui.updateItems();
		
		GameState.set(State.playing);
	}
	
	public Tile getCore(){
		return core;
	}
	
	public Array<SpawnPoint> getSpawnPoints(){
		return spawnpoints;
	}
	
	public void setCore(Tile tile){
		this.core = tile;
	}
	
	public InputHandler getInput(){
		return input;
	}
	
	public void addSpawnPoint(Tile tile){
		SpawnPoint point = new SpawnPoint();
		point.start = tile;
		spawnpoints.add(point);
	}
	
	public void playMap(Map map){
		Vars.ui.showLoading();
		
		Timers.run(16, ()->{
			reset();
			world.loadMap(map);
			play();
		});
		
		Timers.run(18, ()-> ui.hideLoading());
	}
	
	public GameMode getMode(){
		return mode;
	}
	
	public void setMode(GameMode mode){
		this.mode = mode;
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
		
		if(lastUpdated < wave + 1){
			world.pathfinder().updatePath();
			lastUpdated = wave + 1;
		}
		
		for(EnemySpawn spawn : spawns){
			for(int lane = 0; lane < spawnpoints.size; lane ++){
				int fl = lane;
				Tile tile = spawnpoints.get(lane).start;
				int spawnamount = spawn.evaluate(wave, lane);
				
				for(int i = 0; i < spawnamount; i ++){
					int index = i;
					float range = 12f;
					
					Timers.run(index*5f, ()->{
						try{
							Enemy enemy = ClassReflection.newInstance(spawn.type);
							enemy.set(tile.worldx() + Mathf.range(range), tile.worldy() + Mathf.range(range));
							enemy.spawn = fl;
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
		
		int last = Settings.getInt("hiscore" + world.getMap().name);
		
		if(wave > last && !mode.infiniteResources && !mode.toggleWaves){
			Settings.putInt("hiscore" + world.getMap().name, wave);
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
		for(int i = 0; i < 16; i ++){
			Timers.run(i*2, ()->{
				Effects.effect(Fx.explosion, core.worldx()+Mathf.range(40), core.worldy()+Mathf.range(40));
			});
		}
		Effects.effect(Fx.coreexplosion, core.worldx(), core.worldy());
		
		Timers.run(60, ()-> ui.showRestart());
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
		Arrays.fill(items, 0);
		
		addItem(Item.stone, 40);
		
		if(debug){
			Arrays.fill(items, 999900);
		}
	}
	
	public  int getAmount(Item item){
		return items[item.id];
	}
	
	public void addItem(Item item, int amount){
		items[item.id] += amount;
		shouldUpdateItems = true;
	}
	
	public boolean hasItems(ItemStack[] items){
		for(ItemStack stack : items)
			if(!hasItem(stack))
				return false;
		return true;
	}
	
	public boolean hasItems(ItemStack[] items, int scaling){
		for(ItemStack stack : items)
			if(!hasItem(stack.item, stack.amount * scaling))
				return false;
		return true;
	}
	
	public boolean hasItem(ItemStack req){
		return items[req.item.id] >= req.amount;
	}
	
	public boolean hasItem(Item item, int amount){
		return items[item.id] >= amount;
	}
	
	public void removeItem(ItemStack req){
		items[req.item.id] -= req.amount;
		shouldUpdateItems = true;
	}
	
	public void removeItems(ItemStack... reqs){
		for(ItemStack req : reqs)
			removeItem(req);
	}
	
	public int[] getItems(){
		return items;
	}
	
	@Override
	public void pause(){
		wasPaused = GameState.is(State.paused);
		if(GameState.is(State.playing)) GameState.set(State.paused);
	}
	
	@Override
	public void resume(){
		if(GameState.is(State.paused) && !wasPaused){
			GameState.set(State.playing);
		}
	}
	
	@Override
	public void init(){
		Timers.run(1f, Musics::shuffleAll);
		
		Entities.initPhysics();
		
		Entities.setCollider(tilesize, (x, y) -> world.solid(x, y));
	}
	
	@Override
	public void update(){

        if(Gdx.input != proxy){
            Gdx.input = proxy;
        }

        if(KeyBinds.getSection("default").device.type == DeviceType.controller){
            if(Inputs.keyTap("select")){
                Inputs.getProcessor().touchDown(Gdx.input.getX(), Gdx.input.getY(), 0, Buttons.LEFT);
            }

            if(Inputs.keyRelease("select")){
				Inputs.getProcessor().touchUp(Gdx.input.getX(), Gdx.input.getY(), 0, Buttons.LEFT);
            }

            float xa = Inputs.getAxis("cursor_x");
            float ya = Inputs.getAxis("cursor_y");

            if(Math.abs(xa) > Vars.controllerMin || Math.abs(ya) > Vars.controllerMin) {
            	float scl = Settings.getInt("sensitivity")/100f * Unit.dp.scl(1f);
                controlx += xa*Vars.baseControllerSpeed*scl;
                controly -= ya*Vars.baseControllerSpeed*scl;
                controlling = true;

				Inputs.getProcessor().touchDragged(Gdx.input.getX(), Gdx.input.getY(), 0);
            }

            controlx = Mathf.clamp(controlx, 0, Gdx.graphics.getWidth());
            controly = Mathf.clamp(controly, 0, Gdx.graphics.getHeight());

            if(Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1)
                controlling = false;
        }else{
            controlling = false;
        }

        if(!controlling){
            controlx = Gdx.input.getX();
            controly = Gdx.input.getY();
        }

        Gdx.input.setCursorCatched(controlling);
		
		if(debug && GameState.is(State.playing)){
			//debug actions
			if(Inputs.keyTap(Keys.P)){
				Effects.effect(Fx.shellsmoke, player);
				Effects.effect(Fx.shellexplosion, player);
			}
			
			if(Inputs.keyTap(Keys.C)){
				enemyGroup.clear();
				enemies = 0;
			}
			
			if(Inputs.keyTap(Keys.F)){
				wavetime = 0f;
			}

			if(Inputs.keyDown(Keys.I)){
				wavetime -= delta() * 10f;
			}

			if(Inputs.keyTap(Keys.U)){
				Vars.showUI = !Vars.showUI;
			}
			
			if(Inputs.keyTap(Keys.O)){
				Vars.noclip = !Vars.noclip;
			}
			
			if(Inputs.keyTap(Keys.Y)){
				if(Inputs.keyDown(Keys.SHIFT_LEFT)){
					new HealerEnemy().set(player.x, player.y).add();
				}else{
					float px = player.x, py = player.y;
					Timers.run(30f, ()-> new FortressEnemy().set(px, py).add());
				}
			}
		}
		
		if(shouldUpdateItems && (Timers.get("updateItems", 8) || GameState.is(State.paused))){
			ui.updateItems();
			shouldUpdateItems = false;
		}
		
		if(!GameState.is(State.menu)){
			input.update();
			
			if(Inputs.keyTap("pause") && !ui.isGameOver() && (GameState.is(State.paused) || GameState.is(State.playing))){
				GameState.set(GameState.is(State.playing) ? State.paused : State.playing);
			}
			
			if(Inputs.keyTap("menu")){
				if(GameState.is(State.paused)){
					ui.hideMenu();
					GameState.set(State.playing);
				}else if (!ui.isGameOver()){
					ui.showMenu();
					GameState.set(State.paused);
				}
			}
		
			if(!GameState.is(State.paused)){
				
				if(respawntime > 0){
					
					respawntime -= delta();
					
					if(respawntime <= 0){
						player.set(core.worldx(), core.worldy()-Vars.tilesize*2);
						player.heal();
						player.add();
						Effects.sound("respawn");
						ui.fadeRespawn(false);
					}
				}
				
				if(tutorial.active()){
					tutorial.update();
				}
				
				if(!tutorial.active() && !mode.toggleWaves){
				
					if(enemies <= 0){
						wavetime -= delta();

						if(lastUpdated < wave + 1 && wavetime < Vars.aheadPathfinding){ //start updatingbeforehand
							world.pathfinder().updatePath();
							lastUpdated = wave + 1;
						}
					}else{
						extrawavetime -= delta();
					}
				}
			
				if(wavetime <= 0 || extrawavetime <= 0){
					runWave();
				}
				
				Entities.update(Entities.defaultGroup());
				Entities.update(bulletGroup);
				Entities.update(enemyGroup);
				Entities.update(tileGroup);
				Entities.update(shieldGroup);
				
				Entities.collideGroups(enemyGroup, bulletGroup);
				Entities.collideGroups(Entities.defaultGroup(), bulletGroup);
			}
		}
	}

}
