package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Draw;

public class Generator extends PowerBlock{
	public static final int powerTime = 8;
	
	public int powerRange = 6;
	public float powerSpeed = 0.15f;

	public Generator(String name) {
		super(name);
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		super.drawPixelOverlay(tile);
		
		Draw.color("yellow");
		Draw.dashcircle(tile.worldx(), tile.worldy(), powerRange * Vars.tilesize);
		Draw.reset();
	}
	
	protected void distributePower(Tile tile){
		PowerEntity p = tile.entity();
		//TODO have two phases, where it checks nearby blocks first, then distributes it evenly
		for(int x = -powerRange; x <= powerRange; x ++){
			for(int y = -powerRange; y <= powerRange; y ++){
				
				if(x == 0 && y == 0){
					continue;
				}
				
				if(p.power <= 0.0001f){
					return;
				}
				
				if(Vector2.dst(x, y, 0, 0) < powerRange){
					Tile dest = World.tile(tile.x + x, tile.y + y);
					if(dest != null && dest.block() instanceof PowerAcceptor){
						PowerAcceptor block = (PowerAcceptor)dest.block();
						
						float transmission = Math.min(powerSpeed, p.power);
						
						if(p.power >= 0.0001f){
							float amount = block.addPower(dest, transmission);
							p.power -= amount;
						}
					}
				}
			}
		}
	}
	
	//don't accept any power
	@Override
	public float addPower(Tile tile, float amount){
		return 0;
	}
}
