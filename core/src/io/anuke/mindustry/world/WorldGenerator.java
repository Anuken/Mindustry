package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.ucore.noise.Noise;

import static io.anuke.mindustry.Vars.state;
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

		IntArray multiblocks = new IntArray();

		for(int y = 0; y < data.height(); y ++){
			for(int x = 0; x < data.width(); x ++){
				TileDataMarker tile = data.read();
				tiles[x][y] = new Tile(x, y, tile.floor, tile.wall == Blocks.blockpart.id ? 0 : tile.wall, tile.rotation, tile.team);

				Team team = Team.values()[tile.team];

				if(tiles[x][y].block().isMultiblock()){
					multiblocks.add(tiles[x][y].packedPosition());
				}

				if(tiles[x][y].block() == StorageBlocks.core &&
						state.teams.has(team)){
					state.teams.get(team).cores.add(tiles[x][y]);
				}

				//TODO ores, plants, extra decoration?
			}
		}

		//place multiblocks now
		for(int i = 0; i < multiblocks.size; i ++){
			int pos = multiblocks.get(i);

			int x = pos % tiles.length;
			int y = pos / tiles[0].length;

			Block result = tiles[x][y].block();
			Team team = tiles[x][y].getTeam();

			int offsetx = -(result.size-1)/2;
			int offsety = -(result.size-1)/2;

			for(int dx = 0; dx < result.size; dx ++){
				for(int dy = 0; dy < result.size; dy ++){
					int worldx = dx + offsetx + x;
					int worldy = dy + offsety + y;
					if(!(worldx == x && worldy == y)){
						Tile toplace = world.tile(worldx, worldy);
						if(toplace != null) {
							toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
							toplace.setTeam(team);
						}
					}
				}
			}
		}

		for(int x = 0; x < data.width(); x ++){
			for(int y = 0; y < data.height(); y ++) {
				tiles[x][y].updateOcclusion();
			}
		}
	}
}
