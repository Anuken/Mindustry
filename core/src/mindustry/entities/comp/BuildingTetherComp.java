package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

/** A unit that depends on a building's existence; if that building is removed, it despawns. */
@Component
abstract class BuildingTetherComp implements Unitc{
    @Import UnitType type;
    @Import Team team;

    public Building building;

    @Override
    public void update(){
        if(building == null || !building.isValid() || building.team != team){
            Call.unitDespawn(self());
        }
    }
}
