package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.renderer;
import static io.anuke.mindustry.Vars.tilesize;

//TODO remove
public class ShieldedWallBlock extends PowerBlock{
	static final float hitTime = 18f;
	static final Color hitColor = Color.SKY.cpy().mul(1.2f);
	public float powerPerDamage = 0.08f;

	public ShieldedWallBlock(String name) {
		super(name);
		destructible = true;
		update = false;
	}
	
	@Override
	public float handleDamage(Tile tile, float amount){
		float drain = amount * powerPerDamage;
		ShieldedWallEntity entity = tile.entity();
		
		if(entity.power.amount > drain){
			entity.power.amount -= drain;
			entity.hit = hitTime;
			return 0;
		}else if(entity.power.amount > 0){
			int reduction = (int)(entity.power.amount / powerPerDamage);
			entity.power.amount = 0;
			
			return amount - reduction;
		}
		
		return amount;
	}

	@Override
	public void setStats(){
		super.setStats();
		//stats.add("powerdraindamage", Strings.toFixed(powerPerDamage, 2));
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		ShieldedWallEntity entity = tile.entity();
		
		if(entity.power.amount > powerPerDamage){
			renderer.addShield(() -> Draw.rect("blank", tile.worldx(), tile.worldy(), tilesize, tilesize));
		}
		
		Draw.color(hitColor);
		Draw.alpha(entity.hit / hitTime * 0.9f);
		Draw.rect("blank", tile.worldx(), tile.worldy(), tilesize, tilesize);
		Draw.reset();
		
		entity.hit -= Timers.delta();
		entity.hit = Math.max(entity.hit, 0);
	}
	
	@Override
	public TileEntity getEntity(){
		return new ShieldedWallEntity();
	}
	
	static class ShieldedWallEntity extends TileEntity{
		public float hit;
	}
}
