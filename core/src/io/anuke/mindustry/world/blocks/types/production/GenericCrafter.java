package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Strings;

public class GenericCrafter extends Block{
	protected final int timerDump = timers++;
	
	/**Can be null. If you use this, make sure to set hasInvetory to true!*/
	protected ItemStack inputItem;
	/**Can be null. If you use this, make sure to set hasLiquids to true!*/
	protected Liquid inputLiquid;
	/**Required.*/
	protected Item output;
	protected float craftTime = 80;
	protected float powerUse;
	protected float liquidUse;
	protected Effect craftEffect = Fx.purify;

	public GenericCrafter(String name) {
		super(name);
		update = true;
		solid = true;
		health = 60;
	}

	@Override
	public void setBars(){
		super.setBars();

		if(inputItem != null) bars.replace(new BlockBar(BarType.inventory, true,
				tile -> (float)tile.entity.inventory.getItem(inputItem.item) / itemCapacity));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("maxitemssecond", Strings.toFixed(60f/craftTime, 1));
		if(inputLiquid != null) stats.add("inputliquid", inputLiquid + " x " + (int)(liquidUse * craftTime));
		if(inputItem != null) stats.add("inputitem", inputItem + " x " + inputItem.amount);
		stats.add("output", output);
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.drawx(), tile.drawy());
		
		if(tile.entity.liquid.liquid == null) return;
		
		Draw.color(tile.entity.liquid.liquid.color);
		Draw.alpha(tile.entity.liquid.amount / liquidCapacity);
		Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
		Draw.color();
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name)};
	}
	
	@Override
	public void update(Tile tile){
		GenericCrafterEntity entity = tile.entity();

		float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());
		float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());
		int itemsUsed = (inputItem == null ? 0 : (int)(1 + inputItem.amount * entity.progress));

		if((!hasLiquids || liquidUsed > entity.liquid.amount) &&
				(!hasPower || powerUsed > entity.power.amount) &&
				(!hasInventory || entity.inventory.hasItem(inputItem.item, itemsUsed))){

			entity.progress += 1f / craftTime * Timers.delta();
			entity.power.amount -= powerUsed;
			entity.liquid.amount -= liquidUsed;
		}

		if(entity.progress >= 1f){
			
			if(inputItem != null) tile.entity.inventory.removeItem(inputItem);
			offloadNear(tile, output);
			Effects.effect(craftEffect, tile.worldx(), tile.worldy());
		}
		
		if(tile.entity.timer.get(timerDump, 5)){
			tryDump(tile, output);
		}
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TileEntity entity = tile.entity();
		return inputItem != null && item == inputItem.item && entity.inventory.getItem(inputItem.item) < itemCapacity;
	}

	@Override
	public TileEntity getEntity() {
		return new GenericCrafterEntity();
	}

	public static class GenericCrafterEntity extends TileEntity{
		public float progress;
	}
}
