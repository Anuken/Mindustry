package mindustry.entities.units;

import arc.*;

public enum UnitCommand{
    attack, rally, idle;

    private final String localized;
    public static final UnitCommand[] all = values();

    UnitCommand(){
        localized = Core.bundle.get("command." + name());
    }

    public String localized(){
        return localized;
    }
}
