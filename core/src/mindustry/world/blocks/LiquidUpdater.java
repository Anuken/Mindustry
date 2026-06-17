package mindustry.world.blocks;

import mindustry.game.*;
import mindustry.gen.*;

public interface LiquidUpdater extends Healthc{
    void updateLiquids(float delta);
    Team team();
}
