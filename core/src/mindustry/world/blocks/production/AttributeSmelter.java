package mindustry.world.blocks.production;

import arc.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

/** A smelter that gains efficiency from attribute tiles. */
public class AttributeSmelter extends GenericSmelter{
    public Attribute attribute = Attribute.heat;
    public float baseEfficiency = 1f;
    public float boostScale = 1f;
    public float maxHeatBoost = 1f;

    public AttributeSmelter(String name){
        super(name);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.format("bar.efficiency",
        (int)((baseEfficiency + Math.min(maxHeatBoost, boostScale * sumAttribute(attribute, x, y))) * 100f)), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("efficiency", entity ->
            new Bar(() ->
            Core.bundle.format("bar.efficiency", (int)(entity.efficiency() * 100)),
            () -> Pal.lightOrange,
            entity::efficiency));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.affinities, attribute, boostScale);
    }

    public class AttributeSmelterEntity extends SmelterEntity{
        public float attrsum;

        @Override
        public float efficiency(){
            return (baseEfficiency + Math.min(maxHeatBoost, boostScale * attrsum)) * super.efficiency();
        }

        @Override
        public void placed(){
            super.placed();

            attrsum = sumAttribute(attribute, tile.x, tile.y);
        }
    }
}