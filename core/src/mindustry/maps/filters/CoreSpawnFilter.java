package mindustry.maps.filters;

import arc.struct.*;
import arc.util.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.state;

/** Selects X spawns from the core spawn pool.*/
public class CoreSpawnFilter extends GenerateFilter{
    int amount = 1;

    @Override
    public FilterOption[] options(){
        return Structs.arr(
        //disabled until necessary
        // SliderOption("amount", () -> amount, f -> amount = (int)f, 1, 10).display()
        );
    }

    @Override
    public void apply(Tiles tiles, GenerateInput in){
        IntSeq spawns = new IntSeq();
        for(Tile tile : tiles){
            if(tile.team() == state.rules.defaultTeam && tile.block() instanceof CoreBlock && tile.isCenter()){
                spawns.add(tile.pos());
            }
        }

        spawns.shuffle();

        int used = Math.min(spawns.size, amount);
        for(int i = used; i < spawns.size; i++){
            tiles.getp(spawns.get(i)).remove();
        }
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
