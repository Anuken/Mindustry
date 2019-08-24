package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.maps.filters.FilterOption.BlockOption;
import static io.anuke.mindustry.maps.filters.FilterOption.oresOnly;

public class OreFilter extends GenerateFilter{
    public float scl = 23, threshold = 0.81f, octaves = 2f, falloff = 0.3f;
    public Block ore = Blocks.oreCopper;

    {
        options(
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
            new BlockOption("ore", () -> ore, b -> ore = b, oresOnly)
        );
    }

    @Override
    public void apply(){
        float noise = noise(in.x, in.y, scl, 1f, octaves, falloff);

        if(noise > threshold && in.ore != Blocks.spawn){
            in.ore = ore;
        }
    }
}
