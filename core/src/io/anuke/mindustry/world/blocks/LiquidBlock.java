package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.modules.LiquidModule;
import io.anuke.ucore.graphics.Draw;

public class LiquidBlock extends Block{
	protected String liquidRegion = name() + "-liquid";
	
	public LiquidBlock(String name) {
		super(name);
		update = true;
		solid = true;
		hasLiquids = true;
		hasItems = false;
		group = BlockGroup.liquids;
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name() + "-bottom"), Draw.region(name() + "-top")};
	}
	
	@Override
	public void draw(Tile tile){
		LiquidModule mod = tile.entity.liquids;

		int rotation = rotate ? tile.getRotation() * 90 : 0;
		
		Draw.rect(name() + "-bottom", tile.drawx(), tile.drawy(), rotation);
		
		if(mod.amount > 0.001f){
			Draw.color(mod.liquid.color);
			Draw.alpha(mod.amount / liquidCapacity);
			Draw.rect(liquidRegion, tile.drawx(), tile.drawy(), rotation);
			Draw.color();
		}
		
		Draw.rect(name() + "-top", tile.drawx(), tile.drawy(), rotation);
	}
}
