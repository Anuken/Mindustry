package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class RangedVenom implements ApplicationListener{

    private Interval timer = new Interval();

    private ObjectSet<Player> embargo = new ObjectSet<>();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(20)) return;

        playerGroup.all().each(p -> {
            if(p.isDead()) return;
            Tile oil = indexer.findClosestLiquid(p.x, p.y, Liquids.oil);
            if(oil != null){
                Log.info(oil.dst(p));
                coreProtect.spark(p, oil.pos(), Liquids.oil.color);

                if(oil.dst(p) < 500){
                    if(!embargo.contains(p)){
                        embargo.add(p);
                        state.rules.bannedBlocks.add(Blocks.oilExtractor);
                        Call.onSetRules(p.con, state.rules);
                        state.rules.bannedBlocks.remove(Blocks.oilExtractor);
                    }
                }else{
                    if(embargo.contains(p)){
                        embargo.remove(p);
                        Call.onSetRules(p.con, state.rules);
                    }
                }
            }
        });
    }
}
