package io.anuke.mindustry.entities.units;

import io.anuke.arc.Core;

public enum UnitCommand{
    attack, retreat, patrol;

    private final String localized;

    UnitCommand(){
        localized = Core.bundle.get("command." + name());
    }

    public String localized(){
        return localized;
    }
}
