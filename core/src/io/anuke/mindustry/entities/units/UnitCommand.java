package io.anuke.mindustry.entities.units;

import io.anuke.arc.*;

public enum UnitCommand{
    attack, retreat, rally;

    private final String localized;
    public static final UnitCommand[] all = values();

    UnitCommand(){
        localized = Core.bundle.get("command." + name());
    }

    public String localized(){
        return localized;
    }
}