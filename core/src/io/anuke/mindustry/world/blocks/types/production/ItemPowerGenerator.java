package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class ItemPowerGenerator extends Generator{
	public int itemCapacity = 20;
	public Item generateItem;
	public float generateAmount;
	public float generateTime = 2f;

	public ItemPowerGenerator(String name) {
		super(name);
		outputOnly = true;
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		super.drawPixelOverlay(tile);
		
		TileEntity entity = tile.entity;
		
		//TODO maybe don't draw it due to clutter
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 10, (float)entity.totalItems() / itemCapacity);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == generateItem && tile.entity.totalItems() < itemCapacity;
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		
		if(Timers.get(tile, "generate", generateTime) && entity.hasItem(generateItem) && tryAddPower(tile, generateAmount)){
			Effects.effect(Fx.generate, tile.entity);
			entity.removeItem(generateItem, 1);
		}
		
		distributeLaserPower(tile);
		
	}

}
