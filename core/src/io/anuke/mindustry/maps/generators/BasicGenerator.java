package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

public class BasicGenerator extends RandomGenerator{
    private Array<Item> ores;
    private Simplex sim = new Simplex();
    private Simplex sim2 = new Simplex();

    public BasicGenerator(int width, int height, Item... ores){
        super(width, height);
        this.ores = Array.with(ores);
    }

    @Override
    public void generate(Tile[][] tiles){
        int seed = Mathf.random(99999999);
        sim.setSeed(seed);
        sim2.setSeed(seed + 1);
        super.generate(tiles);
    }

    @Override
    public void generate(int x, int y){
        floor = Blocks.stone;

        if(ores != null && ((Floor) floor).hasOres){
            int offsetX = x - 4, offsetY = y + 23;
            for(int i = ores.size - 1; i >= 0; i--){
                Item entry = ores.get(i);
                if(Math.abs(0.5f - sim.octaveNoise2D(2, 0.7, 1f / (50 + i * 2), offsetX, offsetY)) > 0.23f &&
                        Math.abs(0.5f - sim2.octaveNoise2D(1, 1, 1f / (40 + i * 4), offsetX, offsetY)) > 0.32f){
                    floor = OreBlock.get(floor, entry);
                    break;
                }
            }
        }

        //rock outcrops
        double rocks = sim.octaveNoise2D(3, 0.7, 1f / 70f, x, y);
        double edgeDist = Math.min(x, Math.min(y, Math.min(Math.abs(x - (width - 1)), Math.abs(y - (height - 1)))));
        double transition = 8;
        if(edgeDist < transition){
            rocks += (transition - edgeDist) / transition / 1.5;
        }

        if(rocks > 0.64){
            block = Blocks.rocks;
        }
    }
}
