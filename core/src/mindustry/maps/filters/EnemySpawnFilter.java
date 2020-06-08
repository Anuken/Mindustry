package mindustry.maps.filters;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

/** Selects X spawns from the spawn pool.*/
public class EnemySpawnFilter extends GenerateFilter{
    int amount = 1;

    @Override
    public FilterOption[] options(){
        return Structs.arr(
        new SliderOption("amount", () -> amount, f -> amount = (int)f, 1, 10).display()
        );
    }

    @Override
    public void apply(Tiles tiles, GenerateInput in){
        IntSeq spawns = new IntSeq();
        for(Tile tile : tiles){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile.pos());
            }
        }

        spawns.shuffle();

        int used = Math.min(spawns.size, amount);
        for(int i = used; i < spawns.size; i++){
            Tile tile = tiles.getp(spawns.get(i));
            tile.clearOverlay();
        }
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
