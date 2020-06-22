package mindustry.game;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

/** Defines a specific objective for a game. */
public interface Objective{

    /** @return whether this objective is met. */
    boolean complete();

    /** @return the string displayed when this objective is completed, in imperative form.
     * e.g. when the objective is 'complete 10 waves', this would display "complete 10 waves".
     * If this objective should not be displayed, should return null.*/
    @Nullable String display();

    /** Build a display for this zone requirement.*/
    default void build(Table table){

    }

    default Zone zone(){
        return this instanceof ZoneObjective ? ((ZoneObjective)this).zone : null;
    }
}
