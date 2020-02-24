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
import mindustry.graphics.*;
import mindustry.plugin.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class CoreProtect extends Plugin implements ApplicationListener{

    private ObjectMap<Player, Stick> sticks = new ObjectMap<>();

    private int timers = 0;
    private int spark = timers++;
    private Interval timer = new Interval(timers);

    private Color red = Pal.remove, blue = Pal.copy;

    public void onTileTapped(Player player, Tile tile){
        Stick stick = sticks.getOr(player, Stick::new);

        if((stick.xyi++ % 2) == 0){
            stick.xy1 = tile.pos();
        }else{
            stick.xy2 = tile.pos();
        }
    }

    @Override
    public void init(){
        Events.on(Trigger.update, () -> {
            if(!Vars.state.is(State.playing)) return;

            if(timer.get(spark, 10)){
                sticks.each((player, stick) -> {
                    if(stick.xy1 != Pos.invalid) spark(player, stick.xy1, red);
                    if(stick.xy2 != Pos.invalid) spark(player, stick.xy2, blue);
                });
            }
        });
    }

    private void spark(Player player, int pos, Color color){
        Call.createLighting(player.con, 0, player.getTeam(), color, 0, Pos.x(pos) * tilesize, Pos.y(pos) * tilesize, 0, 2);
    }

    class Stick{
        int xyi = Pos.invalid;
        int xy1 = Pos.invalid;
        int xy2 = Pos.invalid;
    }
}
