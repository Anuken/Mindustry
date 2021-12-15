package mindustry.ai.types;

import arc.math.geom.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.units.UnitAssembler.*;

public class AssemblerAI extends AIController{
    public Vec2 targetPos = new Vec2();

    @Override
    public void updateMovement(){
        //TODO
        if(!targetPos.isZero()){
            moveTo(targetPos, 8f, 11f);
        }

        if(unit instanceof BuildingTetherc tether && tether.building() instanceof UnitAssemblerBuild assembler){
            unit.lookAt(assembler.getUnitSpawn());
        }
    }
}
