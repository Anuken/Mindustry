package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.GestureHandler;
import io.anuke.mindustry.input.Input;
import io.anuke.mindustry.world.Generator;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class Control extends RendererModule{
	public int rangex = 10, rangey = 10;
	public float targetzoom = 1f;
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
		
		Generator.loadMaps();
		
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
	
	@Override
	public void init(){
		Musics.shuffleAll();
		
		Entities.initPhysics();
		
		Entities.setCollider(tilesize, (x, y)->{
			return World.solid(x, y);
		});

		EffectLoader.create();
	}
	
	public void clampZoom(){
		targetzoom = Mathf.clamp(targetzoom, 0.5f, 2f);
		camera.zoom = Mathf.clamp(camera.zoom, 0.5f, 2f);
	}
	
	@Override
	public void update(){
		
		if(Inputs.keyUp(Keys.ESCAPE) && debug)
			Gdx.app.exit();
		
		//camera.zoom = MathUtils.lerp(camera.zoom, targetzoom, 0.5f*delta());
		
		if(Inputs.keyUp(Keys.SPACE) && debug)
			Effects.sound("shoot", core.worldx(), core.worldy());
		
		if(!playing){
			clearScreen();
		}else{
			
			if(Inputs.keyUp("menu")){
				if(paused){
					ui.hideMenu();
					paused = false;
				}else{
					ui.showMenu();
					paused = true;
				}
			}
		
			if(!paused){
				
				if(respawntime > 0){
					
					respawntime -= delta();
					
					if(respawntime <= 0){
						player.set(core.worldx(), core.worldy()-8);
						player.heal();
						player.add();
						Effects.sound("respawn");
					}
				}
				
				if(enemies <= 0)
					wavetime -= delta();
			
				if(wavetime <= 0 || (debug && Inputs.keyUp(Keys.F))){
					GameState.runWave();
				}
			
				Entities.update();
				
				if(!android){
					Input.doInput();
				}else{
					AndroidInput.doInput();
				}
				
			}
			
			if(core.block() == ProductionBlocks.core){
				smoothCamera(player.x, player.y, android ? 0.3f : 0.14f);
			}else{
				smoothCamera(core.worldx(), core.worldy(), 0.4f);
			}
			
			updateShake();
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f, pixsize - tilesize / 2f, pixsize - tilesize / 2f);
			
			if(android){
				UCore.log(camera.position.x-prevx, camera.position.y-prevy);
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
		
		if(!paused){
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
	}
}
