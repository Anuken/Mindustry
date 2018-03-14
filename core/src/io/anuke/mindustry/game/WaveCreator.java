package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;

public class WaveCreator{
	
	public static Array<EnemySpawn> getSpawns(){
		//TODO
		return null;
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
