package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.math.*;

/** Handles tutorial state. */
public class Tutorial{
    public TutorialStage stage = TutorialStage.values()[0];

    /** Resets tutorial state. */
    public void reset(){
        stage = TutorialStage.values()[0];
    }

    /** Goes on to the next tutorial step. */
    public void next(){
        stage = TutorialStage.values()[Mathf.clamp(stage.ordinal() + 1, 0, TutorialStage.values().length)];
    }

    public enum TutorialStage{
        intro;

        public final String text;

        TutorialStage(){
            text = Core.bundle.get("tutorial." + name());
        }


    }

}
