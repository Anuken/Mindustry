package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;

import static arc.util.Log.info;
import static mindustry.Vars.*;

public class Stalemate implements ApplicationListener{

    private int frozen = 0;

    @Override
    public void update(){
        if(Core.graphics.getFramesPerSecond() > 1){
            frozen = 0;
            return;
        }

        if(frozen++ > 10){
            frozen = 0;
            info("&lmstalemate!");
            Events.fire(new GameOverEvent(Team.crux));
        }
    }
}
