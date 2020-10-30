package mindustry.maps.filters;

import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class ClearFilter extends GenerateFilter{
    protected Block block = Blocks.air;

    @Override
    public FilterOption[] options(){
        return Structs.arr(new BlockOption("block", () -> block, b -> block = b, b -> oresOnly.get(b) || wallsOnly.get(b)));
    }

    @Override
    public void apply(){

        if(in.block == block){
            in.block = Blocks.air;
        }

        if(in.overlay == block){
            in.overlay = Blocks.air;
        }
    }
}
