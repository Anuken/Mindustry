package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class Purifier extends Conduit{
	public Item input = null;
	public int inputAmount = 5;
	public Liquid inputLiquid = null;
	public float liquidAmount = 19.99f;
	public Item output = null;
	public int itemCapacity = 100;
	public int purifyTime = 90;

	public Purifier(String name) {
		super(name);
		update = true;
		rotate = false;
		liquidCapacity = 20f;
	}
	
	@Override
	public void draw(Tile tile){
		ConduitEntity entity = tile.entity();
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
	}
	
	@Override
	public void update(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(Timers.get(tile, "purify", purifyTime) && entity.liquidAmount >= liquidAmount &&
				entity.hasItem(input, inputAmount)){
			
			entity.removeItem(input, inputAmount);
			entity.liquidAmount -= liquidAmount;
			offloadNear(tile, output);
			Effects.effect("purify", tile.worldx(), tile.worldy());
		}
		
		if(Timers.get(tile.hashCode(), "dump", 30)){
			tryDump(tile, -1, output);
		}
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		float fract = (float)tile.entity.items.get(input, 0) / itemCapacity;
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 13, fract);
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}
	
	@Override
	public boolean accept(Item item, Tile tile, Tile source){
		TileEntity entity = tile.entity();
		return item == input && entity.items.get(item, 0) < itemCapacity;
	}

}
