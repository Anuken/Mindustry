package mindustry.game;

import arc.*;

public enum Difficulty{
    //TODO these need tweaks
    casual(0.75f, 0.5f, 2f),
    easy(1f, 0.75f, 1.5f),
    normal(1f, 1f, 1f),
    hard(1.25f, 1.5f, 0.8f),
    eradication(1.5f, 2f, 0.6f);

    public static final Difficulty[] all = values();

    //TODO add more fields
    public float enemyHealthMultiplier, enemySpawnMultiplier, waveTimeMultiplier;

    Difficulty(float enemyHealthMultiplier, float enemySpawnMultiplier, float waveTimeMultiplier){
        this.enemySpawnMultiplier = enemySpawnMultiplier;
        this.waveTimeMultiplier = waveTimeMultiplier;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
    }

    public String localized(){
        return Core.bundle.get("difficulty." + name());
    }
}
