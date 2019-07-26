package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.filters.FilterOption.BlockOption;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.maps.filters.FilterOption.floorsOnly;
import static io.anuke.mindustry.maps.filters.FilterOption.wallsOnly;

public class NoiseFilter extends GenerateFilter{
    float scl = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    Block floor = Blocks.stone, block = Blocks.rocks;

    {
        options(
        new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
        new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
        new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
        new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
        new BlockOption("wall", () -> block, b -> block = b, wallsOnly)
        );
    }

    @Override
    public void apply(){
        float noise = noise(in.x, in.y, scl, 1f, octaves, falloff);

        if(noise > threshold){
            in.floor = floor;
            if(wallsOnly.test(in.block)) in.block = block;
        }
    }
}
