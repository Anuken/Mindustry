package io.anuke.mindustry.entities.traits;

/** A class for gracefully merging mining and building traits.*/
public interface BuilderMinerTrait extends MinerTrait, BuilderTrait{

    default void updateMechanics(){
        updateBuilding();

        //mine only when not building
        if(buildRequest() == null){
            updateMining();
        }
    }

    default void drawMechanics(){
        if(isBuilding()){
            drawBuilding();
        }else{
            drawMining();
        }
    }
}
