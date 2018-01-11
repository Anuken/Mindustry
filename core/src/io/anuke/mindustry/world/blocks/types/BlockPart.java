package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

/**Used for multiblocks. Each block that is not the center of the multiblock is a blockpart.
 * Think of these as delegates to the actual block; all events are passed to the target block.
 * They are made to share all properties from the linked tile/block.*/
public class BlockPart extends Block implements PowerAcceptor, LiquidAcceptor{

	public BlockPart() {
		super("blockpart");
		solid = false;
	}
	
	@Override
	public void draw(Tile tile){
		//do nothing
	}
	
	@Override
	public boolean isSolidFor(Tile tile){
		return tile.getLinked() == null
				|| (tile.getLinked().block() instanceof BlockPart || tile.getLinked().solid()
				|| tile.getLinked().block().isSolidFor(tile.getLinked()));
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		tile.getLinked().block().handleItem(item, tile.getLinked(), source);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return tile.getLinked().block().acceptItem(item, tile.getLinked(), source);
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
	
	@Override
	public boolean acceptsPower(Tile tile){
		Block block = linked(tile);
		if(block instanceof PowerAcceptor){
			return ((PowerAcceptor)block).acceptsPower(tile.getLinked());
		}else{
			return false;
		}
	}

	@Override
	public void setPower(Tile tile, float power){
		Block block = linked(tile);
		
		if(block instanceof PowerAcceptor){
			((PowerAcceptor)block).setPower(tile.getLinked(), power);
		}
	}
	
	private Block linked(Tile tile){
		return tile.getLinked().block();
	}

}
