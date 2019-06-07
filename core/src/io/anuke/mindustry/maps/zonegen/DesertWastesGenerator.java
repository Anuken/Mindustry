package io.anuke.mindustry.maps.zonegen;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.generators.BasicGenerator;
import io.anuke.mindustry.world.Tile;

public class DesertWastesGenerator extends BasicGenerator{

    public DesertWastesGenerator(int width, int height){
        super(width, height, Blocks.oreCopper, Blocks.oreLead, Blocks.oreCoal, Blocks.oreCopper);
    }

    @Override
    public void generate(int x, int y){
        floor = Blocks.sand;
    }

    @Override
    public void decorate(Tile[][] tiles){
        ores(tiles);
        terrain(tiles, Blocks.sandRocks, 60f, 1.5f, 0.9f);

        int rand = 40;
        int border = 25;
        int spawnX = Mathf.clamp(30 + Mathf.range(rand), border, width - border), spawnY = Mathf.clamp(30 + Mathf.range(rand), border, height - border);
        int endX = Mathf.clamp(width - 30 + Mathf.range(rand), border, width - border), endY = Mathf.clamp(height - 30 + Mathf.range(rand), border, height - border);

        brush(tiles, pathfind(tiles, spawnX, spawnY, endX, endY, tile -> tile.solid() ? 5f : 0f, manhattan), 6);
        brush(tiles, pathfind(tiles, spawnX, spawnY, endX, endY, tile -> tile.solid() ? 4f : 0f + (float)sim.octaveNoise2D(1, 1, 1f / 40f, tile.x, tile.y) * 20, manhattan), 5);

        erase(tiles, endX, endY, 10);
        erase(tiles, spawnX, spawnY, 20);
        distort(tiles, 20f, 4f);
        inverseFloodFill(tiles, tiles[spawnX][spawnY], Blocks.sandRocks);

        noise(tiles, Blocks.salt, Blocks.saltRocks, 5, 0.6f, 200f, 0.55f);
        noise(tiles, Blocks.darksand, Blocks.duneRocks, 5, 0.7f, 120f, 0.5f);

        tech(tiles);
        overlay(tiles, Blocks.sand, Blocks.pebbles, 0.15f, 5, 0.8f, 30f, 0.62f);
        //scatter(tiles, Blocks.sandRocks, Blocks.creeptree, 1f);

        tiles[endX][endY].setOverlay(Blocks.spawn);
        loadout.setup(spawnX, spawnY);
    }
}
