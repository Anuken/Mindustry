package mindustry.maps.filters;

import arc.util.*;
import mindustry.content.Blocks;
import mindustry.maps.filters.FilterOption.SliderOption;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.BlockOption;
import static mindustry.maps.filters.FilterOption.oresOnly;

public class OreFilter extends GenerateFilter{
    public float scl = 23, threshold = 0.81f, octaves = 2f, falloff = 0.3f;
    public Block ore = Blocks.oreCopper;

    @Override
    public FilterOption[] options(){
        return Structs.arr(
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
