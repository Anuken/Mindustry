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
		PowerEntity entity = tile.entity();
		
		if(entity.power > powerAmount){
			drawLaserTo(tile, tile.rotation);
		}
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		Tile target = target(tile, tile.rotation);
		
		if(target == null) return;
		
		PowerAcceptor p = (PowerAcceptor)target.block();
		if(p.acceptsPower(target) && entity.power >= powerAmount){
			entity.power -= (powerAmount - p.addPower(target, powerAmount));
		}
	}
	
	protected void drawLaserTo(Tile tile, int rotation){
		
		Tile target = target(tile, rotation);
		
		if(target != null){
			Angles.translation(rotation * 90, 6f);
			
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
	
	protected Tile target(Tile tile, int rotation){
		if(rotation < 0)
			rotation += 4;
		rotation %= 4;
		GridPoint2 point = Geometry.getD4Points()[rotation];
		
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
