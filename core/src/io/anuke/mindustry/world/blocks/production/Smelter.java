package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Mathf;

import java.util.Arrays;

public class Smelter extends Block{
	protected final int timerDump = timers++;
	protected final int timerCraft = timers++;
	
	protected Item[] inputs;
	protected Item fuel;
	protected Item result;

	protected float minFlux = 0.2f;
	protected float baseFluxChance = 0.15f;
	protected boolean useFlux = false;

	protected float craftTime = 20f;
	protected float burnDuration = 50f;
	protected Effect craftEffect = BlockFx.smelt, burnEffect = BlockFx.fuelburn;
	protected Color flameColor = Color.valueOf("ffb879");

	public Smelter(String name) {
		super(name);
		update = true;
		hasItems = true;
		solid = true;
		itemCapacity = 20;
	}

	@Override
	public void setBars(){
		for(Item item : inputs){
			bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.items.getItem(item)/itemCapacity));
		}
	}
	
	@Override
	public void setStats(){
		super.setStats();

		stats.add(BlockStat.inputFuel, fuel.toString());
		stats.add(BlockStat.fuelBurnTime, burnDuration/60f);
		stats.add(BlockStat.inputItems, Arrays.toString(inputs));
		stats.add(BlockStat.outputItem, result.toString());
		stats.add(BlockStat.craftSpeed, 60f/craftTime);
		stats.add(BlockStat.inputItemCapacity, itemCapacity);
		stats.add(BlockStat.outputItemCapacity, itemCapacity);
	}

	@Override
	public void init() {
		super.init();

		for(Item item : inputs){
			if(item.fluxiness >= minFlux && useFlux){
				throw new IllegalArgumentException("'" + name + "' has input item '" + item.name + "', which is a flux, when useFlux is enabled. To prevent ambiguous item use, either remove this flux item from the inputs, or set useFlux to false.");
			}
		}
	}

	@Override
	public void update(Tile tile){
		SmelterEntity entity = tile.entity();
		
		if(entity.timer.get(timerDump, 5) && entity.items.hasItem(result)){
			tryDump(tile, result);
		}

		//add fuel
		if(entity.items.getItem(fuel) > 0 && entity.burnTime <= 0f){
			entity.items.removeItem(fuel, 1);
			entity.burnTime += burnDuration;
			Effects.effect(burnEffect, entity.x + Mathf.range(2f), entity.y + Mathf.range(2f));
		}

		//decrement burntime
		if(entity.burnTime > 0){
			entity.burnTime -= Timers.delta();
			entity.heat = Mathf.lerp(entity.heat, 1f, 0.02f);
		}else{
			entity.heat = Mathf.lerp(entity.heat, 0f, 0.02f);
		}

		//make sure it has all the items
		for(Item item : inputs){
			if(!entity.items.hasItem(item)){
				return;
			}
		}

		if(entity.items.getItem(result) >= itemCapacity //output full
				|| entity.burnTime <= 0 //not burning
				|| !entity.timer.get(timerCraft, craftTime)){ //not yet time
			return;
		}

		boolean consumeInputs = false;

		if(useFlux){
			//remove flux materials if present
			for(Item item : Item.all()){
				if(item.fluxiness >= minFlux && tile.entity.items.getItem(item) > 0){
					tile.entity.items.removeItem(item, 1);

					//chance of not consuming inputs if flux material present
					consumeInputs = !Mathf.chance(item.fluxiness * baseFluxChance);
					break;
				}
			}
		}

		if(consumeInputs) {
			for (Item item : inputs) {
				entity.items.removeItem(item, 1);
			}
		}
		
		offloadNear(tile, result);
		Effects.effect(craftEffect, flameColor, tile.drawx(), tile.drawy());
	}

	@Override
	public int getMaximumAccepted(Tile tile, Item item) {
		return itemCapacity - tile.entity.items.getItem(item);
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		boolean isInput = false;

		for(Item req : inputs){
			if(req == item){
				isInput = true;
				break;
			}
		}

		return (isInput && tile.entity.items.getItem(item) < itemCapacity) || (item == fuel && tile.entity.items.getItem(fuel) < itemCapacity) ||
				(useFlux && item.fluxiness >= minFlux && tile.entity.items.getItem(item) < itemCapacity);
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);

        SmelterEntity entity = tile.entity();

        //draw glowing center
		if(entity.heat > 0f){
			float g = 0.1f;

			Draw.alpha(((1f-g) + Mathf.absin(Timers.time(), 8f, g)) * entity.heat);

			Draw.tint(flameColor);
			Fill.circle(tile.drawx(), tile.drawy(), 2f + Mathf.absin(Timers.time(), 5f, 0.8f));
			Draw.color(1f, 1f, 1f, entity.heat);
			Fill.circle(tile.drawx(), tile.drawy(), 1f + Mathf.absin(Timers.time(), 5f, 0.7f));

			Draw.color();
		}
    }

	@Override
	public TileEntity getEntity() {
		return new SmelterEntity();
	}

	public class SmelterEntity extends TileEntity{
		public float burnTime;
		public float heat;
	}
}
