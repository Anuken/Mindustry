package mindustry.world.consumers;

/** A ConsumeLiquidFilter that consumes specific coolant, selected based on stats. */
public class ConsumeCoolant extends ConsumeLiquidFilter{
    public float maxTemp = 0.5f, maxFlammability = 0.1f;

    public ConsumeCoolant(float amount){
        this.filter = liquid -> liquid.temperature <= maxTemp && liquid.flammability < maxFlammability;
        this.amount = amount;
    }

    //mods
    public ConsumeCoolant(){
        this(0.1f);
    }
}
