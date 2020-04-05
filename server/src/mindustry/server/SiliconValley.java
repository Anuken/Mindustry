package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;

public class SiliconValley implements ApplicationListener{

    private static final Block gate = Blocks.siliconSmelter;
    private Interval timer = new Interval();

    private boolean restartWhenEmpty = false;

    @Override
    public void update(){
        if(!Vars.state.rules.tags.containsKey("silicon")) return;
        if(!timer.get(60)) return;

        Vars.playerGroup.all().each(p -> {
            if(p.spiderling.unlockedBlocks.contains(gate)){
                Call.onConnect(p.con, "mindustry.nydus.app", 1337);
                Events.fire(new GameOverEvent(Team.crux));
            }
        });

        if(!Vars.playerGroup.isEmpty()) restartWhenEmpty = true;
        if(Vars.playerGroup.isEmpty() && restartWhenEmpty) System.exit(2);
    }

    @Override
    public void init(){
        Events.on(WaveEvent.class, event -> {
            if(!Vars.state.rules.tags.containsKey("silicon")) return;
            Events.fire(new GameOverEvent(Team.crux));
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!Vars.state.rules.tags.containsKey("silicon")) return;
            Vars.state.wavetime -= 60f;
            if(Vars.state.wavetime < 0f) Vars.state.wavetime = 0f;
        });
    }
}
