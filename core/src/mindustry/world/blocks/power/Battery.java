package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.entities.type.TileEntity;
import mindustry.world.*;
import mindustry.world.modules.PowerModule;

public class Battery extends PowerDistributor{
    public TextureRegion lightsRegion;

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
    }
    
    @Override
    public void load(){
        super.load();
        lightsRegion = Core.atlas.find(name + "-lights");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);
        TileEntity ent = tile.ent();
        PowerModule entity = ent.power;

        Draw.alpha(entity.graph.getBatteryStored() / entity.graph.getBatteryCapacity());
        Draw.rect(lightsRegion, tile.drawx(), tile.drawy());

        Draw.reset();
    }

}
