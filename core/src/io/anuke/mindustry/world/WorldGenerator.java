package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.SpecialBlocks;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.noise.Noise;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class WorldGenerator {
	public static final ObjectMap<Block, Block> rocks = new ObjectMap(){{
		put(Blocks.stone, Blocks.rock);
		put(Blocks.snow, Blocks.icerock);
		put(Blocks.grass, Blocks.shrub);
		put(Blocks.blackstone, Blocks.blackrock);
	}};
	
	/**Returns the core (starting) block. Should fill spawns with the correct spawnpoints.*/
	public static Tile generate(Pixmap pixmap, Tile[][] tiles, Array<SpawnPoint> spawns){
		Noise.setSeed(world.getSeed());

		Tile core = null;
		
		for(int x = 0; x < pixmap.getWidth(); x ++){
			for(int y = 0; y < pixmap.getHeight(); y ++){
				Block floor = Blocks.stone;
				Block block = Blocks.air;
				
				int color = pixmap.getPixel(x, pixmap.getHeight()-1-y);
				BlockPair pair = ColorMapper.get(color);
				
				if(pair != null){
					block = pair.wall;
					floor = pair.floor;
				}
					
				if(block == SpecialBlocks.playerSpawn){
					block = Blocks.air;
					core = world.tile(x, y);
				}else if(block == SpecialBlocks.enemySpawn){
					block = Blocks.air;
					spawns.add(new SpawnPoint(tiles[x][y]));
				}
				
				if(block == Blocks.air && Mathf.chance(0.025) && rocks.containsKey(floor)){
					block = rocks.get(floor);
				}
				
				if(world.getMap().oreGen && (floor == Blocks.stone || floor == Blocks.grass || floor == Blocks.blackstone ||
						floor == Blocks.snow || floor == Blocks.sand)){
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
				
				if(color == Hue.rgb(Color.PURPLE)){
					if(!Vars.mobile) new Enemy(EnemyTypes.target).set(x * tilesize, y * tilesize).add();
					floor = Blocks.stone;
				}
				
				tiles[x][y].setBlock(block, 0);
				tiles[x][y].setFloor(floor);
			}
		}

		for(int x = 0; x < pixmap.getWidth(); x ++){
			for(int y = 0; y < pixmap.getHeight(); y ++) {
				tiles[x][y].updateOcclusion();
			}
		}

		return core;
	}
}
