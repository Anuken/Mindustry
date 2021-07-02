package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class NoiseFilter extends GenerateFilter{
    float scl = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    Block floor = Blocks.stone, block = Blocks.stoneWall, target = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
            new BlockOption("target", () -> target, b -> target = b, anyOptional),
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
            new BlockOption("wall", () -> block, b -> block = b, wallsOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockPebbles;
    }

    @Override
    public void apply(GenerateInput in){
        float noise = noise(in, scl, 1f, octaves, falloff);

        if(noise > threshold && (target == Blocks.air || in.floor == target || in.block == target)){
            if(floor != Blocks.air) in.floor = floor;
            if(block != Blocks.air && in.block != Blocks.air) in.block = block;
        }
    }
}
