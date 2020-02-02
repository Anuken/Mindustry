package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class Limbo implements ApplicationListener{
    @Override
    public void init(){
        Events.on(PlayerJoin.class, event -> {
            if(world.getMap().name().equals("limbo")) Timer.schedule(() -> Call.onConnect(event.player.con, "mindustry.nydus.app", port), 10);
        });
    }

    public static void send(Player player){
        Call.onConnect(player.con, "mindustry.nydus.app", 6561);
    }
}
