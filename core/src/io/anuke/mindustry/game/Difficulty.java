package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum Difficulty{
    training(3f, 3f),
    easy(1.4f, 1.5f),
    normal(1f, 1f),
    hard(0.5f, 0.75f),
    insane(0.25f, 0.5f);

    /**Multiplier of the time between waves.*/
    public final float timeScaling;
    /**Multiplier of spawner grace period.*/
    public final float spawnerScaling;

    private String value;

    Difficulty(float timeScaling, float spawnerScaling){
        this.timeScaling = timeScaling;
        this.spawnerScaling = spawnerScaling;
    }

    @Override
    public String toString(){
        if(value == null){
            value = Bundles.get("setting.difficulty." + name());
        }
        return value;
    }
}
