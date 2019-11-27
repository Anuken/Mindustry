package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.renderer;

public class ThermalGenerator extends PowerGenerator{
    public Effect generateEffect = Fx.none;

    public ThermalGenerator(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        GeneratorEntity entity = tile.entity();

        if(entity.productionEfficiency > 0.1f && Mathf.chance(0.05 * entity.delta())){
            Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", sumAttribute(Attribute.heat, x, y) * 100, 1), x, y, valid);
    }

    @Override
    public void drawLight(Tile tile){
        GeneratorEntity entity = tile.entity();
        renderer.lights.add(tile.drawx(), tile.drawy(), (40f + Mathf.absin(10f, 5f)) * entity.productionEfficiency * size, Color.scarlet, 0.4f);
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        GeneratorEntity entity = tile.entity();
        entity.productionEfficiency = sumAttribute(Attribute.heat, tile.x, tile.y);
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
        return tile.getLinkedTilesAs(this, tempTiles).sumf(other -> other.floor().attributes.get(Attribute.heat)) > 0.01f;
    }
}
