package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.editor.MapGenerateDialog.GenTile;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.content;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    {
        buffered = true;
        options(
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 400f),
            new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 100f)
        );
    }

    @Override
    public void apply(){
        GenTile tile = in.tile(in.x + noise(in.x, in.y, scl, mag) - mag / 2f, in.y + noise(in.x, in.y + o, scl, mag) - mag / 2f);

        in.floor = content.block(tile.floor);
        if(!content.block(tile.block).synthetic() && !in.block.synthetic()) in.block = content.block(tile.block);
        if(!((Floor)in.floor).isLiquid) in.ore = content.block(tile.ore);
    }
}
