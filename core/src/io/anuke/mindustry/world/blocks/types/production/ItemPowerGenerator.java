package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
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
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Item Capacity: " + itemCapacity);
		list.add("[powerinfo]Generation: " + Strings.toFixed(powerOutput*60f, 2) + " power/s");
		list.add("[powerinfo]Generation Time: " + Strings.toFixed(itemDuration/60f, 2) + " s/item");
		list.add("[powerinfo]Input: " + generateItem);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		PowerEntity entity = tile.entity();
		
		if(entity.time > 0){
			Draw.color(heatColor);
			float alpha = (entity.hasItem(generateItem) ? 1f : Mathf.clamp(entity.time));
			alpha = alpha * 0.7f + Mathf.absin(Timers.time(), 12f, 0.3f) * alpha;
			Draw.alpha(alpha);
			Draw.rect(name + "-top", tile.worldx(), tile.worldy());
			Draw.reset();
		}
	}
	
	@Override
	public void drawSelect(Tile tile){
		super.drawSelect(tile);
		
		TileEntity entity = tile.entity;
		
		//TODO maybe don't draw it due to clutter
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 10, (float)entity.getItem(generateItem) / itemCapacity);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == generateItem && tile.entity.getItem(generateItem) < itemCapacity;
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		
		float maxPower = Math.min(powerCapacity - entity.power, powerOutput * Timers.delta());
		float mfract = maxPower/(powerOutput);
		
		if(entity.time > 0f){
			entity.time -= 1f/itemDuration*mfract;
			entity.power += maxPower;
			entity.time = Mathf.clamp(entity.time);
		}
		
		if(entity.time <= 0f && entity.hasItem(generateItem)){
			Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			entity.removeItem(generateItem, 1);
			entity.time = 1f;
		}
		
		distributeLaserPower(tile);
		
	}

}
