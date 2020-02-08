package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

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
