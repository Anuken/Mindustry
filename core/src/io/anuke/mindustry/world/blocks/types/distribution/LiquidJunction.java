package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.graphics.Draw;

public class LiquidJunction extends LiquidBlock{

	public LiquidJunction(String name) {
		super(name);
		hasLiquids = false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name)};
	}

	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(tile.x, tile.y);
		dir = (dir+4)%4;
		Tile to = tile.getNearby(dir);

        if(to.block().hasLiquids && to.block().acceptLiquid(to, tile, liquid, amount))
            to.block().handleLiquid(to, tile, liquid, amount);
	}

	@Override
	public boolean acceptLiquid(Tile dest, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(dest.x, dest.y);
		dir = (dir+4)%4;
		Tile to = dest.getNearby(dir);
		return to != null && to.block().hasLiquids &&
				to.block().acceptLiquid(to, dest, liquid, amount);
	}
}
