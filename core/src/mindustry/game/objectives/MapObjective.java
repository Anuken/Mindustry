package mindustry.game.objectives;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.game.markers.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** Base abstract class for any in-map objective. */
public abstract class MapObjective implements AllowSerialization{
    public boolean hidden;
    public @Nullable @Multiline String details;
    public @Nullable @LogicCode String completionLogicCode;
    public @Unordered String[] flagsAdded = {};
    public @Unordered String[] flagsRemoved = {};
    public ObjectiveMarker[] markers = {};

    /** The parents of this objective. All parents must be done in order for this to be updated. */
    public transient Seq<MapObjective> parents = new Seq<>(2);
    /** Temporary container to store references since this class is static. Will immediately be flattened. */
    transient final Seq<MapObjective> children = new Seq<>(2);

    /** For the objectives UI dialog. Do not modify directly! */
    public transient int editorX = -999, editorY = -999;

    /** Whether this objective has been done yet. This is internally set. */
    boolean completed;
    /** Internal value. Do not modify! */
    transient boolean depFinished;

    /** @return True if this objective is done and should be removed from the executor. */
    public abstract boolean update();

    /** Reset internal state, if any. */
    public void reset(){
    }

    /** Called once after {@link #update()} returns true, before this objective is removed. */
    public void done(){
        state.rules.objectiveFlags.removeAll(flagsRemoved);
        state.rules.objectiveFlags.addAll(flagsAdded);
        completed = true;

        LExecutor.runLogicScript(completionLogicCode);
    }

    /** @return true if all {@link #parents} are completed, rendering this objective able to execute. */
    public final boolean dependencyFinished(){
        if(depFinished) return true;

        for(var parent : parents){
            if(!parent.isCompleted()) return false;
        }

        return depFinished = true;
    }

    /** @return true if this objective is done (practically, has been removed from the executor). */
    public final boolean isCompleted(){
        return completed;
    }

    /** @return Whether this objective should run at all. */
    public boolean qualified(){
        return !completed && dependencyFinished();
    }

    /** @return This objective, with the given child's parents added with this, for chaining operations. */
    public MapObjective child(MapObjective child){
        child.parents.add(this);
        children.add(child);
        return this;
    }

    /** @return This objective, with the given parent added to this objective's parents, for chaining operations. */
    public MapObjective parent(MapObjective parent){
        parents.add(parent);
        return this;
    }

    /** @return This objective, with the details message assigned to, for chaining operations. */
    public MapObjective details(String details){
        this.details = details;
        return this;
    }

    /** @return This objective, with the added-flags assigned to, for chaining operations. */
    public MapObjective flagsAdded(String... flagsAdded){
        this.flagsAdded = flagsAdded;
        return this;
    }

    /** @return This objective, with the removed-flags assigned to, for chaining operations. */
    public MapObjective flagsRemoved(String... flagsRemoved){
        this.flagsRemoved = flagsRemoved;
        return this;
    }

    /** @return This objective, with the markers assigned to, for chaining operations. */
    public MapObjective markers(ObjectiveMarker... markers){
        this.markers = markers;
        return this;
    }

    /** @return Basic mission display text. If null, falls back to standard text. */
    public @Nullable String text(){
        return null;
    }

    /** @return Details that appear upon click. */
    public @Nullable String details(){
        return details;
    }

    /** @return The localized type-name of this objective, defaulting to the class simple name without the "Objective" prefix. */
    public String typeName(){
        String className = getClass().getSimpleName().replace("Objective", "");
        return Core.bundle == null ? className : Core.bundle.get("objective." + className.toLowerCase() + ".name", className);
    }

    /** Validate fields after reading to make sure none of them are null. */
    public void validate(){

    }
}
