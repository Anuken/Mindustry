package io.anuke.mindustry.world.mapgen;

import com.badlogic.gdx.math.GridPoint2;
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
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SeedRandom;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;


public class WorldGenerator {
	static int oreIndex = 0;
	
	/**Should fill spawns with the correct spawnpoints.*/
	public static void loadTileData(Tile[][] tiles, MapTileData data, boolean genOres, int seed){
		data.position(0, 0);
		TileDataMarker marker = data.newDataMarker();

		for(int y = 0; y < data.height(); y ++){
			for(int x = 0; x < data.width(); x ++){
				data.read(marker);
				
				tiles[x][y] = new Tile(x, y, marker.floor, marker.wall == Blocks.blockpart.id ? 0 : marker.wall, marker.rotation, marker.team, marker.elevation);
			}
		}

		prepareTiles(tiles, seed, genOres);
	}

	public static void prepareTiles(Tile[][] tiles, int seed, boolean genOres){
		
		//find multiblocks
		IntArray multiblocks = new IntArray();

		for(int x = 0; x < tiles.length; x ++) {
			for (int y = 0; y < tiles[0].length; y++) {
				Tile tile = tiles[x][y];
				
				Team team = tile.getTeam();

				if(tile.block() == StorageBlocks.core &&
						state.teams.has(team)){
					state.teams.get(team).cores.add(tile);
				}
				
				if(tiles[x][y].block().isMultiblock()){
					multiblocks.add(tiles[x][y].packedPosition());
				}
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
		for(int x = 0; x < tiles.length; x ++){
			for(int y = 0; y < tiles[0].length; y ++) {
				Tile tile = tiles[x][y];

				tile.updateOcclusion();

				//fix things on cliffs that shouldn't be
				if(tile.block() != Blocks.air && tile.cliffs != 0){
					tile.setBlock(Blocks.air);
				}
			}
		}

		oreIndex = 0;

		if(genOres) {
			Array<OreEntry> ores = Array.with(
					new OreEntry(Items.tungsten, 0.3f, seed),
					new OreEntry(Items.coal, 0.284f, seed),
					new OreEntry(Items.lead, 0.28f, seed),
					new OreEntry(Items.titanium, 0.27f, seed),
					new OreEntry(Items.thorium, 0.26f, seed)
			);

			for (int x = 0; x < tiles.length; x++) {
				for (int y = 0; y < tiles[0].length; y++) {

					Tile tile = tiles[x][y];

					if(!tile.floor().hasOres || tile.cliffs != 0 || tile.block() != Blocks.air){
						continue;
					}

					for(int i = ores.size-1; i >= 0; i --){
						OreEntry entry = ores.get(i);
						if(entry.noise.octaveNoise2D(2, 0.7, 1f / (2 + i*2), x, y)/2f +
								entry.ridge.getValue(x, y, 1f / (28 + i*4)) >= 2.0f - entry.frequency*4.0f
								&& entry.ridge.getValue(x+9999, y+9999, 1f/100f) > 0.4){
							tile.setFloor((Floor) OreBlocks.get(tile.floor(), entry.item));
							break;
						}
					}
				}
			}
		}
	}

	public static void generateMap(Tile[][] tiles, int seed){
		Simplex sim = new Simplex(Mathf.random(99999));
		Simplex sim2 = new Simplex(Mathf.random(99999));
		Simplex sim3 = new Simplex(Mathf.random(99999));

		SeedRandom random = new SeedRandom(Mathf.random(99999));

		int width = tiles.length, height = tiles[0].length;

		ObjectMap<Block, Block> decoration = new ObjectMap<>();

		decoration.put(Blocks.grass, Blocks.shrub);
		decoration.put(Blocks.stone, Blocks.rock);
		decoration.put(Blocks.ice, Blocks.icerock);
		decoration.put(Blocks.snow, Blocks.icerock);
		decoration.put(Blocks.blackstone, Blocks.blackrock);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Block floor = Blocks.stone;
				Block wall = Blocks.air;

				double elevation = sim.octaveNoise2D(3, 0.5, 1f/100, x, y) * 4.1 - 1;
				double temp = sim3.octaveNoise2D(7, 0.53, 1f/320f, x, y);

				double r = sim2.octaveNoise2D(1, 0.6, 1f/70, x, y);
				double edgeDist = Math.max(width/2, height/2) - Math.max(Math.abs(x - width/2), Math.abs(y - height/2));
				double dst = Vector2.dst(width/2, height/2, x, y);
				double elevDip = 30;

				double border = 14;

				if(edgeDist < border){
					elevation += (border - edgeDist)/6.0;
				}

				if(temp < 0.35){
					floor = Blocks.snow;
				}else if(temp < 0.45){
					floor = Blocks.stone;
				}else if(temp < 0.65){
					floor = Blocks.grass;
				}else if(temp < 0.8){
					floor = Blocks.sand;
				}else if(temp < 0.9){
					floor = Blocks.blackstone;
					elevation = 0f;
				}else{
					floor = Blocks.lava;
				}

				if(dst < elevDip){
					elevation -= (elevDip - dst)/elevDip * 3.0;
				}else if(r > 0.9){
					floor = Blocks.water;
					elevation = 0;

					if(r > 0.94){
						floor = Blocks.deepwater;
					}
				}

				if(wall == Blocks.air && decoration.containsKey(floor) && random.chance(0.03)){
					wall = decoration.get(floor);
				}

				Tile tile = new Tile(x, y, (byte)floor.id, (byte)wall.id);
				tile.elevation = (byte)Math.max(elevation, 0);
				tiles[x][y] = tile;
			}
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Tile tile = tiles[x][y];

				byte elevation = tile.elevation;

				for(GridPoint2 point : Geometry.d4){
					if(!Mathf.inBounds(x + point.x, y + point.y, width, height)) continue;
					if(tiles[x + point.x][y + point.y].elevation < elevation){

						if(Mathf.chance(0.05)){
							tile.elevation = -1;
						}
						break;
					}
				}
			}
		}

		tiles[width/2][height/2].setBlock(StorageBlocks.core);
		tiles[width/2][height/2].setTeam(Team.blue);
		
		prepareTiles(tiles, seed, true);
	}

	public static class OreEntry{
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
