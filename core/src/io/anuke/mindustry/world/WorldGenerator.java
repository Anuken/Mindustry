package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;


public class WorldGenerator {
	static int oreIndex = 0;
	
	/**Should fill spawns with the correct spawnpoints.*/
	public static void generate(Tile[][] tiles, MapTileData data, boolean genOres, int seed){
		oreIndex = 0;

		Array<OreEntry> ores = Array.with(
				new OreEntry(Blocks.iron, 0.3f, seed),
				new OreEntry(Blocks.coal, 0.284f, seed),
				new OreEntry(Blocks.lead, 0.28f, seed),
				new OreEntry(Blocks.titanium, 0.27f, seed),
				new OreEntry(Blocks.thorium, 0.26f, seed)
		);

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

		//update cliffs, occlusion data
		for(int x = 0; x < data.width(); x ++){
			for(int y = 0; y < data.height(); y ++) {
				tiles[x][y].updateOcclusion();
			}
		}

		if(genOres) {

			for (int x = 0; x < data.width(); x++) {
				for (int y = 0; y < data.height(); y++) {

					Tile tile = tiles[x][y];

					if(!tile.floor().hasOres || tile.cliffs != 0 || tile.block() != Blocks.air){
						continue;
					}

					for(int i = ores.size-1; i >= 0; i --){
						OreEntry entry = ores.get(i);
						if(entry.noise.octaveNoise2D(2, 0.7, 1f / (2 + i*2), x, y)/2f +
								entry.ridge.getValue(x, y, 1f / (28 + i*4)) >= 2.0f - entry.frequency*4.0f
								&& entry.ridge.getValue(x+9999, y+9999, 1f/100f) > 0.4){
							tile.setFloor(entry.block);
						}
					}
				}
			}
		}
	}

	static class OreEntry{
		final float frequency;
		final Block block;
		final Simplex noise;
		final RidgedPerlin ridge;
		final int index;

		OreEntry(Block block, float frequency, int seed) {
			this.frequency = frequency;
			this.block = block;
			this.noise = new Simplex(seed + oreIndex);
			this.ridge = new RidgedPerlin(seed + oreIndex, 2);
			this.index = oreIndex ++;
		}
	}
}
