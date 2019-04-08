package io.anuke.mindustry.editor.generation;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.generation.FilterOption.BlockOption;
import io.anuke.mindustry.editor.generation.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.editor.generation.FilterOption.floorsOnly;
import static io.anuke.mindustry.editor.generation.FilterOption.wallsOnly;

public class ScatterFilter extends GenerateFilter{
    float chance = 0.1f;
    Block floor = Blocks.ice, block = Blocks.icerocks;

    {
        options(
        new SliderOption("chance", () -> chance, f -> chance = f, 0f, 1f),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
        new BlockOption("block", () -> block, b -> block = b, wallsOnly)
        );
    }

    @Override
    public void apply(){

        if(in.srcfloor == floor && in.srcblock == Blocks.air && chance() <= chance){
            in.block = block;
        }
    }
}
