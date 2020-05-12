package mindustry.entities.def;

import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.ai.formations.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** A unit that can command other units. */
@Component
abstract class CommanderComp implements Unitc{
    private static final Array<FormationMember> members = new Array<>();

    @Import float x, y, rotation;

    transient @Nullable Formation formation;
    transient Array<Unitc> controlling = new Array<>();

    @Override
    public void update(){
        if(formation != null){
            formation.anchor.set(x, y, rotation);
            formation.updateSlots();
        }
    }

    @Override
    public void remove(){
        clearCommand();
    }

    @Override
    public void killed(){
        clearCommand();
    }

    //make sure to reset command state when the controller is switched
    @Override
    public void controller(UnitController next){
        clearCommand();
    }

    void command(Formation formation, Array<Unitc> units){
        clearCommand();

        controlling.addAll(units);
        for(Unitc unit : units){
            unit.controller(new FormationAI(this, formation));
        }
        this.formation = formation;

        members.clear();
        for(Unitc u : units){
            members.add((FormationAI)u.controller());
        }


        //TODO doesn't handle units that don't fit a formation
        formation.addMembers(members);
    }

    boolean isCommanding(){
        return formation != null;
    }

    void clearCommand(){
        //reset controlled units
        for(Unitc unit : controlling){
            if(unit.controller().isBeingControlled(this)){
                unit.controller(unit.type().createController());
            }
        }

        controlling.clear();
        formation = null;
    }
}
