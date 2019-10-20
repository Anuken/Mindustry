package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

/** Holds objective classes. */
public class Objectives{

    //TODO
    public static class Wave implements Objective{
        public int wave;

        public Wave(int wave){
            this.wave = wave;
        }

        protected Wave(){}

        @Override
        public boolean complete(){
            return false;
        }

        @Override
        public String display(){
            //TODO
            return null;
        }
    }

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

    public static class ZoneWave extends ZoneObjective{
        public int wave;

        public ZoneWave(Zone zone, int wave){
            this.zone = zone;
            this.wave = wave;
        }

        protected ZoneWave(){}

        @Override
        public boolean complete(){
            return zone.bestWave() >= wave;
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.wave", wave, zone.localizedName);
        }
    }

    public static class Launched extends ZoneObjective{

        public Launched(Zone zone){
            this.zone = zone;
        }

        protected Launched(){}

        @Override
        public boolean complete(){
            return zone.hasLaunched();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirement.core", zone.localizedName);
        }
    }

    public abstract static class ZoneObjective implements Objective{
        public @NonNull Zone zone;
    }
}
