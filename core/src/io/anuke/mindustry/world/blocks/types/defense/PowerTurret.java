package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
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
	public void drawSelect(Tile tile){
		Draw.color(Color.GREEN);
		Lines.dashCircle(tile.drawx(), tile.drawy(), range);
		Draw.reset();
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
