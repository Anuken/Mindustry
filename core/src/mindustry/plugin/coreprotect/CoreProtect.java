package mindustry.plugin.coreprotect;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.plugin.*;
import mindustry.world.*;

import static mindustry.Vars.tilesize;

public class CoreProtect extends Plugin implements ApplicationListener{

    private ObjectMap<Player, IntSet> sparks = new ObjectMap<>();

    private int timers = 0;
    private int spark = timers++;
    private Interval timer = new Interval(timers);

    public void onTileTapped(Player player, Tile tile){

        if(!sparks.containsKey(player)) sparks.put(player, new IntSet());

        sparks.get(player).add(tile.pos());
    }

    @Override
    public void init(){
        Events.on(Trigger.update, () -> {
            if(!Vars.state.is(State.playing)) return;

            if(timer.get(spark, 10)){
                sparks.each((player, ints) -> ints.each(pos -> spark(player, pos)));
            }
        });
    }

    private void spark(Player player, int pos){
        Call.createLighting(player.con, 0, player.getTeam(), Color.white, 0, Pos.x(pos) * tilesize, Pos.y(pos) * tilesize, 0, 2);
    }
}
