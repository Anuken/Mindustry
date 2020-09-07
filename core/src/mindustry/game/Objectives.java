package mindustry.game;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.type.*;
import mindustry.world.*;

/** Holds objective classes. */
public class Objectives{

    public static class Unlock implements Objective{
        public @NonNull Block block;

        public Unlock(Block block){
            this.block = block;
        }

        protected Unlock(){}

        @Override
        public boolean complete(){
            return block.unlocked();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.unlock", block.localizedName);
        }
    }

    public static class SectorWave extends SectorObjective{
        public int wave;

        public SectorWave(SectorPreset zone, int wave){
            this.preset = zone;
            this.wave = wave;
        }

        protected SectorWave(){}

        @Override
        public boolean complete(){
            return preset.bestWave() >= wave;
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.wave", wave, preset.localizedName);
        }
    }

    public static class Launched extends SectorObjective{

        public Launched(SectorPreset zone){
            this.preset = zone;
        }

        protected Launched(){}

        @Override
        public boolean complete(){
            return preset.hasLaunched();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.core", preset.localizedName);
        }
    }

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
