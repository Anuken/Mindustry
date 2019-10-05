package io.anuke.mindustry.game;

/** Holds objective classes. */
public class Objectives{

    public static class WaveObjective implements Objective{
        public int wave;

        public WaveObjective(int wave){
            this.wave = wave;
        }

        protected WaveObjective(){}

        @Override
        public boolean complete(){
            return false;
        }

        @Override
        public String display(){
            return null;
        }
    }

    public static class LaunchObjective implements Objective{

        @Override
        public boolean complete(){
            return false;
        }

        @Override
        public String display(){
            return null;
        }
    }
}
