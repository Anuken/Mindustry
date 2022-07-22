package mindustry.world.blocks.power;

import mindustry.world.blocks.heat.*;

public class VariableReactor extends PowerGenerator{
    //TODO
    public float maxEfficiency = 1f;
    //TODO
    public float maxHeat = 100f;

    public VariableReactor(String name){
        super(name);
    }

    public class VariableReactorBuild extends GeneratorBuild implements HeatConsumer{
        public float[] sideHeat = new float[4];
        public float heat = 0f;

        @Override
        public void updateTile(){
            heat = calculateHeat(sideHeat);
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float heatRequirement(){
            //TODO
            return maxHeat;
        }
    }
}
