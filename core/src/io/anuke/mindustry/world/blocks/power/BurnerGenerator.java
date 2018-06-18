package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BurnerGenerator extends PowerGenerator {
	protected float minEfficiency = 0.2f;
	protected float powerOutput;
	protected float itemDuration = 70f;
	protected Effect generateEffect = BlockFx.generatespark;
	protected Color heatColor = Color.valueOf("ff9b59");

	public BurnerGenerator(String name) {
		super(name);
		itemCapacity = 20;
		hasItems = true;
	}

	@Override
	public void setBars(){
		super.setBars();
		bars.replace(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.items.totalItems() / itemCapacity));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		//TODO both of these depend on the item type, implement later
		//stats.add("powergenerationsecond", Strings.toFixed(powerOutput*60f, 2));
		//stats.add("generationsecondsitem", Strings.toFixed(itemDuration/60f, 2));
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);

		GeneratorEntity entity = tile.entity();
		
		if(entity.generateTime > 0){
			Draw.color(heatColor);
			float alpha = (entity.items.totalItems() > 0 ? 1f : Mathf.clamp(entity.generateTime));
			alpha = alpha * 0.7f + Mathf.absin(Timers.time(), 12f, 0.3f) * alpha;
			Draw.alpha(alpha);
			Draw.rect(name + "-top", tile.worldx(), tile.worldy());
			Draw.reset();
		}
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return getItemEfficiency(item) >= minEfficiency && tile.entity.items.totalItems() < itemCapacity;
	}
	
	@Override
	public void update(Tile tile){
		BurnerEntity entity = tile.entity();
		
		float maxPower = Math.min(powerCapacity - entity.power.amount, powerOutput * Timers.delta()) * entity.efficiency;
		float mfract = maxPower/(powerOutput);
		
		if(entity.generateTime > 0f){
			entity.generateTime -= 1f/itemDuration*mfract;
			entity.power.amount += maxPower;
			entity.generateTime = Mathf.clamp(entity.generateTime);
		}
		
		if(entity.generateTime <= 0f && entity.items.totalItems() > 0){
			Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			for(int i = 0; i < entity.items.items.length; i ++){
				if(entity.items.items[i] > 0){
					entity.items.items[i] --;
					entity.efficiency = getItemEfficiency(Item.getByID(i));
					break;
				}
			}
			entity.generateTime = 1f;
		}
		
		distributePower(tile);
		
	}

	protected float getItemEfficiency(Item item){
		return item.flammability;
	}

	@Override
	public TileEntity getEntity() {
		return new BurnerEntity();
	}

	public static class BurnerEntity extends GeneratorEntity{
		public float efficiency;

		@Override
		public void write(DataOutputStream stream) throws IOException {
			stream.writeFloat(efficiency);
		}

		@Override
		public void read(DataInputStream stream) throws IOException {
			efficiency = stream.readFloat();
		}
	}

}
