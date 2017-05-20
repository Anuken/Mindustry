package io.anuke.mindustry.world;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

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
		Hue.rgb(100, 100, 100), Blocks.stoneblock
	);
	
	/**Returns world size.*/
	public static void generate(int map){
		Pixmap pix = mapPixmaps[map];
		
		Noise.setSeed(MathUtils.random(0, 99999));
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles.length; y ++){
				Block floor = Blocks.stone;
				Block block = Blocks.air;
				
				int color = pix.getPixel(x, pix.getHeight()-1-y);
				
				if(colors.containsKey(color)){
					//TODO less hacky method
					if(colors.get(color).name().contains("block")){
						block = colors.get(color);
					}else{
						floor = colors.get(color);
					}
				}else if(color == start){
					core = tiles[x][y];
				}else if(color == spawn){
					spawnpoints.add(tiles[x][y]);
				}else{
					if(Mathf.chance(0.02)){
						block = Mathf.choose(Blocks.rock, Blocks.rock2);
					}
				}
				
				if(floor == Blocks.stone || floor == Blocks.grass){
					if(Noise.nnoise(x, y, 8, 1) > 0.2){
						floor = Blocks.iron;
					}
					
					if(Noise.nnoise(x, y, 6, 1) > 0.242){
						floor = Blocks.coal;
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
				
				tiles[x][y].setBlock(block);
				tiles[x][y].setFloor(floor);
			}
		}
	}
	
	public static void loadMaps(){
		mapPixmaps = new Pixmap[maps.length];
		mapTextures = new Texture[maps.length];
		
		for(int i = 0; i < maps.length; i ++){
			Pixmap pix = new Pixmap(Gdx.files.internal("maps/"+maps[i]+".png"));
			mapPixmaps[i] = pix;
			mapTextures[i] = new Texture(pix);
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
