package io.anuke.mindustry.game;

import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.state;

public class EnemySpawn{
	/**The enemy type spawned*/
	public final EnemyType type;
	/**When this spawns should end*/
	protected int before = Integer.MAX_VALUE;
	/**When this spawns should start*/
	protected int after;
	/**The spacing, in waves, of spawns. 2 = spawns every other wave*/
	protected int spacing = 1;
	/**How many waves need to pass after the start of this spawn for the tier to increase by one*/
	protected int tierscale = 17;
	/**How many more enemies there are, every time the tier increases*/
	protected int tierscaleback = 0;
	/**The tier this spawn starts at.*/
	protected int tier = 1;
	/**Maximum amount of enemies that spawn*/
	protected int max = 60;
	/**How many waves need to pass before the amount of enemies increases by 1*/
	protected float scaling = 9999f;
	/**Amount of enemies spawned initially, with no scaling*/
	protected int amount = 1;
	
	public EnemySpawn(EnemyType type){
		this.type = type;
	}
	
	public int evaluate(int wave, int lane){
		if(wave < after || wave > before || (wave - after) % spacing != 0){
			return 0;
		}
		float scaling = this.scaling * state.difficulty.enemyScaling;
		
		return Math.min(amount-1 + Math.max((int)((wave / spacing) / scaling), 1) + (tier(wave, lane)-1) * tierscaleback, max);
	}
	
	public int tier(int wave, int lane){
		return Mathf.clamp(tier + (wave-after)/tierscale, 1, EnemyType.maxtier);
	}
}
