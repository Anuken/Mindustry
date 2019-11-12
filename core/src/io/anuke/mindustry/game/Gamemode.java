package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.func.*;
import io.anuke.mindustry.maps.*;

import static io.anuke.mindustry.Vars.waveTeam;

/** Defines preset rule sets. */
public enum Gamemode{
    survival(rules -> {
        rules.waveTimer = true;
        rules.waves = true;
        rules.unitDrops = true;
    }, map -> map.spawns > 0),
    sandbox(rules -> {
        rules.infiniteResources = true;
        rules.waves = true;
        rules.waveTimer = false;
        rules.respawnTime = 0f;
    }),
    attack(rules -> {
        rules.unitDrops = true;
        rules.attackMode = true;
    }, map -> map.teams.contains(waveTeam.ordinal())),
    pvp(rules -> {
        rules.pvp = true;
        rules.enemyCoreBuildRadius = 600f;
        rules.respawnTime = 60 * 10;
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1f;
        rules.playerDamageMultiplier = 0.33f;
        rules.playerHealthMultiplier = 0.5f;
        rules.unitBuildSpeedMultiplier = 2f;
        rules.unitHealthMultiplier = 3f;
        rules.attackMode = true;
    }, map -> map.teams.size > 1),
    editor(true, rules -> {
        rules.infiniteResources = true;
        rules.editor = true;
        rules.waves = false;
        rules.enemyCoreBuildRadius = 0f;
        rules.waveTimer = false;
        rules.respawnTime = 0f;
    });

    private final Cons<Rules> rules;
    private final Boolf<Map> validator;

    public final boolean hidden;
    public final static Gamemode[] all = values();

    Gamemode(Cons<Rules> rules){
        this(false, rules);
    }

    Gamemode(boolean hidden, Cons<Rules> rules){
         this(hidden, rules, m -> true);
    }

    Gamemode(Cons<Rules> rules, Boolf<Map> validator){
        this(false, rules, validator);
    }

    Gamemode(boolean hidden, Cons<Rules> rules, Boolf<Map> validator){
        this.rules = rules;
        this.hidden = hidden;
        this.validator = validator;
    }

    public static Gamemode bestFit(Rules rules){
        if(rules.pvp){
            return pvp;
        }else if(rules.editor){
            return editor;
        }else if(rules.attackMode){
            return attack;
        }else if(rules.infiniteResources){
            return sandbox;
        }else{
            return survival;
        }
    }

    /** Applies this preset to this ruleset. */
    public Rules apply(Rules in){
        rules.get(in);
        return in;
    }

    /** @return whether this mode can be played on the specified map. */
    public boolean valid(Map map){
        return validator.get(map);
    }

    public String description(){
        return Core.bundle.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Core.bundle.get("mode." + name() + ".name");
    }
}
