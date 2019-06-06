package io.anuke.mindustry.editor.generation;

import io.anuke.mindustry.editor.MapGenerateDialog.GenTile;
import io.anuke.mindustry.editor.generation.FilterOption.SliderOption;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.content;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    {
        options(
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 400f),
            new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 100f)
        );
    }

    @Override
    public void apply(){
        GenTile tile = in.tile(in.x / (in.scaling) + (noise(in.x, in.y, scl, mag) - mag / 2f) / in.scaling, in.y / (in.scaling) + (noise(in.x, in.y + o, scl, mag) - mag / 2f) / in.scaling);

        in.floor = content.block(tile.floor);
        if(!content.block(tile.block).synthetic() && !in.block.synthetic()) in.block = content.block(tile.block);
        if(!((Floor)in.floor).isLiquid) in.ore = content.block(tile.ore);
    }
}
