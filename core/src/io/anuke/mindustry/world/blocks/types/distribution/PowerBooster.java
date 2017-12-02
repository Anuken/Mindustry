package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.production.Generator;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class PowerBooster extends Generator{
	
	public PowerBooster(String name) {
		super(name);
		drawRadius = true;
		explosive = false;
		hasLasers = false;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Range: " + powerRange + " tiles");
	}
	
	@Override
	public void update(Tile tile){
		distributePower(tile);
	}
	
	@Override
	public void drawOver(Tile tile){}
	
	@Override
	public boolean acceptsPower(Tile tile){
		PowerEntity entity = tile.entity();
		
		return entity.power + 0.001f <= powerCapacity;
	}
	
	//TODO better distribution
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
							Tile dest = Vars.world.tile(tile.x + x, tile.y + y);
							if(dest != null && dest.block() instanceof PowerAcceptor && ((PowerAcceptor) dest.block()).acceptsPower(dest)){
								if(i == 1){
									PowerAcceptor block = (PowerAcceptor) dest.block();

									float transmission = Math.min(flow, p.power);

									float amount = block.addPower(dest, transmission);
									p.power -= amount;
								}else{
									acceptors++;
								}
							}
						}
					}
				}

				//TODO better distribution scheme
				if(i == 0 && acceptors > 0){
					flow = Mathf.clamp(p.power / acceptors, 0f, powerSpeed / acceptors);
				}
			}
		}
}
