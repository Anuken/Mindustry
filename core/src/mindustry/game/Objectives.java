package mindustry.game;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.ctype.*;
import mindustry.type.*;

/** Holds objective classes. */
public class Objectives{

    public static class Research implements Objective{
        public @NonNull UnlockableContent content;

        public Research(UnlockableContent content){
            this.content = content;
        }

        protected Research(){}

        @Override
        public boolean complete(){
            return content.unlocked();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.research", content.emoji() + " " + content.localizedName);
        }
    }

    public static class SectorComplete extends SectorObjective{

        public SectorComplete(SectorPreset zone){
            this.preset = zone;
        }

        protected SectorComplete(){}

        @Override
        public boolean complete(){
            return preset.sector.isCaptured();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.capture", preset.localizedName);
        }
    }

    //TODO merge
    public abstract static class SectorObjective implements Objective{
        public @NonNull SectorPreset preset;
    }

    /** Defines a specific objective for a game. */
    public interface Objective{

        /** @return whether this objective is met. */
        boolean complete();

        /** @return the string displayed when this objective is completed, in imperative form.
         * e.g. when the objective is 'complete 10 waves', this would display "complete 10 waves". */
        String display();

        /** Build a display for this zone requirement.*/
        default void build(Table table){

        }

        default SectorPreset zone(){
            return this instanceof SectorObjective ? ((SectorObjective)this).preset : null;
        }
    }
}
