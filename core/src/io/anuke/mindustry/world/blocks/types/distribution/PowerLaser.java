package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

public class PowerLaser extends PowerBlock{
	public int laserRange = 6;
	public float powerAmount = 0.03f;
	public Color color = Color.valueOf("e54135");

	public PowerLaser(String name) {
		super(name);
		rotate = true;
		solid = true;
	}
	
	@Override
	public void drawOver(Tile tile){
		Tile target = target(tile);
		
		PowerEntity entity = tile.entity();
		
		if(target != null && entity.power > powerAmount){
			Angles.translation(tile.rotation * 90, 6f);
			
			Draw.color(Color.GRAY, Color.WHITE, 0.902f + Mathf.sin(Timers.time(), 1.7f, 0.08f));
			Draw.alpha(1f);
				
			float r = 0f;
			
			Draw.laser("laser", "laserend", 
					tile.worldx() + Angles.x() + Mathf.range(r), tile.worldy() + Angles.y() + Mathf.range(r), 
					target.worldx() - Angles.x() + Mathf.range(r), target.worldy() - Angles.y() + Mathf.range(r),
					0.7f + Mathf.sin(Timers.time(), 2f, 0.1f*0));
			
			Draw.color();
		}
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		Tile target = target(tile);
		
		if(target == null) return;
		
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
