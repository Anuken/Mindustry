package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import mindustry.entities.type.TileEntity;
import mindustry.world.*;
import mindustry.world.modules.PowerModule;

public class Battery extends PowerDistributor{
    public TextureRegion lightsRegion;
    public Color lightColor = Color.valueOf("fb9567");

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

        Draw.color(lightColor);
        Draw.alpha(entity.power.status);
        Draw.rect(lightsRegion, tile.drawx(), tile.drawy());

        Draw.reset();
    }

}
