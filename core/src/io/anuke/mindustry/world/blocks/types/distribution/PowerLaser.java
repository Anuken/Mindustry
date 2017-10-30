package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.math.GridPoint2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;

public class PowerLaser extends PowerBlock{
	public int laserRange = 4;
	public float powerAmount = 0.01f;

	public PowerLaser(String name) {
		super(name);
		rotate = true;
	}
	
	@Override
	public void drawOver(Tile tile){
		Tile target = target(tile);
		
		if(target != null){
			Draw.laser("laser", "laserend", tile.worldx(), tile.worldy(), target.worldx(), target.worldy());
		}else{
			Angles.translation(tile.rotation*90, laserRange*Vars.tilesize);
			
			Draw.laser("laser", "laserend", tile.worldx(), tile.worldy(), tile.worldx() + Angles.x(), tile.worldy() + Angles.y());
		}
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		Tile target = target(tile);
		
		PowerAcceptor p = (PowerAcceptor)target.block();
		if(p.acceptsPower(target) && entity.power >= powerAmount){
			entity.power -= (powerAmount - p.addPower(target, powerAmount));
		}
	}
	
	private Tile target(Tile tile){
		GridPoint2 point = Geometry.getD4Points()[tile.rotation];
		
		int i = 0;
		
		for(i = 1; i < laserRange; i ++){
			Tile other = World.tile(tile.x + i * point.x, tile.y + i * point.y);
			
			if(other != null && other.block() instanceof PowerAcceptor){
				return other;
			}
		}
		return null;
	}
}
