package io.anuke.mindustry.entities;

import io.anuke.mindustry.entities.enemies.Enemy;

public class EnemySpawn{
	public final Class<? extends Enemy> type;
	int before = Integer.MAX_VALUE;
	int after;
	int spacing = 1;
	float scaling = 9999f;
	
	public EnemySpawn(Class<? extends Enemy> type){
		this.type = type;
	}
	
	public int evaluate(int wave, int lane){
		if(wave < after || wave > before || wave % spacing != 0){
			return 0;
		}
		return 1 * Math.max((int)((wave / spacing) / scaling), 1);
	}
}
