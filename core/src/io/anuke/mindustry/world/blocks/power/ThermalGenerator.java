package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.entities.Effects;
import io.anuke.arc.entities.Effects.Effect;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.world.Tile;

public class ThermalGenerator extends PowerGenerator{
    protected Effect generateEffect = Fx.none;

    public ThermalGenerator(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        GeneratorEntity entity = tile.entity();

        if(entity.productionEfficiency > 0.1f && Mathf.chance(0.05 * entity.delta())){
            Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
        }

        super.update(tile);
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        GeneratorEntity entity = tile.entity();
        entity.productionEfficiency = 0f;
        for(Tile other : tile.getLinkedTiles(tempTiles)){
            entity.productionEfficiency += other.floor().heat;
        }
    }

    @Override
    public float getPowerProduction(Tile tile){
        //in this case, productionEfficiency means 'total heat'
        //thus, it may be greater than 1.0
        return powerProduction * tile.<GeneratorEntity>entity().productionEfficiency;
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        //make sure there's heat at this location
        return tile.getLinkedTilesAs(this, tempTiles).sum(other -> other.floor().heat) > 0.01f;
    }
}
