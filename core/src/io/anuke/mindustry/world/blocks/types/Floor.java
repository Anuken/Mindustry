package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class Floor extends Block{
	protected Predicate<Block> blends = block -> block != this;
	protected boolean blend = true;
	
	public Floor(String name) {
		super(name);
		variants = 3;
	}
	
	@Override
	public void draw(Tile tile){
		MathUtils.random.setSeed(tile.id());
		
		Draw.rect(variants > 0 ? (name() + MathUtils.random(1, variants))  : name(), tile.worldx(), tile.worldy());
		
		if(blend)
		for(int dx = -1; dx <= 1; dx ++){
			for(int dy = -1; dy <= 1; dy ++){
				
				if(dx == 0 && dy == 0) continue;
				
				Tile other = world.tile(tile.x+dx, tile.y+dy);
				
				if(other == null) continue;
				
				Block floor = other.floor();
				
				if(floor.id <= this.id || !blends.test(floor)) continue;
				
				TextureRegion region = Draw.hasRegion(floor.name() + "edge") ? Draw.region(floor.name() + "edge") :
					Draw.region(floor.edge + "edge");
				
				int sx = -dx*8+2, sy = -dy*8+2;
				int x = Mathf.clamp(sx, 0, 12);
				int y = Mathf.clamp(sy, 0, 12);
				int w = Mathf.clamp(sx+8, 0, 12) - x, h = Mathf.clamp(sy+8, 0, 12) - y;
				
				float rx = Mathf.clamp(dx*8, 0, 8-w);
				float ry = Mathf.clamp(dy*8, 0, 8-h);
				
				temp.setTexture(region.getTexture());
				temp.setRegion(region.getRegionX()+x, region.getRegionY()+y+h, w, -h);
				
				Draw.crect(temp, tile.worldx()-4 + rx, tile.worldy()-4 + ry, w, h);
			}
		}
	}

}
