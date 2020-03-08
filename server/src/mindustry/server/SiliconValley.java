package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.net.Packets.*;
import mindustry.world.*;

public class SiliconValley implements ApplicationListener{

    private static final Block gate = Blocks.siliconSmelter;
    private Interval timer = new Interval();

    private boolean restartWhenEmpty = true;

    @Override
    public void update(){
        if(!Vars.state.rules.tags.containsKey("silicon")) return;
        if(!timer.get(60)) return;

        Vars.playerGroup.all().each(p -> {
            if(p.spiderling.unlockedBlocks.contains(gate)){
                p.con.yeet(KickReason.serverRestarting, "unlocked", gate.name);
            }
        });

        if(!Vars.playerGroup.isEmpty()) restartWhenEmpty = true;
        if(Vars.playerGroup.isEmpty() && restartWhenEmpty) System.exit(2);
    }
}
