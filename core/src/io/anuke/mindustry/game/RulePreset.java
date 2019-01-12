package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.function.Supplier;

/**Defines preset rule sets..*/
public enum RulePreset{
    survival(() -> new Rules(){{
        waveTimer = true;
        waves = true;
        unitDrops = true;
    }}),
    sandbox(() -> new Rules(){{
        infiniteResources = true;
        waves = true;
        waveTimer = false;
    }}),
    attack(() -> new Rules(){{
        enemyCheat = true;
        unitDrops = true;
    }}),
    pvp(() -> new Rules(){{
        pvp = true;
        enemyCoreBuildRadius = 600f;
        respawnTime = 60 * 10;
    }});

    private final Supplier<Rules> rules;

    RulePreset(Supplier<Rules> rules){
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
