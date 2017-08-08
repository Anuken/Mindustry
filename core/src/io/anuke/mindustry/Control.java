package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.entities.enemies.*;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.GestureHandler;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class Control extends RendererModule{
	public int rangex = 10, rangey = 10;
	public float targetzoom = 1f;
	
	boolean showedTutorial;
	boolean hiscore = false;
	
	final Array<Weapon> weapons = new Array<>();
	
	int wave = 1;
	float wavetime;
	int enemies = 0;
	
	float respawntime;
	
	//GifRecorder recorder = new GifRecorder(batch);
	
	public Control(){
		cameraScale = baseCameraScale;
		pixelate();
		
		Gdx.input.setCatchBackKey(true);
		
		if(android){
			Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, new GestureHandler()));
			Inputs.addProcessor(new AndroidInput());
		}
		
		Draw.addSurface("shadow", cameraScale);
		
		atlas = new Atlas("mindustry.atlas");
		
		Sounds.load("shoot.wav", "place.wav", "explosion.wav", "enemyshoot.wav", 
				"corexplode.wav", "break.wav", "spawn.wav", "flame.wav", "die.wav", 
				"respawn.wav", "purchase.wav", "flame2.wav");
		
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
		
		for(String map : maps)
			Settings.defaults("hiscore"+map, 0);
		
		Sounds.setFalloff(9000f);
		
		player = new Player();
	}
	
	public void setCameraScale(int scale){
		this.cameraScale = scale;
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		setCamera(player.x, player.y);
		Draw.getSurface("pixel").setScale(cameraScale);
		Draw.getSurface("shadow").setScale(cameraScale);
	}
	
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
		player.y = World.core.worldy()-8;
		
		control.camera.position.set(player.x, player.y, 0);
		
		wavetime = waveSpacing();
		
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
		int amount = wave;
		Sounds.play("spawn");
		
		Pathfind.updatePath();
		
		for(int i = 0; i < amount; i ++){
			int pos = i;
			
			for(int w = 0; w < World.spawnpoints.size; w ++){
				int point = w;
				Tile tile = World.spawnpoints.get(w);
				
				Timers.run(i*30f, ()->{
					
					Enemy enemy = null;
					
					if(wave%5 == 0 & pos < wave/5){
						enemy = new BossEnemy(point);
					}else if(wave > 3 && pos < amount/2){
						enemy = new FastEnemy(point);
					}else if(wave > 8 && pos % 3 == 0 && wave%2==1){
						enemy = new FlameEnemy(point);
					}else{
						enemy = new Enemy(point);
					}
					
					enemy.set(tile.worldx(), tile.worldy());
					Effects.effect("spawn", enemy);
					enemy.add();
				});
				
				enemies ++;
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
		Effects.shake(5, 6);
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
	
	public void clampZoom(){
		targetzoom = Mathf.clamp(targetzoom, 0.5f, 2f);
		camera.zoom = Mathf.clamp(camera.zoom, 0.5f, 2f);
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
			if(Inputs.keyUp(Keys.ESCAPE))
				Gdx.app.exit();
			
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
			
			updateShake(0.5f);
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f, World.pixsize - tilesize / 2f, World.pixsize - tilesize / 2f);
			
			if(android){
				player.x += camera.position.x-prevx;
				player.y += camera.position.y-prevy;
			}
			
			float lastx = camera.position.x, lasty = camera.position.y;
			
			if(android){
				camera.position.set((int)camera.position.x, (int)camera.position.y, 0);
				
				if(Gdx.graphics.getHeight()/cameraScale % 2 == 1){
					camera.position.add(0, -0.5f, 0);
				}
			}
	
			drawDefault();
			
			batch.setProjectionMatrix(control.camera.combined);
			batch.begin();
			Renderer.renderOverlay();
			batch.end();
			
			camera.position.set(lastx, lasty, 0);
			
			//recorder.update();
		}
		
		if(!GameState.is(State.paused)){
			Inputs.update();
			Timers.update(Gdx.graphics.getDeltaTime()*60f);
		}
	}
	
	@Override
	public void draw(){
		Renderer.renderTiles();
		Entities.draw();
		Renderer.renderPixelOverlay();
	}
	
	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		
		rangex = (int) (width / tilesize / cameraScale/2)+2;
		rangey = (int) (height / tilesize / cameraScale/2)+2;
		
		AndroidInput.mousex = Gdx.graphics.getWidth()/2;
		AndroidInput.mousey = Gdx.graphics.getHeight()/2;
		camera.position.set(player.x, player.y, 0);
	}

}
