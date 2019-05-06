package io.anuke.mindustry.maps.zonegen;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.generators.BasicGenerator;
import io.anuke.mindustry.world.Tile;

public class OvergrowthGenerator extends BasicGenerator{

    public OvergrowthGenerator(int width, int height){
        super(width, height, Blocks.oreCopper, Blocks.oreLead, Blocks.oreCoal, Blocks.oreCopper);
    }

    @Override
    public void generate(int x, int y){
        floor = Blocks.moss;
    }

    @Override
    public void decorate(Tile[][] tiles){
        ores(tiles);
        terrain(tiles, Blocks.sporePine, 70f, 1.4f, 1f);

        int rand = 40;
        int border = 25;
        int spawnX = Mathf.clamp(30 + Mathf.range(rand), border, width - border), spawnY = Mathf.clamp(30 + Mathf.range(rand), border, height - border);
        int endX = Mathf.clamp(width - 30 + Mathf.range(rand), border, width - border), endY = Mathf.clamp(height - 30 + Mathf.range(rand), border, height - border);

        brush(tiles, pathfind(tiles, spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 5f : 0f) + (float)sim.octaveNoise2D(1, 1, 1f / 50f, tile.x, tile.y) * 50, manhattan), 6);
        brush(tiles, pathfind(tiles, spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 4f : 0f) + (float)sim.octaveNoise2D(1, 1, 1f / 90f, tile.x+999, tile.y) * 70, manhattan), 5);

        erase(tiles, endX, endY, 10);
        erase(tiles, spawnX, spawnY, 20);
        distort(tiles, 20f, 4f);
        inverseFloodFill(tiles, tiles[spawnX][spawnY], Blocks.sporerocks);

        noise(tiles, Blocks.darksandTaintedWater, Blocks.duneRocks, 4, 0.7f, 120f, 0.64f);
        //scatter(tiles, Blocks.sporePine, Blocks.whiteTreeDead, 1f);

        tiles[endX][endY].setOverlay(Blocks.spawn);
        loadout.setup(spawnX, spawnY);
    }
}
