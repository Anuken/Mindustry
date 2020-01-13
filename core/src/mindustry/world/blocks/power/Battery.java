package mindustry.world.blocks.power;

public class Battery extends PowerDistributor{

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
    }
    
    @Override
    public void load(){
        super.load();
    }
}
