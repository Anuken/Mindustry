package io.anuke.mindustry.game;

import com.badlogic.gdx.ai.pfa.Heuristic;
import io.anuke.mindustry.ai.Heuristics.DestrutiveHeuristic;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.distribution.Conveyor;
import io.anuke.mindustry.world.blocks.types.distribution.Router;
import io.anuke.mindustry.world.blocks.types.production.Drill;
import io.anuke.mindustry.world.blocks.types.production.Generator;
import io.anuke.mindustry.world.blocks.types.production.Smelter;
import io.anuke.ucore.util.Bundles;

public enum Difficulty {
    easy(4f, 2f, new DestrutiveHeuristic(b -> b instanceof Generator)),
    normal(2f, 1f, new DestrutiveHeuristic(b -> b instanceof Smelter || b instanceof Generator)),
    hard(1.5f, 0.5f, new DestrutiveHeuristic(b -> b instanceof Turret || b instanceof Generator || b instanceof Drill || b instanceof Smelter)),
    insane(0.5f, 0.25f, new DestrutiveHeuristic(b -> b instanceof Generator || b instanceof Drill || b instanceof Smelter || b instanceof Router)),
    purge(0.25f, 0.01f, new DestrutiveHeuristic(b -> b instanceof Generator || b instanceof Drill || b instanceof Router
            || b instanceof Smelter || b instanceof Conveyor || b instanceof LiquidBlock || b instanceof PowerBlock));

    /**The scaling of how many waves it takes for one more enemy of a type to appear.
     * For example: with enemeyScaling = 2 and the default scaling being 2, it would take 4 waves for
     * an enemy spawn to go from 1->2 enemies.*/
    public final float enemyScaling;
    /**Multiplier of the time between waves.*/
    public final float timeScaling;
    /**Pathfdining heuristic for calculating tile costs.*/
    public final Heuristic<Tile> heuristic;

    Difficulty(float enemyScaling, float timeScaling, Heuristic<Tile> heuristic){
        this.enemyScaling = enemyScaling;
        this.timeScaling = timeScaling;
        this.heuristic = heuristic;
    }

    @Override
    public String toString() {
        return Bundles.get("setting.difficulty." + name());
    }
}
