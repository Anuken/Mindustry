package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.noise.Noise;

import static io.anuke.mindustry.Vars.world;


public class WorldGenerator {
	public static final ObjectMap<Block, Block> rocks = new ObjectMap<Block, Block>(){{
		put(Blocks.stone, Blocks.rock);
		put(Blocks.snow, Blocks.icerock);
		put(Blocks.grass, Blocks.shrub);
		put(Blocks.blackstone, Blocks.blackrock);
	}};
	
	/**Should fill spawns with the correct spawnpoints.*/
	public static void generate(Tile[][] tiles, MapTileData data){
		Noise.setSeed(world.getSeed());
		
		for(int x = 0; x < data.width(); x ++){
			for(int y = 0; y < data.height(); y ++){
				TileDataMarker tile = data.read();
				tiles[x][y] = new Tile(x, y, tile.floor, tile.wall, tile.rotation, tile.team);

				//TODO ores, plants, extra decoration?
			}
		}

		for(int x = 0; x < data.width(); x ++){
			for(int y = 0; y < data.height(); y ++) {
				tiles[x][y].updateOcclusion();
			}
		}
	}
}
