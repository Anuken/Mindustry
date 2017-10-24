package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class LiquidCrafter extends LiquidBlock{
	/**Can be null.*/
	public Item input = null;
	public int inputAmount = 5;
	public Liquid inputLiquid = null;
	public float liquidAmount = 19.99f;
	public Item output = null;
	public int itemCapacity = 90;
	public int purifyTime = 80;
	public String craftEffect = "purify";

	public LiquidCrafter(String name) {
		super(name);
		update = true;
		rotate = false;
		solid = true;
		health = 60;
		liquidCapacity = 20f;
	}
	
	@Override
	public void draw(Tile tile){
		LiquidEntity entity = tile.entity();
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
	}
	
	@Override
	public void update(Tile tile){
		LiquidEntity entity = tile.entity();
		
		if(Timers.get(tile, "purify", purifyTime) && entity.liquidAmount >= liquidAmount &&
				(input == null || entity.hasItem(input, inputAmount))){
			
			if(input != null)
				entity.removeItem(input, inputAmount);
			entity.liquidAmount -= liquidAmount;
			offloadNear(tile, output);
			Effects.effect(craftEffect, tile.worldx(), tile.worldy());
		}
		
		if(Timers.get(tile.hashCode(), "dump", 30)){
			tryDump(tile, -1, output);
		}
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		if(input == null) return;
		
		float fract = (float)tile.entity.items.get(input, 0) / itemCapacity;
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 13, fract);
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TileEntity entity = tile.entity();
		return input != null && item == input && entity.items.get(item, 0) < itemCapacity;
	}

}
