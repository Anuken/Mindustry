package mindustry.game;

import arc.*;
import arc.func.*;
import mindustry.maps.*;

import static mindustry.Vars.*;

/** Defines preset rule sets. */
public enum Gamemode{
    survival(rules -> {
        rules.waveTimer = true;
        rules.waves = true;
    }, map -> map.spawns > 0),
    sandbox(rules -> {
        rules.infiniteResources = true;
        rules.waves = true;
        rules.waveTimer = false;
    }),
    attack(rules -> {
        rules.attackMode = true;
        rules.waves = true;
        rules.waveTimer = true;

        rules.waveSpacing /= 2f;
        rules.teams.get(rules.waveTeam).ai = true;
        rules.teams.get(rules.waveTeam).infiniteResources = true;
    }, map -> map.teams.contains(state.rules.waveTeam.id)),
    pvp(rules -> {
        rules.pvp = true;
        rules.enemyCoreBuildRadius = 600f;
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1f;
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
