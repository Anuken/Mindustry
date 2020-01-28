package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.world.*;

public class Battery extends PowerDistributor{
    public TextureRegion bottomRegion;
    public TextureRegion bottomIconRegion;
    
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
        bottomRegion = Core.atlas.find(name + "-bottom");
        bottomIconRegion = Core.atlas.find(name + "-icon-lights");
    }

    @Override
    public void draw(Tile tile){
        Draw.color(emptyLightColor, fullLightColor, tile.entity.power.status);
        Draw.rect(bottomRegion, tile.drawx(), tile.drawy());
        Draw.reset();
        
        super.draw(tile);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-icon-lights")};
    }

}
