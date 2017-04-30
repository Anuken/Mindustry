package io.anuke.moment.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.moment.Moment;
import io.anuke.ucore.noise.Noise;

public class Generator{
	static final int black = Color.rgba8888(Color.BLACK);
	static final int white = Color.rgba8888(Color.WHITE);
	static final int red = Color.rgba8888(Color.RED);
	static final int blue = Color.rgba8888(Color.BLUE);
	
	public static void generate(Tile[][] tiles, String mapname){
		Pixmap pix = new Pixmap(Gdx.files.internal("maps/"+mapname+".png"));
		Noise.setSeed(MathUtils.random(0, 99999));
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles.length; y ++){
				TileType floor = TileType.stone;
				TileType block = TileType.air;
				
				int color = pix.getPixel(x, pix.getHeight()-1-y);
				
				if(Noise.nnoise(x, y, 8, 1) > 0.22){
					floor = TileType.iron;
				}
				
				if(Noise.nnoise(x, y, 6, 1) > 0.245){
					floor = TileType.coal;
				}
				
				
				
				if(color == white){
					block = TileType.stoneblock;
					
				}else if(color == blue){
					Moment.i.core = tiles[x][y];
				}else if(color == red){
					Moment.i.spawnpoints.add(tiles[x][y]);
				}
				
				tiles[x][y].setBlock(block);
				tiles[x][y].setFloor(floor);
			}
		}
		
		pix.dispose();
	}
}
