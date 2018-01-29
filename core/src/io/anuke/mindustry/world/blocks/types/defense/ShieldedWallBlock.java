package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.renderer;

public class ShieldedWallBlock extends PowerBlock{
	static final float hitTime = 18f;
	static final Color hitColor = Color.SKY.cpy().mul(1.2f);
	public float powerPerDamage = 0.08f;

	public ShieldedWallBlock(String name) {
		super(name);
		destructible = true;
		update = false;
		voltage = 0.00001f;
	}
	
	@Override
	public int handleDamage(Tile tile, int amount){
		float drain = amount * powerPerDamage;
		ShieldedWallEntity entity = tile.entity();
		
		if(entity.power > drain){
			entity.power -= drain;
			entity.hit = hitTime;
			return 0;
		}else if(entity.power > 0){
			int reduction = (int)(entity.power / powerPerDamage);
			entity.power = 0;
			
			return amount - reduction;
		}
		
		return amount;
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Drain/damage: " + Strings.toFixed(powerPerDamage, 2));
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		ShieldedWallEntity entity = tile.entity();
		
		if(entity.power > powerPerDamage){
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
	
	static class ShieldedWallEntity extends PowerEntity{
		public float hit;
	}
}
