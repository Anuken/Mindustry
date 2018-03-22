package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Strings;

public abstract class PowerTurret extends Turret{
	public float powerUsed = 0.5f;

	public PowerTurret(String name) {
		super(name);
		ammo = null;
		hasPower = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("powershot", Strings.toFixed(powerUsed, 1));
	}
	
	@Override
	public boolean hasAmmo(Tile tile){
		return tile.entity.power.amount >= powerUsed;
	}
	
	@Override
	public void consumeAmmo(Tile tile){
		tile.entity.power.amount -= powerUsed;
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return false;
	}
}
