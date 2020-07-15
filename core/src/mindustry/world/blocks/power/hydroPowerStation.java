package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.type.Liquid.*;
import mindustry.entities.type.TileEntity;
import mindustry.world.consumers.*;
import mindustry.world.Tile.*;

public class HydroPowerStation extends PowerGenerator{
    
    public float powerProduction
    public double powerMultipler = 0.6
    public static Liquids liquid = Liquids.water
    
    public SolarGenerator(String name){
        super(name);
        
        
        entityType = GeneratorEntity::new;
        
    }
    
    public float getPowerProduction(Tile tile){
    if(tile.floor().liquidDrop == liquid){
        return powerProduction*tile.<GeneratorEntity>entity.productionEfficiency;
    } else {
        if(tile.entity.liquids.get(liquid) > 0.1){
            if(tile.entity.timer.get(60)){
            tile.entity.liquids.remove(liquid, 0.1);
            };
        return powerProduction*tile.<GeneratorEntity>entity.productionEfficiency;
        } else {
            return 0;
        };
    };
};
    public void draw(Tile tile){
        
        super.draw(tile);
    
    Draw.color(tile.entity.liquids.current().color);
    Draw.alpha((tile.floor().liquidDrop == liquid) ? 1 : (tile.entity.liquids.currentAmount() / this.liquidCapacity));
    Draw.rect(topRegion, tile.drawx(), tile.drawy());
    Draw.color();
        
    };
    public void update(Tile tile){
        
        tile.<GeneratorEntity>ent().productionEfficiency = (tile.floor().liquidDrop == liquid) ? 1f : (tile.entity.liquids.get(liquid) >= 0.1f) ? powerMultipler : 0f
        
    };
};