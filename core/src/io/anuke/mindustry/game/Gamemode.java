package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.function.Supplier;

/** Defines preset rule sets.. */
public enum Gamemode{
    survival(() -> new Rules(){{
        waveTimer = true;
        waves = true;
        unitDrops = true;
        spawns = DefaultWaves.get();
    }}),
    sandbox(() -> new Rules(){{
        infiniteResources = true;
        waves = true;
        waveTimer = false;
        respawnTime = 0f;
    }}),
    attack(() -> new Rules(){{
        enemyCheat = true;
        unitDrops = true;
        waves = false;
        attackMode = true;
    }}),
    pvp(() -> new Rules(){{
        pvp = true;
        enemyCoreBuildRadius = 600f;
        respawnTime = 60 * 10;
        buildCostMultiplier = 0.5f;
        buildSpeedMultiplier = 2f;
        playerDamageMultiplier = 0.45f;
        playerHealthMultiplier = 0.8f;
        unitBuildSpeedMultiplier = 3f;
        unitHealthMultiplier = 2f;
        attackMode = true;
    }});

    private final Supplier<Rules> rules;

    Gamemode(Supplier<Rules> rules){
        this.rules = rules;
    }

    public Rules get(){
        return rules.get();
    }

    public String description(){
        return Core.bundle.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Core.bundle.get("mode." + name() + ".name");
    }
}
