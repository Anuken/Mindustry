package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.function.Consumer;

/** Defines preset rule sets.. */
public enum Gamemode{
    survival(rules -> {
        rules.waveTimer = true;
        rules.waves = true;
        rules.unitDrops = true;
    }),
    sandbox(rules -> {
        rules.infiniteResources = true;
        rules.waves = true;
        rules.waveTimer = false;
        rules.respawnTime = 0f;
    }),
    attack(rules -> {
        rules.enemyCheat = true;
        rules.unitDrops = true;
        rules.waves = false;
        rules.attackMode = true;
    }),
    pvp(rules -> {
        rules.pvp = true;
        rules.enemyCoreBuildRadius = 600f;
        rules.respawnTime = 60 * 10;
        rules.buildCostMultiplier = 0.5f;
        rules.buildSpeedMultiplier = 2f;
        rules.playerDamageMultiplier = 0.33f;
        rules.playerHealthMultiplier = 0.5f;
        rules.unitBuildSpeedMultiplier = 3f;
        rules.unitHealthMultiplier = 3f;
        rules.attackMode = true;
    }),
    editor(true, rules -> {
        rules.infiniteResources = true;
        rules.editor = true;
        rules.waves = false;
        rules.enemyCoreBuildRadius = 0f;
        rules.waveTimer = false;
        rules.respawnTime = 0f;
    });

    private final Consumer<Rules> rules;
    public final boolean hidden;

    Gamemode(Consumer<Rules> rules){
        this(false, rules);
    }

    Gamemode(boolean hidden, Consumer<Rules> rules){
        this.rules = rules;
        this.hidden = hidden;
    }

    /** Applies this preset to this ruleset. */
    public Rules apply(Rules in){
        rules.accept(in);
        return in;
    }

    public String description(){
        return Core.bundle.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Core.bundle.get("mode." + name() + ".name");
    }
}
