package mindustry.logic.instructions;

import arc.math.geom.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class SpawnWaveI implements LogicInstruction{
    public LogicVar natural;
    public LogicVar x, y;

    public SpawnWaveI(){
    }

    public SpawnWaveI(LogicVar natural, LogicVar x, LogicVar y){
        this.natural = natural;
        this.x = x;
        this.y = y;
    }

    @Override
    public void run(LogicExecutor exec){
        if(net.client()) return;

        if(natural.bool()){
            logic.skipWave();
            return;
        }

        float
        spawnX = World.unconv(x.numf()),
        spawnY = World.unconv(y.numf());
        int packed = Point2.pack(x.numi(), y.numi());

        for(SpawnGroup group : state.rules.spawns){
            if(group.type == null || (group.spawn != -1 && group.spawn != packed)) continue;

            int spawned = group.getSpawned(state.wave - 1);
            float spread = tilesize * 2;

            for(int i = 0; i < spawned; i++){
                Tmp.v1.rnd(spread);

                state.spawner.spawnUnit(group, spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);
            }
        }
    }
}
