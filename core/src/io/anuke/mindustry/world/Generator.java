package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.TargetEnemy;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.noise.Noise;
import io.anuke.ucore.util.Mathf;

public class Generator{
	static final int spawn = Color.rgba8888(Color.RED);
	static final int start = Color.rgba8888(Color.GREEN);
	
	static ObjectMap<Integer, Block> colors = map(
		Hue.rgb(80, 150, 90), Blocks.grass,
		Hue.rgb(90, 180, 100), Blocks.grassblock,
		Hue.rgb(80, 110, 180), Blocks.water,
		Hue.rgb(70, 90, 150), Blocks.deepwater,
		Hue.rgb(110, 80, 30), Blocks.dirt,
		Hue.rgb(160, 120, 70), Blocks.dirtblock,
		Hue.rgb(100, 100, 100), Blocks.stoneblock,
		Color.valueOf("ed5334"), Blocks.lava,
		Color.valueOf("292929"), Blocks.oil
	);
	
	/**Returns world size.*/
	public static void generate(Pixmap pixmap){
		
		Noise.setSeed(World.getSeed());
		
		for(int x = 0; x < pixmap.getWidth(); x ++){
			for(int y = 0; y < pixmap.getHeight(); y ++){
				Block floor = Blocks.stone;
				Block block = Blocks.air;
				
				int color = pixmap.getPixel(x, pixmap.getHeight()-1-y);
				
				if(colors.containsKey(color)){
					//TODO less hacky method
					if(colors.get(color).name().contains("block")){
						block = colors.get(color);
					}else{
						floor = colors.get(color);
					}
				}else if(color == start){
					World.core = World.tile(x, y);
				}else if(color == spawn){
					World.spawnpoints.add(World.tile(x, y));
					floor = Blocks.dirt;
				}else{
					if(Mathf.chance(0.02)){
						block = Mathf.choose(Blocks.rock, Blocks.rock2);
					}
				}
				
				if(floor == Blocks.stone || floor == Blocks.grass){
					if(Noise.nnoise(x, y, 8, 1) > 0.2){
						floor = Blocks.iron;
					}
					
					if(Noise.nnoise(x, y, 6, 1) > 0.237){
						floor = Blocks.coal;
					}
					
					if(Noise.nnoise(x + 9999, y + 9999, 8, 1) > 0.253){
						floor = Blocks.titanium;
					}
					
					if(Noise.nnoise(x + 99999, y + 99999, 7, 1) > 0.258){
						floor = Blocks.uranium;
					}
				}
				
				if(block == Blocks.grassblock){
					floor = Blocks.grass;
					block = Mathf.choose(Blocks.grassblock, Blocks.grassblock2);
				}
				
				if(block == Blocks.stoneblock){
					block = Mathf.choose(Blocks.stoneblock, Blocks.stoneblock2, Blocks.stoneblock3);
				}
				
				if(floor == Blocks.grass && Mathf.chance(0.02) && block == Blocks.air){
					block = Blocks.shrub;
				}
				
				if(color == Hue.rgb(Color.PURPLE)){
					if(!Vars.android) new TargetEnemy(0).set(x * Vars.tilesize, y * Vars.tilesize).add();
					floor = Blocks.stone;
				}
				
				//preformance debugging
				//if(Vector2.dst(pixmap.getWidth()/2, pixmap.getHeight()/2, x, y) < 30){
				//	block = Mathf.choose(ProductionBlocks.stonedrill, DistributionBlocks.conveyor);
				//}
				
				World.tile(x, y).setBlock(block);
				World.tile(x, y).setFloor(floor);
				
				
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
