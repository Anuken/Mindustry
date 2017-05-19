package io.anuke.mindustry.world;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.noise.Noise;
import io.anuke.ucore.util.Mathf;

public class Generator{
	static final int stonefloor = Color.rgba8888(Hue.rgb(54, 54, 54));
	static final int stone = Color.rgba8888(Hue.rgb(128, 128, 128));
	static final int spawn = Color.rgba8888(Color.RED);
	static final int start = Color.rgba8888(Color.GREEN);
	
	/**Returns world size.*/
	public static void generate(int map){
		Pixmap pix = mapPixmaps[map];
		
		Noise.setSeed(MathUtils.random(0, 99999));
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles.length; y ++){
				Block floor = Blocks.stone;
				Block block = Blocks.air;
				
				int color = pix.getPixel(x, pix.getHeight()-1-y);
				
				if(Noise.nnoise(x, y, 8, 1) > 0.22){
					floor = Blocks.iron;
				}
				
				if(Noise.nnoise(x, y, 8, 1) > 0.1){
					floor = Blocks.grass;
				}
				
				if(Noise.nnoise(x, y, 8, 1) > 0.1){
					floor = Blocks.water;
				}
				
				if(Mathf.chance(0.01)){
					block = Blocks.rock;
				}
				
				if(Mathf.chance(0.01)){
					block = Blocks.rock2;
				}
				
				if(Noise.nnoise(x, y, 6, 1) > 0.245){
					floor = Blocks.coal;
				}
				if(color == stone && map == 1){
					block = Blocks.dirtblock;
				}else if(color == stone){
					block = Mathf.choose(Blocks.stoneblock, Blocks.stoneblock2, Blocks.stoneblock3);
				}else if(color == start){
					core = tiles[x][y];
				}else if(color == spawn){
					spawnpoints.add(tiles[x][y]);
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
}
