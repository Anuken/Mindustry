package io.anuke.mindustry.world.mapgen;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.OreBlocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.DataPosition;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SeedRandom;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;


public class WorldGenerator {
	static int oreIndex = 0;
	
	/**Should fill spawns with the correct spawnpoints.*/
	public static void generate(Tile[][] tiles, MapTileData data, boolean genOres, int seed){
		oreIndex = 0;

		Array<OreEntry> ores = Array.with(
			new OreEntry(Items.tungsten, 0.3f, seed),
			new OreEntry(Items.coal, 0.284f, seed),
			new OreEntry(Items.lead, 0.28f, seed),
			new OreEntry(Items.titanium, 0.27f, seed),
			new OreEntry(Items.thorium, 0.26f, seed)
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

				//fix things on cliffs that shouldn't be
				if(tiles[x][y].block() != Blocks.air && tiles[x][y].cliffs != 0){
					tiles[x][y].setBlock(Blocks.air);
				}
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
							tile.setFloor(OreBlocks.get(tile.floor(), entry.item));
							break;
						}
					}
				}
			}
		}
	}

	public static MapTileData generate(){
		Simplex sim = new Simplex(Mathf.random(99999));
		Simplex sim2 = new Simplex(Mathf.random(99999));
		Simplex sim3 = new Simplex(Mathf.random(99999));

		SeedRandom random = new SeedRandom(Mathf.random(99999));

		MapTileData data = new MapTileData(300, 300);
		TileDataMarker marker = data.newDataMarker();

		ObjectMap<Block, Block> decoration = new ObjectMap<>();

		decoration.put(Blocks.grass, Blocks.shrub);
		decoration.put(Blocks.stone, Blocks.rock);

		for (int x = 0; x < data.width(); x++) {
			for (int y = 0; y < data.height(); y++) {
				marker.floor = (byte)Blocks.stone.id;

				double r = sim2.octaveNoise2D(1, 0.6, 1f/70, x, y);
				double elevation = sim.octaveNoise2D(3, 0.5, 1f/70, x, y) * 4 - 1.2;
				double edgeDist = Math.max(data.width()/2, data.height()/2) - Math.max(Math.abs(x - data.width()/2), Math.abs(y - data.height()/2));
				double dst = Vector2.dst(data.width()/2, data.height()/2, x, y);

				double border = 14;

				if(edgeDist < border){
					elevation += (border - edgeDist)/6.0;
				}

				if(sim3.octaveNoise2D(6, 0.5, 1f/120f, x, y) > 0.5){
					marker.floor = (byte)Blocks.grass.id;
				}

				if(dst < 20){
					elevation = 0;
				}else if(r > 0.9){
					marker.floor = (byte)Blocks.water.id;
					elevation = 0;

					if(r > 0.94){
						marker.floor = (byte)Blocks.deepwater.id;
					}
				}

				marker.elevation = (byte)Math.max(elevation, 0);

				if(marker.wall == 0 && decoration.containsKey(Block.getByID(marker.floor)) && random.chance(0.03)){
					marker.wall = (byte)decoration.get(Block.getByID(marker.floor)).id;
				}

				data.write(marker);

				marker.wall = 0;
			}
		}
		data.write(data.width()/2, data.height()/2, DataPosition.wall, (byte)StorageBlocks.core.id);
		data.write(data.width()/2, data.height()/2, DataPosition.rotationTeam, Bits.packByte((byte)0, (byte)Team.blue.ordinal()));
		return data;
	}

	static class OreEntry{
		final float frequency;
		final Item item;
		final Simplex noise;
		final RidgedPerlin ridge;
		final int index;

		OreEntry(Item item, float frequency, int seed) {
			this.frequency = frequency;
			this.item = item;
			this.noise = new Simplex(seed + oreIndex);
			this.ridge = new RidgedPerlin(seed + oreIndex, 2);
			this.index = oreIndex ++;
		}
	}
}
