package mindustry.world.blocks.heat;

/** Basic interface for any block that produces or requires heat.*/
public interface HeatBlock{
    float heat();

    //potentially unnecessary
    /*
    void heat(float value);

    default void addHeat(float amount){
        heat(heat() + amount);
    }*/
}
