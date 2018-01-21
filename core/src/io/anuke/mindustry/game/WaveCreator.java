package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.enemies.EnemyTypes;

public class WaveCreator{
	
	public static Array<EnemySpawn> getSpawns(){

		return Array.with(
			new EnemySpawn(EnemyTypes.standard){{
				scaling = 1;
				before = 3;
			}},
			
			new EnemySpawn(EnemyTypes.fast){{
				scaling = 1;
				after = 3;
				spacing = 5;
				amount = 3;
				tierscaleback = 0;
			}},
			
			new EnemySpawn(EnemyTypes.blast){{
				after = 4;
				amount = 2;
				spacing = 5;
				scaling = 2;
				tierscaleback = 1;
			}},
			
			new EnemySpawn(EnemyTypes.tank){{
				after = 5;
				spacing = 5;
				scaling = 2;
				amount = 2;
			}},
			
			new EnemySpawn(EnemyTypes.rapid){{
				after = 7;
				spacing = 5;
				scaling = 2;
				amount = 3;
			}},
			
			new EnemySpawn(EnemyTypes.healer){{
				after = 5;
				spacing = 5;
				scaling = 1;
				amount = 1;
			}},
			
			new EnemySpawn(EnemyTypes.standard){{
				scaling = 3;
				after = 8;
				spacing = 4;
				tier = 2;
			}},
			
			new EnemySpawn(EnemyTypes.titan){{
				after = 6;
				amount = 2;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(EnemyTypes.flamer){{
				after = 12;
				amount = 2;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(EnemyTypes.emp){{
				after = 15;
				amount = 1;
				spacing = 5;
				scaling = 2;
			}},
			
			new EnemySpawn(EnemyTypes.blast){{
				after = 4 + 5 + 5;
				amount = 3;
				spacing = 5;
				scaling = 2;
				tierscaleback = 0;
			}},
			//boss wave
			new EnemySpawn(EnemyTypes.fortress){{
				after = 16;
				amount = 1;
				spacing = 5;
				scaling = 1;
			}},
			
			new EnemySpawn(EnemyTypes.titan){{
				after = 16;
				amount = 1;
				spacing = 5;
				scaling = 3;
				tierscaleback = 0;
			}},
			
			new EnemySpawn(EnemyTypes.healer){{
				after = 16;
				spacing = 5;
				scaling = 2;
				amount = 2;
			}},
			//end boss wave
			
			//enchanced boss wave
			new EnemySpawn(EnemyTypes.mortar){{
				after = 16 + 5;
				amount = 1;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(EnemyTypes.emp){{
				after = 16 + 5;
				amount = 1;
				spacing = 5;
				scaling = 3;
			}}
			//end enchanced boss wave
		);
	}

	public static void testWaves(int from, int to){
		Array<EnemySpawn> spawns = getSpawns();
		for(int i = from; i <= to; i ++){
			System.out.print(i+": ");
			int total = 0;
			for(EnemySpawn spawn : spawns){
				int a = spawn.evaluate(i, 0);
				int t = spawn.tier(i, 0);
				total += a;
				
				if(a > 0){
					System.out.print(a+"x" + spawn.type.name + "-" + t + " ");
				}
			}
			System.out.print(" (" + total + ")");
			System.out.println();
		}
	}
}
