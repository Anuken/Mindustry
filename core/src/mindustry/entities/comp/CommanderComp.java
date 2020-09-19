package mindustry.entities.comp;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.ai.formations.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** A unit that can command other units. */
@Component
abstract class CommanderComp implements Unitc{
    private static final Seq<FormationMember> members = new Seq<>();
    private static final Seq<Unit> units = new Seq<>();

    @Import float x, y, rotation;

    transient @Nullable Formation formation;
    transient Seq<Unit> controlling = new Seq<>();
    /** minimum speed of any unit in the formation. */
    transient float minFormationSpeed;

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

    void commandNearby(FormationPattern pattern){
        commandNearby(pattern, u -> true);
    }

    void commandNearby(FormationPattern pattern, Boolf<Unit> include){
        Formation formation = new Formation(new Vec3(x, y, rotation), pattern);
        formation.slotAssignmentStrategy = new DistanceAssignmentStrategy(pattern);

        units.clear();

        Units.nearby(team(), x, y, 200f, u -> {
            if(u.isAI() && include.get(u) && u != self()){
                units.add(u);
            }
        });

        units.sort(u -> u.dst2(this));
        units.truncate(type().commandLimit);

        command(formation, units);
    }

    void command(Formation formation, Seq<Unit> units){
        clearCommand();

        float spacing = hitSize() * 1.7f;
        minFormationSpeed = type().speed;

        controlling.addAll(units);
        for(Unit unit : units){
            FormationAI ai;
            unit.controller(ai = new FormationAI(self(), formation));
            spacing = Math.max(spacing, ai.formationSize());
            minFormationSpeed = Math.min(minFormationSpeed, unit.type().speed);
        }
        this.formation = formation;

        //update formation spacing based on max size
        formation.pattern.spacing = spacing;

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
        for(Unit unit : controlling){
            if(unit.controller().isBeingControlled(self())){
                unit.controller(unit.type().createController());
            }
        }

        controlling.clear();
        formation = null;
    }
}
