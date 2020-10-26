package mindustry.maps.filters;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MedianFilter extends GenerateFilter{
    float radius = 2;
    float percentile = 0.5f;
    IntSeq blocks = new IntSeq(), floors = new IntSeq();

    @Override
    public FilterOption[] options(){
        return Structs.arr(
        new SliderOption("radius", () -> radius, f -> radius = f, 1f, 12f),
        new SliderOption("percentile", () -> percentile, f -> percentile = f, 0f, 1f)
        );
    }

    @Override
    public boolean isBuffered(){
        return true;
    }

    @Override
    public void apply(){
        int rad = (int)radius;
        blocks.clear();
        floors.clear();
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;

                Tile tile = in.tile(in.x + x, in.y + y);
                blocks.add(tile.block().id);
                floors.add(tile.floor().id);
            }
        }

        floors.sort();
        blocks.sort();

        int index = Math.min((int)(floors.size * percentile), floors.size - 1);
        int floor = floors.get(index), block = blocks.get(index);

        in.floor = content.block(floor);
        if(!content.block(block).synthetic() && !in.block.synthetic()) in.block = content.block(block);
    }
}
