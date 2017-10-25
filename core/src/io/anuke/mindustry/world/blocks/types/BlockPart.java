package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

/**Used for multiblocks. Each block that is not the center of the multiblock is a blockpart.
 * Think of these as delegates to the actual block; all events are passed to the target block.*/
public class BlockPart extends Block implements PowerAcceptor, LiquidAcceptor{

	public BlockPart() {
		super("blockpart");
		//TODO note: all block parts are solid, which means you can't have non-solid multiblocks
		solid = true;
	}
	
	@Override
	public void draw(Tile tile){
		//do nothing
	}
	
	@Override
	public void handleItem(Tile tile, Item item, Tile source){
		tile.getLinked().block().handleItem(tile.getLinked(), item, source);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		return dest.getLinked().block().acceptItem(item, dest.getLinked(), source);
	}

	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		Block block = linked(tile);
		return block instanceof LiquidAcceptor 
				&& ((LiquidAcceptor)block).acceptLiquid(tile.getLinked(), source, liquid, amount);
	}

	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		Block block = linked(tile);
		((LiquidAcceptor)block).handleLiquid(tile.getLinked(), source, liquid, amount);
	}
	
	@Override
	public float getLiquid(Tile tile){
		Block block = linked(tile);
		return block instanceof LiquidAcceptor ? ((LiquidAcceptor)block).getLiquid(tile.getLinked()) : 0;
	}

	@Override
	public float getLiquidCapacity(Tile tile){
		Block block = linked(tile);
		return block instanceof LiquidAcceptor ? ((LiquidAcceptor)block).getLiquidCapacity(tile.getLinked()) : 0;
	}

	@Override
	public float addPower(Tile tile, float amount){
		Block block = linked(tile);
		if(block instanceof PowerAcceptor){
			return ((PowerAcceptor)block).addPower(tile.getLinked(), amount);
		}else{
			return amount;
		}
	}
	
	private Block linked(Tile tile){
		return tile.getLinked().block();
	}

}
