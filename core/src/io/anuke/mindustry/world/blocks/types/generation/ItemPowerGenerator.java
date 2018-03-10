package io.anuke.mindustry.world.blocks.types.generation;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class ItemPowerGenerator extends Generator{
	public int itemCapacity = 20;
	public Item generateItem;
	public float powerOutput;
	public float itemDuration = 70f;
	public Effect generateEffect = Fx.generatespark;
	public Color heatColor = Color.valueOf("ff9b59");

	public ItemPowerGenerator(String name) {
		super(name);
		outputOnly = true;
	}

	@Override
	public void setBars(){
		super.setBars();
		bars.replace(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.inventory.getItem(generateItem) / itemCapacity));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("powergenerationsecond", Strings.toFixed(powerOutput*60f, 2));
		stats.add("generationsecondsitem", Strings.toFixed(itemDuration/60f, 2));
		stats.add("input", generateItem);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		PowerEntity entity = tile.entity();
		
		if(entity.time > 0){
			Draw.color(heatColor);
			float alpha = (entity.inventory.hasItem(generateItem) ? 1f : Mathf.clamp(entity.time));
			alpha = alpha * 0.7f + Mathf.absin(Timers.time(), 12f, 0.3f) * alpha;
			Draw.alpha(alpha);
			Draw.rect(name + "-top", tile.worldx(), tile.worldy());
			Draw.reset();
		}
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == generateItem && tile.entity.inventory.getItem(generateItem) < itemCapacity;
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		
		float maxPower = Math.min(powerCapacity - entity.power.amount, powerOutput * Timers.delta());
		float mfract = maxPower/(powerOutput);
		
		if(entity.time > 0f){
			entity.time -= 1f/itemDuration*mfract;
			entity.power.amount += maxPower;
			entity.time = Mathf.clamp(entity.time);
		}
		
		if(entity.time <= 0f && entity.inventory.hasItem(generateItem)){
			Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			entity.inventory.removeItem(generateItem, 1);
			entity.time = 1f;
		}
		
		distributeLaserPower(tile);
		
	}

}
