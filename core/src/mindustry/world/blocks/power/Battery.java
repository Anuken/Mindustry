package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.world.*;

public class Battery extends PowerDistributor{
    public TextureRegion lightsRegion;
    public Color emptyLightColor = Color.valueOf("6e7080");
    public Color fullLightColor = Color.valueOf("fb9567");

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

        Draw.color(emptyLightColor, fullLightColor, tile.entity.power.status);
        Draw.rect(lightsRegion, tile.drawx(), tile.drawy());

        Draw.reset();
    }

}
