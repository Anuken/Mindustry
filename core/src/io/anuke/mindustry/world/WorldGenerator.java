package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.ucore.noise.Noise;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;


public class WorldGenerator {
	
	/**Should fill spawns with the correct spawnpoints.*/
	public static void generate(Tile[][] tiles, MapTileData data){
		Noise.setSeed(world.getSeed());

		IntArray multiblocks = new IntArray();

		data.position(0, 0);
		TileDataMarker marker = data.newDataMarker();

		for(int y = 0; y < data.height(); y ++){
			for(int x = 0; x < data.width(); x ++){
				data.read(marker);

				Tile tile = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team, marker.elevation);

				Team team = Team.values()[marker.team];

				if(tile.block().isMultiblock()){
					multiblocks.add(tile.packedPosition());
				}

				if(tile.block() == StorageBlocks.core &&
						state.teams.has(team)){
					state.teams.get(team).cores.add(tile);
				}

				tiles[x][y] = tile;

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
