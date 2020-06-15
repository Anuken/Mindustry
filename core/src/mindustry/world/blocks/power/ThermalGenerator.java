package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ThermalGenerator extends PowerGenerator{
    public Effect generateEffect = Fx.none;
    public Attribute attribute = Attribute.heat;

    public ThermalGenerator(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.tiles, attribute);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", sumAttribute(attribute, x, y) * 100, 1), x, y, valid);
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        //make sure there's heat at this location
        return tile.getLinkedTilesAs(this, tempTiles).sumf(other -> other.floor().attributes.get(attribute)) > 0.01f;
    }

    public class ThermalGeneratorEntity extends GeneratorEntity{
        @Override
        public void updateTile(){
            if(productionEfficiency > 0.1f && Mathf.chance(0.05 * delta())){
                generateEffect.at(x + Mathf.range(3f), y + Mathf.range(3f));
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, (40f + Mathf.absin(10f, 5f)) * productionEfficiency * size, Color.scarlet, 0.4f);
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            productionEfficiency = sumAttribute(attribute, tile.x, tile.y);
        }

        @Override
        public float getPowerProduction(){
            //in this case, productionEfficiency means 'total heat'
            //thus, it may be greater than 1.0
            return powerProduction * productionEfficiency;
        }
    }
}
