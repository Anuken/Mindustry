package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum Difficulty {
    easy(4f, 2f, 1f),
    normal(2f, 1f, 1f),
    hard(1.5f, 0.5f, 0.75f),
    insane(0.5f, 0.25f, 0.5f);
    //purge removed due to new wave system
    /*purge(0.25f, 0.01f, 0.25f)*/;

    /**The scaling of how many waves it takes for one more enemy of a type to appear.
     * For example: with enemeyScaling = 2 and the default scaling being 2, it would take 4 waves for
     * an enemy spawn to go from 1->2 enemies.*/
    public final float enemyScaling;
    /**Multiplier of the time between waves.*/
    public final float timeScaling;
    /**Scaling of max time between waves. Default time is 4 minutes.*/
    public final float maxTimeScaling;

    private String value;

    Difficulty(float enemyScaling, float timeScaling, float maxTimeScaling){
        this.enemyScaling = enemyScaling;
        this.timeScaling = timeScaling;
        this.maxTimeScaling = maxTimeScaling;
    }

    @Override
    public String toString() {
        if(value == null){
            value = Bundles.get("setting.difficulty." + name());
        }
        return value;
    }
}
