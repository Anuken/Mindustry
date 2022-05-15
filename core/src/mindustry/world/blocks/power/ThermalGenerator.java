package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ThermalGenerator extends PowerGenerator{
    public Effect generateEffect = Fx.none;
    public float effectChance = 0.05f;
    public float minEfficiency = 0f;
    public float spinSpeed = 1f;
    public float displayEfficiencyScale = 1f;
    public boolean spinners = false;
    public boolean displayEfficiency = true;
    public @Nullable LiquidStack outputLiquid;
    public Attribute attribute = Attribute.heat;

    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-rotator-blur") TextureRegion blurRegion;

    public ThermalGenerator(String name){
        super(name);
        noUpdateDisabled = true;
    }

    @Override
    public void init(){
        if(outputLiquid != null){
            outputsLiquid = true;
            hasLiquids = true;
        }
        super.init();
        //proper light clipping
        clipSize = Math.max(clipSize, 45f * size * 2f * 2f);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.tiles, attribute, floating, size * size * displayEfficiencyScale, !displayEfficiency);
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f / displayEfficiencyScale, StatUnit.powerSecond);

        if(outputLiquid != null){
            stats.add(Stat.output, StatValues.liquid(outputLiquid.liquid, outputLiquid.amount * size * size * 60f, true));
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        if(displayEfficiency){
            drawPlaceText(Core.bundle.formatFloat("bar.efficiency", sumAttribute(attribute, x, y) * 100, 1), x, y, valid);
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        //make sure there's heat at this location
        return tile.getLinkedTilesAs(this, tempTiles).sumf(other -> other.floor().attributes.get(attribute)) > minEfficiency;
    }

    @Override
    public TextureRegion[] icons(){
        return spinners ? new TextureRegion[]{region, rotatorRegion} : super.icons();
    }

    public class ThermalGeneratorBuild extends GeneratorBuild{
        public float sum, spinRotation;

        @Override
        public void updateTile(){
            productionEfficiency = sum + attribute.env();

            if(productionEfficiency > 0.1f && Mathf.chanceDelta(effectChance)){
                generateEffect.at(x + Mathf.range(3f), y + Mathf.range(3f));
            }

            spinRotation += productionEfficiency * spinSpeed;

            if(outputLiquid != null){
                float added = Math.min(productionEfficiency * delta() * outputLiquid.amount, liquidCapacity - liquids.get(outputLiquid.liquid));
                liquids.add(outputLiquid.liquid, added);
                dumpLiquid(outputLiquid.liquid);
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(spinners){
                Drawf.spinSprite(blurRegion.found() && enabled && productionEfficiency > 0 ? blurRegion : rotatorRegion, x, y, spinRotation);
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, (40f + Mathf.absin(10f, 5f)) * Math.min(productionEfficiency, 2f) * size, Color.scarlet, 0.4f);
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            sum = sumAttribute(attribute, tile.x, tile.y);
        }
    }
}
