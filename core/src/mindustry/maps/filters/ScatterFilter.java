package mindustry.maps.filters;

import mindustry.content.Blocks;
import mindustry.maps.filters.FilterOption.BlockOption;
import mindustry.maps.filters.FilterOption.SliderOption;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.*;

public class ScatterFilter extends GenerateFilter{
    protected float chance = 0.014f;
    protected Block flooronto = Blocks.air, floor = Blocks.air, block = Blocks.air;

    {
        options(
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
