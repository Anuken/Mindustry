package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class PowerSmelter extends PowerBlock{
    protected final int timerDump = timers++;
    protected final int timerCraft = timers++;

    protected Item result;

    protected float minFlux = 0.2f;
    protected int fluxNeeded = 1;
    protected float fluxSpeedMult = 0.75f;
    protected float baseFluxChance = 0.25f;
    protected boolean useFlux = false;

    protected float heatUpTime = 80f;
    protected float minHeat = 0.5f;

    protected float craftTime = 20f; //time to craft one item, so max 3 items per second by default
    protected float burnEffectChance = 0.01f;
    protected Effect craftEffect = BlockFx.smelt,
            burnEffect = BlockFx.fuelburn;
    protected Color flameColor = Color.valueOf("ffc999");

    protected TextureRegion topRegion;

    public PowerSmelter(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
        itemCapacity = 20;
    }

    @Override
    public void init(){
        super.init();

        produces.set(result);
    }

    @Override
    public void load(){
        super.load();
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove(BarType.inventory);

        for(ItemStack item : consumes.items()){
            bars.add(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.items.get(item.item) / itemCapacity));
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.outputItem, result);
        stats.add(BlockStat.craftSpeed, 60f / craftTime, StatUnit.itemsSecond);
        stats.add(BlockStat.inputItemCapacity, itemCapacity, StatUnit.items);
        stats.add(BlockStat.outputItemCapacity, itemCapacity, StatUnit.items);
    }

    @Override
    public void update(Tile tile){

        PowerSmelterEntity entity = tile.entity();

        if(entity.timer.get(timerDump, 5) && entity.items.has(result)){
            tryDump(tile, result);
        }

        //heat it up if there's enough power
        if(entity.cons.valid()){
            entity.heat += 1f / heatUpTime * entity.delta();
            if(Mathf.chance(entity.delta() * burnEffectChance))
                Effects.effect(burnEffect, entity.x + Mathf.range(size * 4f), entity.y + Mathf.range(size * 4));
        }else{
            entity.heat -= 1f / heatUpTime * Timers.delta();
        }

        entity.heat = Mathf.clamp(entity.heat);
        entity.time += entity.heat * entity.delta();

        if(!entity.cons.valid()){
            return;
        }

        float baseSmeltSpeed = 1f;
        for(Item item : content.items()){
            if(item.fluxiness >= minFlux && tile.entity.items.get(item) > 0){
                baseSmeltSpeed = fluxSpeedMult;
                break;
            }
        }

        entity.craftTime += entity.delta() * entity.power.satisfaction;

        if(entity.items.get(result) >= itemCapacity //output full
                || entity.heat <= minHeat //not burning
                || entity.craftTime < craftTime*baseSmeltSpeed){ //not yet time
            return;
        }

        entity.craftTime = 0f;

        boolean consumeInputs = true;

        if(useFlux){
            //remove flux materials if present
            for(Item item : content.items()){
                if(item.fluxiness >= minFlux && tile.entity.items.get(item) >= fluxNeeded){
                    tile.entity.items.remove(item, fluxNeeded);

                    //chance of not consuming inputs if flux material present
                    consumeInputs = !Mathf.chance(item.fluxiness * baseFluxChance);
                    break;
                }
            }
        }

        if(consumeInputs){
            for(ItemStack item : consumes.items()){
                entity.items.remove(item.item, item.amount);
            }
        }

        offloadNear(tile, result);
        Effects.effect(craftEffect, flameColor, tile.drawx(), tile.drawy());
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){

        for(ItemStack stack : consumes.items()){
            if(stack.item == item){
                return tile.entity.items.get(item) < itemCapacity;
            }
        }

        return useFlux && item.fluxiness >= minFlux && tile.entity.items.get(item) < itemCapacity;

    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        PowerSmelterEntity entity = tile.entity();

        //draw glowing center
        if(entity.heat > 0f && flameColor.a > 0.001f){
            float g = 0.3f;
            float r = 0.06f;
            float cr = Mathf.random(0.1f);

            Draw.alpha(((1f - g) + Mathf.absin(Timers.time(), 8f, g) + Mathf.random(r) - r) * entity.heat);

            Draw.tint(flameColor);
            Fill.circle(tile.drawx(), tile.drawy(), 3f + Mathf.absin(Timers.time(), 5f, 2f) + cr);
            Draw.color(1f, 1f, 1f, entity.heat);
            Draw.rect(topRegion, tile.drawx(), tile.drawy());
            Fill.circle(tile.drawx(), tile.drawy(), 1.9f + Mathf.absin(Timers.time(), 5f, 1f) + cr);

            Draw.color();
        }
    }

    @Override
    public TileEntity newEntity(){
        return new PowerSmelterEntity();
    }

    class PowerSmelterEntity extends TileEntity{
        public float heat;
        public float time;
        public float craftTime;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            heat = stream.readFloat();
        }
    }
}
