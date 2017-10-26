package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;

public class ItemPowerGenerator extends Generator{
	public int itemCapacity = 20;
	public Item generateItem;
	public float generateAmount;

	public ItemPowerGenerator(String name) {
		super(name);
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
		
		if(entity.hasItem(generateItem) && tryAddPower(tile, generateAmount)){
			Effects.effect("generate", tile.entity);
			entity.removeItem(generateItem, 1);
		}
		
		distributePower(tile);
		
	}

}
