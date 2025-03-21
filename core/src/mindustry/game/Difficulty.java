package mindustry.game;

import arc.*;

import java.util.List;

public class Difficulty{

    //TODO add more fields
    public float enemyHealthMultiplier, enemySpawnMultiplier, waveTimeMultiplier;

    public String name;

    /* In order for a modder to use this they have to use Difficulties.addDifficullties() */
    public Difficulty(float enemyHealthMultiplier, float enemySpawnMultiplier, float waveTimeMultiplier, String name){
        this.enemySpawnMultiplier = enemySpawnMultiplier;
        this.waveTimeMultiplier = waveTimeMultiplier;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.name = name;
    }

    public String localized(){
        return Core.bundle.get("difficulty." + this.name);
    }
}
