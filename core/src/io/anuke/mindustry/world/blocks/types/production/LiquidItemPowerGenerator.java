package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class LiquidItemPowerGenerator extends LiquidPowerGenerator{
	public Item generateItem;
	public int itemInput = 5;
	public int itemCapacity = 30;

	public LiquidItemPowerGenerator(String name) {
		super(name);
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		super.drawPixelOverlay(tile);
		
		TileEntity entity = tile.entity();
		
		Vector2 offset = getPlaceOffset();
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx() + offset.x, tile.worldy() + 6 +
				offset.y + height*Vars.tilesize/2f, (float)entity.totalItems() / itemCapacity);
	}
	
	@Override
	public void update(Tile tile){
		LiquidPowerEntity entity = tile.entity();

		//TODO don't generate when full of energy
		if(entity.liquidAmount >= inputLiquid && entity.hasItem(generateItem, itemInput) 
				&& Timers.get(tile, "consume", generateTime)){
			entity.liquidAmount -= inputLiquid;
			entity.power += generatePower;
			
			Vector2 offset = getPlaceOffset();
			Effects.effect(generateEffect, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		if(Timers.get(tile, "generate", generateTime)){
			distributePower(tile);
		}
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == generateItem && tile.entity.totalItems() < itemCapacity;
	}

}
