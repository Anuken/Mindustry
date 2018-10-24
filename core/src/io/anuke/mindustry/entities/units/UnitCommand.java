package io.anuke.mindustry.entities.units;

import io.anuke.ucore.util.Bundles;

public enum UnitCommand{
    attack, retreat, patrol;

    private final String localized;

    UnitCommand(){
        localized = Bundles.get("command." + name());
    }

    public String localized(){
        return localized;
    }
}
