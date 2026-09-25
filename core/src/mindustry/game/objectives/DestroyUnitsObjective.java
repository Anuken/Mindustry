package mindustry.game.objectives;

import arc.*;

import static mindustry.Vars.*;

/** Produce a certain amount of units. */
public class DestroyUnitsObjective extends MapObjective{
    public int count = 1;

    public DestroyUnitsObjective(int count){
        this.count = count;
    }

    public DestroyUnitsObjective(){
    }

    @Override
    public boolean update(){
        return state.stats.enemyUnitsDestroyed >= count;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.destroyunits", count - state.stats.enemyUnitsDestroyed);
    }

    @Override
    public String toString(){
        return "destroyUnits: " + count;
    }
}
