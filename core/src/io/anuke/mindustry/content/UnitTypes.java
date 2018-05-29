package io.anuke.mindustry.content;

import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.entities.units.types.Drone;
import io.anuke.mindustry.entities.units.types.Scout;
import io.anuke.mindustry.entities.units.types.Vtol;
import io.anuke.mindustry.type.ContentList;

public class UnitTypes implements ContentList {
    public static UnitType drone, scout, vtol;

    @Override
    public void load() {
        drone = new Drone();
        scout = new Scout();
        vtol = new Vtol();
    }
}
