package mindustry.maps.filters;

import arc.struct.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

/** Selects X spawns from the spawn pool.*/
public class EnemySpawnFilter extends GenerateFilter{
    public int amount = 1;

    @Override
    public FilterOption[] options(){
        return new SliderOption[]{
            new SliderOption("amount", () -> amount, f -> amount = (int)f, 1, 10).display()
        };
    }

    @Override
    public char icon(){
        return Iconc.blockSpawn;
    }

    @Override
    public void apply(World world, GenerateInput in){
        IntSeq spawns = new IntSeq();
        for(Tile tile : world){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile.pos());
            }
        }

        spawns.shuffle();

        int used = Math.min(spawns.size, amount);
        for(int i = used; i < spawns.size; i++){
            Tile tile = world.getp(spawns.get(i));
            tile.clearOverlay();
        }
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
