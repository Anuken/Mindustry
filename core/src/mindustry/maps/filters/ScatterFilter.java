package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class ScatterFilter extends GenerateFilter{
    public float chance = 0.013f;
    public Block flooronto = Blocks.air, floor = Blocks.air, block = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("chance", () -> chance, f -> chance = f, 0f, 1f),
            new BlockOption("flooronto", () -> flooronto, b -> flooronto = b, floorsOptional),
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
            new BlockOption("block", () -> block, b -> block = b, wallsOresOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockBoulder;
    }

    @Override
    public void apply(GenerateInput in){

        if(block != Blocks.air && (in.floor == flooronto || flooronto == Blocks.air) && in.block == Blocks.air && chance(in.x, in.y) <= chance){
            if(!block.isOverlay()){
                in.block = block;
            }else{
                in.overlay = block;
            }
        }

        if(floor != Blocks.air && (in.floor == flooronto || flooronto == Blocks.air) && chance(in.x, in.y) <= chance){
            in.floor = floor;
        }
    }
}
