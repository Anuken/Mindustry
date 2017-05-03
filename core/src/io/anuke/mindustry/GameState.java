package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class GameState{
	
	public static void reset(){
		wave = 1;
		wavetime = waveSpacing();
		Entities.clear();
		enemies = 0;
		player.add();
		player.heal();
		Inventory.clearItems();
		spawnpoints.clear();
		
		ui.updateItems();
	}
	
	public static void play(){
		player.x = core.worldx();
		player.y = core.worldy()-8;
		
		wavetime = waveSpacing();
		
		if(showedTutorial || !Settings.getBool("tutorial")){
			playing = true;
			paused = false;
		}else{
			playing = true;
			paused = true;
			ui.showTutorial();
			showedTutorial = true;
		}
	}
	
	public static void runWave(){
		int amount = wave;
		Sounds.play("spawn");
		
		for(int i = 0; i < amount; i ++){
			int pos = i;
			
			for(int w = 0; w < spawnpoints.size; w ++){
				int point = w;
				Tile tile = spawnpoints.get(w);
				
				Timers.run(i*30f, ()->{
					
					Enemy enemy = null;
					
					if(wave%5 == 0 /*&& point == 1 */&& pos == 0){
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
		
		wavetime = waveSpacing();
	}
	
	public static void coreDestroyed(){
		Effects.shake(5, 6);
		Sounds.play("corexplode");
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
	
	public static float waveSpacing(){
		int scale = Settings.getInt("difficulty");
		float out = (scale == 0 ? 2f : scale == 1f ? 1f : 0.5f);
		return wavespace*out;
	}
}
