package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.content.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.maps.filters.FilterOption.*;

public class ClearFilter extends GenerateFilter{
    protected Block block = Blocks.air;

    {
        options(
        new BlockOption("block", () -> block, b -> block = b, wallsOnly)
        );
    }

    @Override
    public void apply(){

        if(in.block == block){
            in.block = Blocks.air;
        }
    }
}
