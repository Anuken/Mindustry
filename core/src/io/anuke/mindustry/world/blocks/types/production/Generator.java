package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Generator extends PowerBlock{
	public static final int powerTime = 2;

	public int powerRange = 6;
	public float powerSpeed = 0.2f;

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
		if(!Timers.get(tile, "generate", powerTime)){
			return;
		}

		PowerEntity p = tile.entity();
		
		int acceptors = 0;
		float flow = 0f;

		//TODO have two phases, where it checks nearby blocks first, then distributes it evenly
		for(int i = 0; i < 2; i++){
			for(int x = -powerRange; x <= powerRange; x++){
				for(int y = -powerRange; y <= powerRange; y++){

					if(x == 0 && y == 0){
						continue;
					}

					if(Vector2.dst(x, y, 0, 0) < powerRange){
						Tile dest = World.tile(tile.x + x, tile.y + y);
						if(dest != null && dest.block() instanceof PowerAcceptor){
							if(i == 1){
								PowerAcceptor block = (PowerAcceptor) dest.block();

								float transmission = Math.min(flow, p.power);

								float amount = block.addPower(dest, transmission);
								p.power -= amount;
							}else{
								acceptors ++;
							}
						}
					}
				}
			}
			
			if(i == 0 && acceptors > 0){
				flow = Mathf.clamp(powerSpeed / acceptors, 0f, p.power / acceptors);
			}
		}
	}

	//don't accept any power
	@Override
	public float addPower(Tile tile, float amount){
		return amount;
	}
}
