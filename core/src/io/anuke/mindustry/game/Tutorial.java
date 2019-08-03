package io.anuke.mindustry.game;

/** Handles tutorial state. */
public class Tutorial{
    private TutorialStage stage = TutorialStage.values()[0];

    public void reset(){
        stage = TutorialStage.values()[0];
    }

    /** Goes on to the next tutorial step. */
    public void next(){

    }


    public enum TutorialStage{
        intro;
    }

}
