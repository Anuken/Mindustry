package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.util.Arrays;

public class PowerSmelter extends PowerBlock {
    protected final int timerDump = timers++;
    protected final int timerCraft = timers++;

    protected ItemStack[] inputs;
    protected Item result;
    protected float powerDrain = 0.01f;

    protected float heatUpTime = 80f;
    protected float minHeat = 0.5f;

    protected float craftTime = 20f; //time to craft one item, so max 3 items per second by default
    protected float burnDuration = 50f; //by default, the fuel will burn 45 frames, so that's 2.5 items/fuel at most
    protected float burnEffectChance = 0.01f;
    protected Effect craftEffect = Fx.smelt, burnEffect = Fx.fuelburn;

    protected int capacity = 20;

    public PowerSmelter(String name) {
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove(BarType.inventory);

        for(ItemStack item : inputs){
            bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.inventory.getItem(item.item)/capacity));
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add("input", Arrays.toString(inputs));
        stats.add("powersecond", Strings.toFixed(powerDrain*60f, 2));
        stats.add("output", result);
        stats.add("fuelduration", Strings.toFixed(burnDuration/60f, 1));
        stats.add("maxoutputsecond", Strings.toFixed(60f/craftTime, 1));
        stats.add("inputcapacity", capacity);
        stats.add("outputcapacity", capacity);
    }

    @Override
    public void update(Tile tile){
        PowerSmelterEntity entity = tile.entity();

        if(entity.timer.get(timerDump, 5) && entity.inventory.hasItem(result)){
            tryDump(tile, result);
        }

        float used = powerDrain * Timers.delta();

        //heat it up if there's enough power
        if(entity.power.amount > used){
            entity.power.amount -= used;
            entity.heat += 1f /heatUpTime;
            if(Mathf.chance(Timers.delta() * burnEffectChance)) Effects.effect(burnEffect, entity.x + Mathf.range(2f), entity.y + Mathf.range(2f));
        }else{
            entity.heat -= 1f /heatUpTime;
        }

        entity.heat = Mathf.clamp(entity.heat);

        //make sure it has all the items
        for(ItemStack item : inputs){
            if(!entity.inventory.hasItem(item.item, item.amount)){
                return;
            }
        }

        if(entity.inventory.getItem(result) >= capacity //output full
                || entity.heat <= minHeat //not burning
                || !entity.timer.get(timerCraft, craftTime)){ //not yet time
            return;
        }

        for(ItemStack item : inputs){
            entity.inventory.removeItem(item.item, item.amount);
        }

        offloadNear(tile, result);
        Effects.effect(craftEffect, entity);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        boolean isInput = false;

        for(ItemStack req : inputs){
            if(req.item == item){
                isInput = true;
                break;
            }
        }

        return (isInput && tile.entity.inventory.getItem(item) < capacity);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        PowerSmelterEntity entity = tile.entity();

        //draw glowing center
        if(entity.heat > 0f){
            Draw.color(1f, 1f, 1f, Mathf.absin(Timers.time(), 9f, 0.4f) + Mathf.random(0.05f));
            Draw.rect("smelter-middle", tile.drawx(), tile.drawy());
            Draw.color();
        }
    }

    @Override
    public TileEntity getEntity() {
        return new PowerSmelterEntity();
    }

    class PowerSmelterEntity extends PowerEntity{
        public float heat;
    }
}
