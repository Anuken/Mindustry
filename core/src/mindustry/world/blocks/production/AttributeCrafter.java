package mindustry.world.blocks.production;

import arc.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/** A crafter that gains efficiency from attribute tiles. */
public class AttributeCrafter extends GenericCrafter{
    public Attribute attribute = Attribute.heat;
    public float baseEfficiency = 1f;
    public float maxBoost = 1f;
    public float minEfficiency = -1f;
    public boolean displayEfficiency = true;
    public boolean displayScaledOutput = true;
    public boolean scaleLiquidConsumption = false;
    /** Scaled output (yield) multiplier, scales with attribute. <=0 to disable. */
    public float outputScale = 0f;
    /** Scaled efficiency (speed) multiplier, scales with attribute. <=0 to disable. */
    public float boostScale = 1f;

    public AttributeCrafter(String name){
        super(name);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        if(!displayEfficiency && !displayScaledOutput) return;

        drawPlaceText(
        (displayEfficiency && boostScale > 0f?
            Core.bundle.format("bar.efficiency",
            (int)((baseEfficiency + Math.min(maxBoost, boostScale * sumAttribute(attribute, x, y))) * 100f))
        : "") +
        (displayScaledOutput && outputScale > 0f ? "\n" +
            Core.bundle.format("bar.yield",
            (int)(Math.min(maxBoost, outputScale * sumAttribute(attribute, x, y)) * 100f))
        : ""), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(!displayEfficiency && !displayScaledOutput) return;

        if(displayEfficiency && boostScale > 0f){
            addBar("efficiency", (AttributeCrafterBuild entity) ->
            new Bar(
            () -> Core.bundle.format("bar.efficiency", (int)(entity.efficiencyMultiplier() * 100)),
            () -> Pal.lightOrange,
            entity::efficiencyMultiplier));
        }
        if(displayScaledOutput && outputScale > 0f){
            addBar("yield", (AttributeCrafterBuild entity) ->
            new Bar(
            () -> Core.bundle.format("bar.yield", (int)((entity.outputMultiplier() - baseEfficiency) * 100)),
            () -> Pal.lightOrange,
            entity::outputMultiplier));
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        //make sure there's enough efficiency at this location
        return baseEfficiency + tile.getLinkedTilesAs(this, tempTiles).sumf(other -> other.floor().attributes.get(attribute)) >= minEfficiency;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(baseEfficiency <= 0.0001f ? Stat.tiles : Stat.affinities, attribute, floating, boostScale * size * size, outputScale * size * size, Seq.with(outputItems), craftTime, !displayEfficiency);
    }

    public class AttributeCrafterBuild extends GenericCrafterBuild{
        public float attrsum;
        public float accumulator;
        public float scaledOutput;
        public int scaledInt;

        @Override
        public float getProgressIncrease(float base){
            return super.getProgressIncrease(base) * efficiencyMultiplier();
        }

        public float outputMultiplier(){
            return baseEfficiency + Math.min(maxBoost, outputScale * attrsum) + attribute.env();
        }

        public float efficiencyMultiplier(){
            return baseEfficiency + Math.min(maxBoost, boostScale * attrsum) + attribute.env();
        }

        @Override
        public float efficiencyScale(){
            return scaleLiquidConsumption ? efficiencyMultiplier() : super.efficiencyScale();
        }

        @Override
        public float scaleOutput(float amount, boolean item, boolean accumulate){
            scaledOutput = amount * outputMultiplier();

            if(item){
                if(accumulate){
                    accumulator += scaledOutput;
                    scaledInt = (int)(accumulator);
                    accumulator -= scaledInt;
                }else{
                    scaledInt = (int)(accumulator + scaledOutput);
                }
            }

            return outputScale > 0f ? Math.min(itemCapacity, scaledInt) : amount;
        }

        @Override
        public void pickedUp(){
            attrsum = 0f;
            warmup = 0f;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            attrsum = sumAttribute(attribute, tile.x, tile.y);
        }
    }
}
