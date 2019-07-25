package io.anuke.mindustry.maps;

import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;

import static io.anuke.mindustry.Vars.*;

/** Defines a specific type of attribute for a map, usually whether or not it supports a certain type of mode.*/
public enum MapAttribute{
    /** Whether a map has a player spawnpoint in it.*/
    spawnpoint(teams -> teams.contains(defaultTeam.ordinal())),
    /** Whether a map has a wave team core to attack.*/
    attack(teams -> teams.contains(waveTeam.ordinal())),
    /** Whether this map supports PvP.*/
    pvp(teams -> teams.size > 1);

    private final Predicate<IntSet> validator;

    public static final MapAttribute[] all = values();

    MapAttribute(Predicate<IntSet> set){
        this.validator = set;
    }

    //todo also take into account enemy spawnpoints
    public boolean validate(IntSet teams){
        return validator.test(teams);
    }
}
