package mindustry.world.blocks;

import mindustry.game.*;
import mindustry.gen.*;

public interface LiquidUpdater extends Healthc{
    default boolean shouldLiquidUpdate(){
        return true;
    }

    void updateLiquids(float delta);
    Team team();
}
