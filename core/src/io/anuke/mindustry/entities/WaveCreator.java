package io.anuke.mindustry.entities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.entities.enemies.*;

public class WaveCreator{
	
	public static Array<EnemySpawn> getSpawns(){
		//TODO
		//Gdx.app.exit();
		return Array.with(
			new EnemySpawn(Enemy.class){{
				scaling = 1;
				before = 3;
			}},
			
			new EnemySpawn(FastEnemy.class){{
				scaling = 1;
				after = 3;
				spacing = 5;
				amount = 4;
				tierscaleback = 0;
			}},
			
			new EnemySpawn(BlastEnemy.class){{
				after = 4;
				amount = 3;
				spacing = 5;
				scaling = 2;
				tierscaleback = 0;
			}},
			
			new EnemySpawn(TankEnemy.class){{
				after = 5;
				spacing = 5;
				scaling = 2;
				amount = 2;
			}},
			
			new EnemySpawn(RapidEnemy.class){{
				after = 7;
				spacing = 5;
				scaling = 2;
				amount = 3;
			}},
			
			new EnemySpawn(HealerEnemy.class){{
				after = 5;
				spacing = 5;
				scaling = 1;
				amount = 1;
			}},
			
			new EnemySpawn(Enemy.class){{
				scaling = 3;
				after = 8;
				spacing = 4;
				tier = 2;
			}},
			
			new EnemySpawn(TitanEnemy.class){{
				after = 6;
				amount = 2;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(FlamerEnemy.class){{
				after = 12;
				amount = 3;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(EmpEnemy.class){{
				after = 15;
				amount = 1;
				spacing = 5;
				scaling = 2;
			}},
			
			new EnemySpawn(BlastEnemy.class){{
				after = 4 + 5 + 5;
				amount = 3;
				spacing = 5;
				scaling = 2;
				tierscaleback = 0;
			}},
			//boss wave
			new EnemySpawn(FortressEnemy.class){{
				after = 16;
				amount = 1;
				spacing = 5;
				scaling = 1;
			}},
			
			new EnemySpawn(TitanEnemy.class){{
				after = 16;
				amount = 1;
				spacing = 5;
				scaling = 3;
				tierscaleback = 0;
			}},
			
			new EnemySpawn(HealerEnemy.class){{
				after = 16;
				spacing = 5;
				scaling = 2;
				amount = 2;
			}},
			//end boss wave
			
			//enchanced boss wave
			new EnemySpawn(MortarEnemy.class){{
				after = 16 + 5;
				amount = 1;
				spacing = 5;
				scaling = 3;
			}},
			
			new EnemySpawn(EmpEnemy.class){{
				after = 16 + 5;
				amount = 1;
				spacing = 5;
				scaling = 3;
			}}
			//end enchanced boss wave
		);
	}
	
	public static Array<EnemySpawn> getSpawnsOld(){
		return Array.with(
			new EnemySpawn(Enemy.class){{
				scaling = 2;
				before = 4;
			}},
			new EnemySpawn(Enemy.class){{
				scaling = 3;
				tierscaleback = 3;
				spacing = 2;
				after = 4;
			}},
			new EnemySpawn(TitanEnemy.class){{
				after = 5;
				spacing = 2;
				scaling = 5;
			}},
			new EnemySpawn(FortressEnemy.class){{
				after = 12;
				spacing = 5;
				scaling = 7;
			}},
			new EnemySpawn(HealerEnemy.class){{
				scaling = 3;
				spacing = 2;
				after = 8;
			}},
			new EnemySpawn(FastEnemy.class){{
				after = 2;
				scaling = 3;
			}},
			new EnemySpawn(FlamerEnemy.class){{
				after = 14;
				spacing = 6;
				scaling = 3;
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
					System.out.print(a+"x" + ClassReflection.getSimpleName(spawn.type) + "-" + t + " ");
				}
			}
			System.out.print(" (" + total + ")");
			System.out.println();
		}
	}
}
