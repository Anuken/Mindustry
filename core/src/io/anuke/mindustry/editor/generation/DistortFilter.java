package io.anuke.mindustry.editor.generation;

import io.anuke.mindustry.editor.generation.FilterOption.SliderOption;
import io.anuke.mindustry.world.Tile;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    {
        options(
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 1000f),
            new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 10f)
        );
    }

    @Override
    public void apply(){
        Tile tile = in.tile(in.x + noise(in.x, in.y, scl, mag)-mag/2f, in.y + noise(in.x, in.y+o, scl, mag)-mag/2f);

        in.floor = tile.floor();
        if(!tile.block().synthetic() && !in.block.synthetic()) in.block = tile.block();
        in.ore = tile.ore();
    }
}
