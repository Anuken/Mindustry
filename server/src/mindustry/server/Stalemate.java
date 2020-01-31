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
        if(Core.graphics.getFramesPerSecond() > 10){
            frozen = 0;
            return;
        }

        if(frozen++ > 60){
            frozen = -10;
            info("&lmstalemate!");
            Events.fire(new GameOverEvent(Team.crux));
        }
    }

    @Override
    public void init(){
        Events.on(PlayEvent.class, event -> {
            frozen = 0;
        });
    }
}
