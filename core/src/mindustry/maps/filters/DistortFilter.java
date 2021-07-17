package mindustry.maps.filters;

import mindustry.gen.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    @Override
    public FilterOption[] options(){
        return new SliderOption[]{
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 200f),
            new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 100f)
        };
    }

    @Override
    public boolean isBuffered(){
        return true;
    }

    @Override
    public char icon(){
        return Iconc.blockTendrils;
    }

    @Override
    public void apply(GenerateInput in){
        Tile tile = in.tile(in.x + noise(in, scl, mag) - mag / 2f, in.y + noise(in, scl, mag) - mag / 2f);

        in.floor = tile.floor();
        if(!tile.block().synthetic() && !in.block.synthetic()) in.block = tile.block();
        in.overlay = tile.overlay();
    }
}
