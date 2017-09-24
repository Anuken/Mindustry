package io.anuke.mindustry.entities;

import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.ucore.util.Mathf;

public class EnemySpawn{
	public final Class<? extends Enemy> type;
	protected int before = Integer.MAX_VALUE;
	protected int after;
	protected int spacing = 1;
	protected int tierscale = 15;
	protected int tierscaleback = 1;
	protected int max = 17;
	protected float scaling = 9999f;
	
	public EnemySpawn(Class<? extends Enemy> type){
		this.type = type;
	}
	
	public int evaluate(int wave, int lane){
		if(wave < after || wave > before || (wave - after) % spacing != 0){
			return 0;
		}
		return Math.min(1 * Math.max((int)((wave / spacing) / scaling), 1) - (tier(wave, lane)-1) * tierscaleback, max);
	}
	
	public int tier(int wave, int lane){
		return Mathf.clamp(1 + (wave-after)/tierscale, 1, Enemy.maxtier);
	}
}
