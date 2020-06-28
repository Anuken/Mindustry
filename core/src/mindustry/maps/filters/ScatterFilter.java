package mindustry.maps.filters;

import arc.util.*;
import mindustry.content.Blocks;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.*;

public class ScatterFilter extends GenerateFilter{
    protected float chance = 0.014f;
    protected Block flooronto = Blocks.air, floor = Blocks.air, block = Blocks.air;

    @Override
    public FilterOption[] options(){
        return Structs.arr(
        new SliderOption("chance", () -> chance, f -> chance = f, 0f, 1f),
        new BlockOption("flooronto", () -> flooronto, b -> flooronto = b, floorsOptional),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
        new BlockOption("block", () -> block, b -> block = b, wallsOresOptional)
        );
    }

    @Override
    public void apply(){

        if(block != Blocks.air && (in.floor == flooronto || flooronto == Blocks.air) && in.block == Blocks.air && chance() <= chance){
            if(!block.isOverlay()){
                in.block = block;
            }else{
                in.ore = block;
            }
        }

        if(floor != Blocks.air && (in.floor == flooronto || flooronto == Blocks.air) && chance() <= chance){
            in.floor = floor;
        }
    }
}
