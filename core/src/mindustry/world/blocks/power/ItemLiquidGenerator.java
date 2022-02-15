package mindustry.world.blocks.power;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
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

    /** Minimum liquid efficiency for a generator to accept it. */
    public float minLiquidEfficiency = 0.2f;
    /** Maximum liquid used per frame. */
    public float maxLiquidGenerate = 0.4f;

    public Effect generateEffect = Fx.generatespark;
    public float generateEffectRnd = 3f;
    public Effect explodeEffect = Fx.generatespark;
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

        drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion());
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
        emitLight = true;
        lightRadius = 65f * size;
        if(!defaults){
            setDefaults();
        }
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    protected float getItemEfficiency(Item item){
        return 0.0f;
    }

    protected float getLiquidEfficiency(Liquid liquid){
        return 0.0f;
    }

    public class ItemLiquidGeneratorBuild extends GeneratorBuild{
        protected Boolf<Liquid> liquidFilter = other -> liquids.get(other) >= 0.001f && getLiquidEfficiency(other) >= minLiquidEfficiency;

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
            boolean cons = consValid();

            heat = Mathf.lerpDelta(heat, generateTime >= 0.001f && enabled && cons ? 1f : 0f, 0.05f);

            if(!cons){
                productionEfficiency = 0.0f;
                return;
            }

            Liquid liquid = hasLiquids ? content.liquids().find(liquidFilter) : null;
            totalTime += heat * Time.delta;

            //liquid takes priority over solids
            //TODO several issues with this! - it does not work correctly, consumption should not be handled here, why am I re-implementing consumes
            //TODO what an awful class
            if(hasLiquids && liquid != null && liquids.get(liquid) >= 0.001f){
                float baseLiquidEfficiency = getLiquidEfficiency(liquid);
                float maximumPossible = maxLiquidGenerate * calculationDelta;
                float used = Math.min(liquids.get(liquid) * calculationDelta, maximumPossible);

                liquids.remove(liquid, used);
                productionEfficiency = baseLiquidEfficiency * used / maximumPossible;

                //TODO this aggressively spams the generate effect why would anyone want this why is this here
                if(used > 0.001f && (generateTime -= delta()) <= 0f){
                    generateEffect.at(x + Mathf.range(generateEffectRnd), y + Mathf.range(generateEffectRnd));
                    generateTime = 1f;
                }
            }else if(hasItems){
                // No liquids accepted or none supplied, try using items if accepted
                if(generateTime <= 0f && items.total() > 0){
                    generateEffect.at(x + Mathf.range(generateEffectRnd), y + Mathf.range(generateEffectRnd));
                    Item item = items.take();
                    productionEfficiency = getItemEfficiency(item);
                    explosiveness = item.explosiveness;
                    generateTime = 1f;
                }

                if(generateTime > 0f){
                    generateTime -= Math.min(1f / itemDuration * delta(), generateTime);

                    if(randomlyExplode && state.rules.reactorExplosions && Mathf.chance(delta() * 0.06 * Mathf.clamp(explosiveness - 0.5f))){
                        //this block is run last so that in the event of a block destruction, no code relies on the block type
                        damage(Mathf.random(11f));
                        explodeEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                    }
                }else{
                    productionEfficiency = 0.0f;
                }
            }
        }

        @Override
        public float warmup(){
            return heat;
        }

        @Override
        public float totalProgress(){
            return totalTime;
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, (60f + Mathf.absin(10f, 5f)) * size, Color.orange, 0.5f * heat);
        }
    }
}
