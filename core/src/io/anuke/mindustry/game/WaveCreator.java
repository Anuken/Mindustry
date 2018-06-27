package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.ItemStack;

public class WaveCreator{
	
	public static Array<SpawnGroup> getSpawns(){
		return Array.with(
				new SpawnGroup(UnitTypes.scout){{
					end = 5;
				}},

				new SpawnGroup(UnitTypes.titan){{
					begin = 6;
				}},

				new SpawnGroup(UnitTypes.scout){{
					begin = 6;
					items = new ItemStack(Items.thermite, 100);
				}},

				new SpawnGroup(UnitTypes.vtol){{
					begin = 8;
				}},

				new SpawnGroup(UnitTypes.monsoon){{
					begin = 16;
				}}
		);
	}

	public static void testWaves(int from, int to){
		Array<SpawnGroup> spawns = getSpawns();
		for(int i = from; i <= to; i ++){
			System.out.print(i+": ");
			int total = 0;
			for(SpawnGroup spawn : spawns){
				int a = spawn.getUnitsSpawned(i);
				total += a;
				
				if(a > 0){
					System.out.print(a+"x" + spawn.type.name);
				}
			}
			System.out.print(" (" + total + ")");
			System.out.println();
		}
	}
}
