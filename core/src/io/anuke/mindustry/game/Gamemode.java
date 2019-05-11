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
    }}),
    editor(true, () -> new Rules(){{
        infiniteResources = true;
        editor = true;
        waves = false;
        enemyCoreBuildRadius = 0f;
        waveTimer = false;
        respawnTime = 0f;
    }}),;

    private final Supplier<Rules> rules;
    public final boolean hidden;

    Gamemode(Supplier<Rules> rules){
        this(false, rules);
    }

    Gamemode(boolean hidden, Supplier<Rules> rules){
        this.rules = rules;
        this.hidden = hidden;
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
