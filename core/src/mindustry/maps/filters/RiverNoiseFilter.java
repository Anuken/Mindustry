package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class RiverNoiseFilter extends GenerateFilter{
    public float scl = 40, threshold = 0f, threshold2 = 0.1f, octaves = 1, falloff = 0.5f;
    public Block floor = Blocks.water, floor2 = Blocks.deepwater, block = Blocks.sandWall, target = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, -1f, 1f),
            new SliderOption("threshold2", () -> threshold2, f -> threshold2 = f, -1f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
            new BlockOption("target", () -> target, b -> target = b, anyOptional),
            new BlockOption("block", () -> block, b -> block = b, wallsOptional),
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
            new BlockOption("floor2", () -> floor2, b -> floor2 = b, floorsOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockShallowWater;
    }

    @Override
    public void apply(GenerateInput in){
        float noise = rnoise(in.x, in.y, (int)octaves, scl, falloff, 1f);

        if(noise >= threshold && (target == Blocks.air || in.floor == target || in.block == target)){
            if(floor != Blocks.air) in.floor = floor;

            if(in.block.solid && block != Blocks.air && in.block != Blocks.air){
                in.block = block;
            }

            if(noise >= threshold2 && floor2 != Blocks.air){
                in.floor = floor2;
            }
        }
    }
}
