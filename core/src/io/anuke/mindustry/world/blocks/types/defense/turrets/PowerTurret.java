package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.util.Strings;

public abstract class PowerTurret extends Turret {
	public float powerUsed = 0.5f;

	public PowerTurret(String name) {
		super(name);
		hasPower = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("powershot", Strings.toFixed(powerUsed, 1));
	}
	
	@Override
	public boolean hasAmmo(Tile tile){
		return tile.entity.power.amount >= powerUsed && super.hasAmmo(tile);
	}

	@Override
	public AmmoType useAmmo(Tile tile){
		tile.entity.power.amount -= powerUsed;
		return super.useAmmo(tile);
	}
}
