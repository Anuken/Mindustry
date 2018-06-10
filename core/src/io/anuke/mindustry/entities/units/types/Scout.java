package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.entities.units.GroundUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Team;

public class Scout extends GroundUnit {
    public static int typeID = -1;

    public Scout(UnitType type, Team team) {
        super(type, team);
    }

    public Scout(){

    }

    @Override
    public int getTypeID() {
        return typeID;
    }
}
