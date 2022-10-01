package mindustry.maps.filters;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;
import static mindustry.maps.filters.FilterOption.*;

/** Selects X spawns from the spawn pool.*/
public class SpawnPathFilter extends GenerateFilter{
    public int radius = 3;
    public Block block = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new SliderOption("radius", () -> radius, f -> radius = (int)f, 1, 20).display(),
            new BlockOption("wall", () -> block, b -> block = b, wallsOnly)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockCommandCenter;
    }

    @Override
    public void apply(Tiles tiles, GenerateInput in){
        var cores = new Seq<Tile>();
        var spawns = new Seq<Tile>();

        for(Tile tile : tiles){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile);
            }
            if(tile.block() instanceof CoreBlock && tile.team() != Vars.state.rules.waveTeam){
                cores.add(tile);
            }
        }

        for(var core : cores){
            for(var spawn : spawns){
                var path = Astar.pathfind(core.x, core.y, spawn.x, spawn.y, t -> t.solid() ? 100 : 1, Astar.manhattan, tile -> !tile.floor().isDeep());
                for(var tile : path){
                    for(int x = -radius; x <= radius; x++){
                        for(int y = -radius; y <= radius; y++){
                            int wx = tile.x + x, wy = tile.y + y;
                            if(Structs.inBounds(wx, wy, world.width(), world.height()) && Mathf.within(x, y, radius)){
                                Tile other = tiles.getn(wx, wy);
                                if(!other.synthetic()){
                                    other.setBlock(block);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
