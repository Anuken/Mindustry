package mindustry.maps.filters;

import arc.math.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class TerrainFilter extends GenerateFilter{
    float scl = 40, threshold = 0.9f, octaves = 3f, falloff = 0.5f, magnitude = 1f, circleScl = 2.1f;
    Block floor = Blocks.air, block = Blocks.stoneWall;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
            new SliderOption("mag", () -> magnitude, f -> magnitude = f, 0f, 2f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
            new SliderOption("circle-scale", () -> circleScl, f -> circleScl = f, 0f, 3f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
            new BlockOption("wall", () -> block, b -> block = b, wallsOnly)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockStoneWall;
    }

    @Override
    public void apply(GenerateInput in){
        float noise = noise(in, scl, magnitude, octaves, falloff) + Mathf.dst((float)in.x / in.width, (float)in.y / in.height, 0.5f, 0.5f) * circleScl;

        if(floor != Blocks.air){
            in.floor = floor;
        }

        if(noise >= threshold){
            in.block = block;
        }
    }
}
