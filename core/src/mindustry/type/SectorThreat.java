package mindustry.type;

import arc.*;

/** Note: Don't use this class; difficulty will be reworked in v9 to use specific discrete values instead of arbitrary numbers. */
public enum SectorThreat{
    low,
    medium,
    high,
    extreme,
    eradication,
    //special difficulty reserved for 27
    unreasonable;

    public static final SectorThreat[] all = values();

    public String localized(){
        return Core.bundle.get("threat." + name());
    }
}
