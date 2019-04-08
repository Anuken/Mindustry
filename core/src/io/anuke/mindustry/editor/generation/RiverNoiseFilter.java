package io.anuke.mindustry.editor.generation;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.generation.FilterOption.BlockOption;
import io.anuke.mindustry.editor.generation.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.editor.generation.FilterOption.floorsOnly;
import static io.anuke.mindustry.editor.generation.FilterOption.wallsOnly;

public class RiverNoiseFilter extends GenerateFilter{
    float scl = 40, threshold = 0f, threshold2 = 0.1f;
    Block floor = Blocks.water, floor2 = Blocks.deepwater, block = Blocks.sandRocks;

    {
        options(
        new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
        new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
        new SliderOption("threshold2", () -> threshold2, f -> threshold2 = f, 0f, 1f),
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

            if(in.srcblock.solid){
                in.block = block;
            }

            if(noise >= threshold2){
                in.floor = floor2;
            }
        }
    }
}
