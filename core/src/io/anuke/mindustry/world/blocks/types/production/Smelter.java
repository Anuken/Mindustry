package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.util.Arrays;

public class Smelter extends Block{
	protected final int timerDump = timers++;
	protected final int timerCraft = timers++;
	
	protected Item[] inputs;
	protected Item fuel;
	protected Item result;

	protected float craftTime = 20f; //time to craft one item, so max 3 items per second by default
	protected float burnDuration = 50f; //by default, the fuel will burn 45 frames, so that's 2.5 items/fuel at most
	protected Effect craftEffect = Fx.smelt, burnEffect = Fx.fuelburn;

	protected int capacity = 20;

	public Smelter(String name) {
		super(name);
		update = true;
		solid = true;
	}

	@Override
	public void setBars(){
		for(Item item : inputs){
			bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.inventory.getItem(item)/capacity));
		}
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("input", Arrays.toString(inputs));
		stats.add("fuel", fuel);
		stats.add("output", result);
		stats.add("fuelduration", Strings.toFixed(burnDuration/60f, 1));
		stats.add("maxoutputsecond", Strings.toFixed(60f/craftTime, 1));
		stats.add("inputcapacity", capacity);
		stats.add("outputcapacity", capacity);
	}
	
	@Override
	public void update(Tile tile){
		CrafterEntity entity = tile.entity();
		
		if(entity.timer.get(timerDump, 5) && entity.inventory.hasItem(result)){
			tryDump(tile, result);
		}

		//add fuel
		if(entity.inventory.getItem(fuel) > 0 && entity.burnTime <= 0f){
			entity.inventory.removeItem(fuel, 1);
			entity.burnTime += burnDuration;
			Effects.effect(burnEffect, entity.x + Mathf.range(2f), entity.y + Mathf.range(2f));
		}

		//decrement burntime
		if(entity.burnTime > 0){
			entity.burnTime -= Timers.delta();
		}

		//make sure it has all the items
		for(Item item : inputs){
			if(!entity.inventory.hasItem(item)){
				return;
			}
		}

		if(entity.inventory.getItem(result) >= capacity //output full
				|| entity.burnTime <= 0 //not burning
				|| !entity.timer.get(timerCraft, craftTime)){ //not yet time
			return;
		}

		for(Item item : inputs){
			entity.inventory.removeItem(item, 1);
		}
		
		offloadNear(tile, result);
		Effects.effect(craftEffect, entity);
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

		return (isInput && tile.entity.inventory.getItem(item) < capacity) || (item == fuel && tile.entity.inventory.getItem(fuel) < capacity);
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);

        CrafterEntity entity = tile.entity();

        //draw glowing center
        if(entity.burnTime > 0){
            Draw.color(1f, 1f, 1f, Mathf.absin(Timers.time(), 9f, 0.4f) + Mathf.random(0.05f));
            Draw.rect("smelter-middle", tile.worldx(), tile.worldy());
            Draw.color();
        }
    }

	@Override
	public TileEntity getEntity() {
		return new CrafterEntity();
	}

	public class CrafterEntity extends TileEntity{
		public float burnTime;
	}
}
