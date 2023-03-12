package mindustry.maps.filters;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class OreFilter extends GenerateFilter{
    public float scl = 23, threshold = 0.81f, octaves = 2f, falloff = 0.3f, tilt = 0f;
    public Block ore = Blocks.oreCopper, target = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 500f),
            new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
            new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
            new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
            new SliderOption("tilt", () -> tilt, f -> tilt = f, -4f, 4f),
            new BlockOption("ore", () -> ore, b -> ore = b, oresOnly),
            new BlockOption("target", () -> target, b -> target = b, oresFloorsOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockOreCopper;
    }

    @Override
    public void apply(GenerateInput in){
        float noise = noise(in.x, in.y + in.x * tilt, scl, 1f, octaves, falloff);

        if(noise > threshold && in.overlay != Blocks.spawn && (target == Blocks.air || in.floor == target || in.overlay == target) && in.floor.asFloor().hasSurface()){
            in.overlay = ore;
        }
    }
}
