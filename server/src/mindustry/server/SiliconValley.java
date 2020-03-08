package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.net.Packets.*;
import mindustry.world.*;

public class SiliconValley implements ApplicationListener{

    public static final Block gate = Blocks.siliconSmelter;
    private Interval timer = new Interval();

    @Override
    public void update(){
        if(!Vars.state.rules.tags.containsKey("silicon")) return;
        if(!timer.get(60)) return;

        Vars.playerGroup.all().each(p -> {
            if(p.spiderling.unlockedBlocks.contains(gate)){
                p.con.yeet(KickReason.serverRestarting, "unlocked", gate.name);
                Time.runTask(5f, () -> System.exit(2));
            }
        });
    }
}
