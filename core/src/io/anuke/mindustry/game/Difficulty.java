package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum Difficulty {
    easy(4f, 2f),
    normal(2f, 1f),
    hard(1.5f, 0.5f),
    insane(0.5f, 0.25f),
    purge(0.4f, 0.01f);

    /**The scaling of how many waves it takes for one more enemy of a type to appear.
     * For example: with enemeyScaling = 2 and the default scaling being 2, it would take 4 waves for
     * an enemy spawn to go from 1->2 enemies.*/
    public final float enemyScaling;
    /**Multiplier of the time between waves.*/
    public final float timeScaling;

    Difficulty(float enemyScaling, float timeScaling){
        this.enemyScaling = enemyScaling;
        this.timeScaling = timeScaling;
    }

    @Override
    public String toString() {
        return Bundles.get("setting.difficulty." + name());
    }
}
