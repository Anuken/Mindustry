package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class ClearFilter extends GenerateFilter{
    protected Block block = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new BlockOption[]{
            new BlockOption("block", () -> block, b -> block = b, b -> oresOnly.get(b) || wallsOnly.get(b))
        };
    }

    @Override
    public char icon(){
        return Iconc.blockSnow;
    }

    @Override
    public void apply(GenerateInput in){

        if(in.block == block){
            in.block = Blocks.air;
        }

        if(in.overlay == block){
            in.overlay = Blocks.air;
        }
    }
}
