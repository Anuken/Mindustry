package io.anuke.mindustry.game;

/** Defines a specific objective for a game. */
public interface Objective{

    /** @return whether this objective is met. */
    boolean complete();

    /** @return the string displayed when this objective is completed, in imperative form.
     * e.g. when the objective is 'complete 10 waves', this would display "complete 10 waves".*/
    String display();
}
