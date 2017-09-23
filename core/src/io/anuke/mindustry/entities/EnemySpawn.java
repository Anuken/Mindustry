package io.anuke.mindustry.entities;

import io.anuke.mindustry.entities.enemies.Enemy;

public class EnemySpawn{
	public final Class<? extends Enemy> type;
	int before = Integer.MAX_VALUE;
	int after;
	int spacing;
	
	public EnemySpawn(Class<? extends Enemy> type){
		this.type = type;
	}
	
	public int evaluate(int wave, int lane){
		return 0;
	}
}
