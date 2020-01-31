package mindustry.server;

import arc.*;
import mindustry.game.*;
import mindustry.game.EventType.*;

public class Stalemate implements ApplicationListener{

    private int frozen = 0;

    @Override
    public void update(){
        if(Core.graphics.getFramesPerSecond() > 1) return;
        if(frozen++ < 20) return;
        frozen = 0;
        Events.fire(new GameOverEvent(Team.crux));
    }
}
