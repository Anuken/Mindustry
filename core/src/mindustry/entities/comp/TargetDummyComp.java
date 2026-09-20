package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.TargetDummy.*;

@Component
abstract class TargetDummyComp implements Unitc, Healthc{
    public @Nullable Building building;

    @Override
    public void update(){
        if(building == null || (!building.isPayload() && !building.isValid())){
            Call.unitDespawn(self()); //Don't despawn even if the building is on another team
        }
    }

    @Replace
    @Override
    public void rawDamage(float amount){
        if(building instanceof TargetDummyBuild td){
            td.dummyHit(amount);
        }
    }

    @Override
    @Replace
    public boolean canLand(){
        //dummies should always respect the boost config even when the landing area is obstructed (usually by other dummies)
        return true;
    }
}