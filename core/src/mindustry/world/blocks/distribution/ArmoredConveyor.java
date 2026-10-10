package mindustry.world.blocks.distribution;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
        armored = true;
        noSideBlend = true;
    }
}
