package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Strings;

public class LiquidCrafter extends LiquidBlock{
	protected final int timerDump = timers++;
	protected final int timerPurify = timers++;
	
	/**Can be null.*/
	public Item input = null;
	public int inputAmount = 5;
	public Liquid inputLiquid = null;
	public float liquidAmount = 20f;
	public Item output = null;
	public int itemCapacity = 90;
	public int purifyTime = 80;
	public Effect craftEffect = Fx.purify;

	public LiquidCrafter(String name) {
		super(name);
		update = true;
		rotate = false;
		solid = true;
		health = 60;
		liquidCapacity = 21f;
	}

	@Override
	public void setBars(){
		super.setBars();
		bars.remove(BarType.inventory);

		bars.add(new BlockBar(BarType.inventory, true,
				tile -> input == null ? -1f : (float)tile.entity.inventory.getItem(input) / itemCapacity));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("maxitemssecond", Strings.toFixed(60f/purifyTime, 1));
		stats.add("inputliquid", inputLiquid + " x " + (int)liquidAmount);
		if(input != null) stats.add("itemcapacity", itemCapacity);
		if(input != null) stats.add("inputitem", input + " x " + inputAmount);
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
	public void update(Tile tile){
		if(tile.entity.timer.get(timerPurify, purifyTime) && tile.entity.liquid.amount >= liquidAmount &&
				(input == null || tile.entity.inventory.hasItem(input, inputAmount))){
			
			if(input != null)
				tile.entity.inventory.removeItem(input, inputAmount);
			tile.entity.liquid.amount -= liquidAmount;
			offloadNear(tile, output);
			Effects.effect(craftEffect, tile.worldx(), tile.worldy());
		}
		
		if(tile.entity.timer.get(timerDump, 15)){
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
		return input != null && item == input && entity.inventory.getItem(input) < itemCapacity;
	}

}
