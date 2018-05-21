package io.anuke.mindustry.world.blocks.types.defense.turrets;

import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.util.Strings;

public abstract class PowerTurret extends Turret {
	protected float powerUsed = 0.5f;
	protected AmmoType shootType;

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
		return tile.entity.power.amount >= powerUsed;
	}

	@Override
	public AmmoType useAmmo(Tile tile){
		tile.entity.power.amount -= powerUsed;
		return shootType;
	}

	@Override
	public AmmoType peekAmmo(Tile tile) {
		return shootType;
	}
}
