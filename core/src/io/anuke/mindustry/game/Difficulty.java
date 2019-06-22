package io.anuke.mindustry.game;

import io.anuke.arc.Core;

/** Presets for time between waves. Currently unused.*/
public enum Difficulty{
    easy(1.4f),
    normal(1f),
    hard(0.5f),
    insane(0.25f);

    /** Multiplier of the time between waves. */
    public final float waveTime;

    private String value;

    Difficulty(float waveTime){
        this.waveTime = waveTime;
    }

    @Override
    public String toString(){
        if(value == null){
            value = Core.bundle.get("setting.difficulty." + name());
        }
        return value;
    }
}
