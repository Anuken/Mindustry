package mindustry.game.conditions;

import arc.scene.ui.layout.*;

/** Defines a condition to be met for content to be unlocked/researchable. */
public interface UnlockCondition{

    /** @return whether this objective is met. */
    boolean complete();

    /**
     * @return the string displayed when this objective is completed, in imperative form.
     * e.g. when the objective is 'complete 10 waves', this would display "complete 10 waves".
     */
    String display();

    /** Build a display for this zone requirement. */
    default void build(Table table){

    }
}
