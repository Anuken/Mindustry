package mindustry.server;

import arc.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

public class PixelThanos implements ApplicationListener{

    @Override
    public void update(){
        if(!Vars.state.is(State.playing)) return;

        if(Sorter.thanos.isEmpty()) return;
        Tile random = Sorter.thanos.random();
        Sorter.thanos.remove(random);

        if(random == null) return;
        if(!(random.block() instanceof Sorter)) return;

        Core.app.post(() -> Call.onDeconstructFinish(random, random.block, -1));
    }
}
