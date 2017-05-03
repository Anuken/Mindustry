package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Generator;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.util.Timers;

public class Control extends RendererModule{
	public int rangex = 10, rangey = 10;
	//GifRecorder recoder = new GifRecorder(batch);
	
	public Control(){
		cameraScale = 4f;
		setPixelation();
		buffers.add("shadow", (int) (Gdx.graphics.getWidth() / cameraScale), (int) (Gdx.graphics.getHeight() / cameraScale));
		
		atlas = new Atlas("mindustry.atlas");
		
		Sounds.load("shoot.wav", "place.wav", "explosion.wav", "enemyshoot.wav", "corexplode.wav", "break.wav", "spawn.wav", "flame.wav");
		Musics.load("1.mp3", "2.mp3", "3.mp3");
		
		Generator.loadMaps();
		
		KeyBinds.defaults(
			"up", Keys.W,
			"left", Keys.A,
			"down", Keys.S,
			"right", Keys.D,
			"rotate", Keys.R,
			"menu", Keys.ESCAPE
		);
			
		Settings.loadAll("io.anuke.moment");
		
		Sounds.setFalloff(9000f);
		
		player = new Player();
	}
	
	@Override
	public void init(){
		Musics.shuffleAll();
		
		Entities.initPhysics(0, 0, pixsize, pixsize);
		
		Entities.setCollider(tilesize, (x, y)->{
			return World.solid(x, y);
		});

		EffectLoader.create();
	}
	
	@Override
	public void update(){
		if(Inputs.keyUp(Keys.ESCAPE) && debug)
			Gdx.app.exit();
		
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
				
				if(enemies <= 0)
					wavetime -= delta();
			
				if(wavetime <= 0 || (debug && Inputs.keyUp(Keys.F))){
					GameState.runWave();
				}
			
				Entities.update();
	
				Input.doInput();
			}
			
			
			
			if(core.block() == ProductionBlocks.core)
				camera.position.set(player.x, player.y, 0f);
			else
				camera.position.set(core.worldx(), core.worldy(), 0f);
			
			updateShake();
			clampCamera(-tilesize / 2f, -tilesize / 2f, pixsize - tilesize / 2f, pixsize - tilesize / 2f);
	
			drawDefault();
			
			batch.setProjectionMatrix(control.camera.combined);
			batch.begin();
			Renderer.renderOverlay();
			batch.end();
			
			//recoder.update();
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

		buffers.remove("shadow");
		buffers.add("shadow", (int) (Gdx.graphics.getWidth() / cameraScale), (int) (Gdx.graphics.getHeight() / cameraScale));

		rangex = (int) (width / tilesize / cameraScale/2)+2;
		rangey = (int) (height / tilesize / cameraScale/2)+2;
	}
}
