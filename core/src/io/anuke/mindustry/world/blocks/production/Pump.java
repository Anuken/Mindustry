package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Pump extends LiquidBlock{
	protected final Array<Tile> drawTiles = new Array<>();
	protected final Array<Tile> updateTiles = new Array<>();

	/**Pump amount per tile this block is on.*/
	protected float pumpAmount = 1f;
	/**Power used per frame per tile this block is on.*/
	protected float powerUse = 0f;
	/**Maximum liquid tier this pump can use.*/
	protected int tier = 0;

	public Pump(String name) {
		super(name);
		layer = Layer.overlay;
		liquidFlowFactor = 3f;
		group = BlockGroup.liquids;
		liquidRegion = "pump-liquid";
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(BlockStat.liquidOutput, 60f*pumpAmount);
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.drawx(), tile.drawy());
		
		Draw.color(tile.entity.liquids.liquid.color);
		Draw.alpha(tile.entity.liquids.amount / liquidCapacity);
		Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
		Draw.color();
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name)};
	}

	@Override
	public boolean canPlaceOn(Tile tile) {
		if(isMultiblock()){
			Liquid last = null;
			for(Tile other : tile.getLinkedTiles(drawTiles)){
				//can't place pump on block with multiple liquids
				if(last != null && other.floor().liquidDrop != last){
					return false;
				}

				if(isValid(other)){
					last = other.floor().liquidDrop;
				}
			}
			return last != null;
		}else{
			return isValid(tile);
		}
	}
	
	@Override
	public void update(Tile tile){
		float tiles = 0f;
		Liquid liquidDrop = null;

		if(isMultiblock()){
			for(Tile other : tile.getLinkedTiles(updateTiles)){
				if(isValid(other)){
					liquidDrop = other.floor().liquidDrop;
					tiles ++;
				}
			}
		}else{
			tiles = 1f;
			liquidDrop = tile.floor().liquidDrop;
		}

		if(hasPower){
			float used = Math.min(powerCapacity, tiles * powerUse * Timers.delta());

			//multiply liquid obtained by the fraction of power this pump has to pump it
			//e.g. only has 50% power required = only pumps 50% of liquid that it can
			tiles *= Mathf.clamp(tile.entity.power.amount / used);

			tile.entity.power.amount -= Math.min(tile.entity.power.amount, used);
		}

		if(liquidDrop != null){
			float maxPump = Math.min(liquidCapacity - tile.entity.liquids.amount, tiles * pumpAmount * Timers.delta());
			tile.entity.liquids.liquid = liquidDrop;
			tile.entity.liquids.amount += maxPump;
		}

		tryDumpLiquid(tile);
	}

	protected boolean isValid(Tile tile){
		return tile.floor().liquidDrop != null && tier >= tile.floor().liquidDrop.tier;
	}

}
