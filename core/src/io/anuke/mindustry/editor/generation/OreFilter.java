package io.anuke.mindustry.editor.generation;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.generation.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.editor.generation.FilterOption.BlockOption;
import static io.anuke.mindustry.editor.generation.FilterOption.oresOnly;

public class OreFilter extends GenerateFilter{
    public float scl = 50, threshold = 0.72f, octaves = 3f, falloff = 0.4f;
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

        if(noise > threshold){
            in.ore = ore;
        }
    }
}
