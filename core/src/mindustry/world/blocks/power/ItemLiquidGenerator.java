package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Power generation block which can use items, liquids or both as input sources for power production.
 * Liquids will take priority over items.
 */
public class ItemLiquidGenerator extends PowerGenerator{
    public float minItemEfficiency = 0.2f;
    /** The time in number of ticks during which a single item will produce power. */
    public float itemDuration = 70f;

    public float minLiquidEfficiency = 0.2f;
    /** Maximum liquid used per frame. */
    public float maxLiquidGenerate = 0.4f;

    public Effect generateEffect = Fx.generatespark;
    public Effect explodeEffect = Fx.generatespark;
    public Color heatColor = Color.valueOf("ff9b59");
    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-liquid") TextureRegion liquidRegion;
    public boolean randomlyExplode = true;
    public boolean defaults = false;

    public ItemLiquidGenerator(boolean hasItems, boolean hasLiquids, String name){
        this(name);
        this.hasItems = hasItems;
        this.hasLiquids = hasLiquids;
        setDefaults();
    }

    public ItemLiquidGenerator(String name){
        super(name);
    }

    protected void setDefaults(){
        if(hasItems){
            consumes.add(new ConsumeItemFilter(item -> getItemEfficiency(item) >= minItemEfficiency)).update(false).optional(true, false);
        }

        if(hasLiquids){
            consumes.add(new ConsumeLiquidFilter(liquid -> getLiquidEfficiency(liquid) >= minLiquidEfficiency, maxLiquidGenerate)).update(false).optional(true, false);
        }

        defaults = true;
    }

    @Override
    public void init(){
        if(!defaults){
            setDefaults();
        }
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(BlockStat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    protected float getItemEfficiency(Item item){
        return 0.0f;
    }

    protected float getLiquidEfficiency(Liquid liquid){
        return 0.0f;
    }

    public class ItemLiquidGeneratorBuild extends GeneratorBuild{
        public float explosiveness, heat, totalTime;

        @Override
        public boolean productionValid(){
            return generateTime > 0;
        }

        @Override
        public void updateTile(){
            //Note: Do not use this delta when calculating the amount of power or the power efficiency, but use it for resource consumption if necessary.
            //Power amount is delta'd by PowerGraph class already.
            float calculationDelta = delta();

            heat = Mathf.lerpDelta(heat, generateTime >= 0.001f ? 1f : 0f, 0.05f);

            if(!consValid()){
                productionEfficiency = 0.0f;
                return;
            }

            Liquid liquid = null;
            for(Liquid other : content.liquids()){
                if(hasLiquids && liquids.get(other) >= 0.001f && getLiquidEfficiency(other) >= minLiquidEfficiency){
                    liquid = other;
                    break;
                }
            }

            totalTime += heat * Time.delta;

            //liquid takes priority over solids
            if(hasLiquids && liquid != null && liquids.get(liquid) >= 0.001f){
                float baseLiquidEfficiency = getLiquidEfficiency(liquid);
                float maximumPossible = maxLiquidGenerate * calculationDelta;
                float used = Math.min(liquids.get(liquid) * calculationDelta, maximumPossible);

                liquids.remove(liquid, used * power.graph.getUsageFraction());
                productionEfficiency = baseLiquidEfficiency * used / maximumPossible;

                if(used > 0.001f && Mathf.chance(0.05 * delta())){
                    generateEffect.at(x + Mathf.range(3f), y + Mathf.range(3f));
                }
            }else if(hasItems){
                // No liquids accepted or none supplied, try using items if accepted
                if(generateTime <= 0f && items.total() > 0){
                    generateEffect.at(x + Mathf.range(3f), y + Mathf.range(3f));
                    Item item = items.take();
                    productionEfficiency = getItemEfficiency(item);
                    explosiveness = item.explosiveness;
                    generateTime = 1f;
                }

                if(generateTime > 0f){
                    generateTime -= Math.min(1f / itemDuration * delta() * power.graph.getUsageFraction(), generateTime);

                    if(randomlyExplode && state.rules.reactorExplosions && Mathf.chance(delta() * 0.06 * Mathf.clamp(explosiveness - 0.5f))){
                        //this block is run last so that in the event of a block destruction, no code relies on the block type
                        Core.app.post(() -> {
                            damage(Mathf.random(11f));
                            explodeEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                        });
                    }
                }else{
                    productionEfficiency = 0.0f;
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(hasItems){
                Draw.color(heatColor);
                Draw.alpha(heat * 0.4f + Mathf.absin(Time.time(), 8f, 0.6f) * heat);
                Draw.rect(topRegion, x, y);
                Draw.reset();
            }

            if(hasLiquids){
                Draw.color(liquids.current().color);
                Draw.alpha(liquids.currentAmount() / liquidCapacity);
                Draw.rect(liquidRegion, x, y);
                Draw.color();
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, (60f + Mathf.absin(10f, 5f)) * size, Color.orange, 0.5f * heat);
        }
    }
}
