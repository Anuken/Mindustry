package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.filters.FilterOption.BlockOption;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.maps.filters.FilterOption.floorsOnly;
import static io.anuke.mindustry.maps.filters.FilterOption.wallsOnly;

public class RiverNoiseFilter extends GenerateFilter{
    float scl = 40, threshold = 0f, threshold2 = 0.1f;
    Block floor = Blocks.water, floor2 = Blocks.deepwater, block = Blocks.sandRocks;

    {
        options(
        new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
        new SliderOption("threshold", () -> threshold, f -> threshold = f, -1f, 0.3f),
        new SliderOption("threshold2", () -> threshold2, f -> threshold2 = f, -1f, 0.3f),
        new BlockOption("block", () -> block, b -> block = b, wallsOnly),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
        new BlockOption("floor2", () -> floor2, b -> floor2 = b, floorsOnly)
        );
    }

    @Override
    public void apply(){
        float noise = rnoise(in.x, in.y, scl, 1f);

        if(noise >= threshold){
            in.floor = floor;

            if(in.block.solid){
                in.block = block;
            }

            if(noise >= threshold2){
                in.floor = floor2;
            }
        }
    }
}
