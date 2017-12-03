package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.TargetEnemy;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.noise.Noise;
import io.anuke.ucore.util.Mathf;

public class Generator{
	static final int spawn = Color.rgba8888(Color.RED);
	static final int start = Color.rgba8888(Color.GREEN);
	
	public static ObjectMap<Integer, Block> colors = map(
		Hue.rgb(80, 150, 90), Blocks.grass,
		Hue.rgb(90, 180, 100), Blocks.grassblock,
		Hue.rgb(80, 110, 180), Blocks.water,
		Hue.rgb(70, 90, 150), Blocks.deepwater,
		Hue.rgb(110, 80, 30), Blocks.dirt,
		Hue.rgb(160, 120, 70), Blocks.dirtblock,
		Hue.rgb(100, 100, 100), Blocks.stoneblock,
		Color.valueOf("323232"), Blocks.stone,
		Color.valueOf("575757"), Blocks.blackstoneblock,
		Color.valueOf("252525"), Blocks.blackstone,
		Color.valueOf("ed5334"), Blocks.lava,
		Color.valueOf("292929"), Blocks.oil,
		Color.valueOf("e5d8bb"), Blocks.sandblock,
		Color.valueOf("988a67"), Blocks.sand,
		Color.valueOf("f7feff"), Blocks.snowblock,
		Color.valueOf("c2d1d2"), Blocks.snow,
		Color.valueOf("c4e3e7"), Blocks.ice
	);
	
	/**Returns world size.*/
	public static void generate(Pixmap pixmap, Tile[][] tiles){
		
		Noise.setSeed(Vars.world.getSeed());
		
		for(int x = 0; x < pixmap.getWidth(); x ++){
			for(int y = 0; y < pixmap.getHeight(); y ++){
				Block floor = Blocks.stone;
				Block block = Blocks.air;
				
				int color = pixmap.getPixel(x, pixmap.getHeight()-1-y);
				
				if(colors.containsKey(color)){
					//TODO less hacky method
					if(!(colors.get(color) instanceof Floor)){
						block = colors.get(color);
					}else{
						floor = colors.get(color);
					}
				}else if(color == start){
					Vars.control.setCore(Vars.world.tile(x, y));
				}else if(color == spawn){
					Vars.control.addSpawnPoint(Vars.world.tile(x, y));
					floor = Blocks.dirt;
				}
				
				if(block == Blocks.air){
					if(floor == Blocks.stone && Mathf.chance(0.02)){
						block = Blocks.rock;
					}
					
					if(floor == Blocks.snow && Mathf.chance(0.02)){
						block = Blocks.icerock;
					}
					
					if(floor == Blocks.blackstone && Mathf.chance(0.03)){
						block = Blocks.blackrock;
					}
				}
				
				if(floor == Blocks.stone || floor == Blocks.grass || floor == Blocks.blackstone ||
						floor == Blocks.snow || floor == Blocks.sand){
					if(Noise.nnoise(x, y, 8, 1) > 0.21){
						floor = Blocks.iron;
					}
					
					if(Noise.nnoise(x, y, 6, 1) > 0.237){
						floor = Blocks.coal;
					}
					
					if(Noise.nnoise(x + 9999, y + 9999, 8, 1) > 0.27){
						floor = Blocks.titanium;
					}
					
					if(Noise.nnoise(x + 99999, y + 99999, 7, 1) > 0.259){
						floor = Blocks.uranium;
					}
				}
				
				if(block == Blocks.grassblock){
					floor = Blocks.grass;
				}
				
				if(block == Blocks.snowblock){
					floor = Blocks.snow;
				}
				
				if(block == Blocks.sandblock){
					floor = Blocks.sand;
				}
				
				if(floor == Blocks.grass && Mathf.chance(0.03) && block == Blocks.air){
					block = Blocks.shrub;
				}
				
				if(color == Hue.rgb(Color.PURPLE)){
					if(!Vars.android) new TargetEnemy().set(x * Vars.tilesize, y * Vars.tilesize).add();
					floor = Blocks.stone;
				}
				
				//preformance debugging
				//if(Vector2.dst(pixmap.getWidth()/2, pixmap.getHeight()/2, x, y) < 30){
				//	block = Mathf.choose(ProductionBlocks.stonedrill, DistributionBlocks.conveyor);
				//}
				
				tiles[x][y].setBlock(block, 0);
				tiles[x][y].setFloor(floor);
			}
		}
	}
	
	private static ObjectMap<Integer, Block> map(Object...objects){
		
		ObjectMap<Integer, Block> out = new ObjectMap<>();
		
		for(int i = 0; i < objects.length; i += 2){
			out.put(Hue.rgb((Color)objects[i]), (Block)objects[i+1]);
		}
		
		return out;
	}
}
