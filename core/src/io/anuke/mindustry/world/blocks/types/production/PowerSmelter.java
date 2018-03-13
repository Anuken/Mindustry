package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class PowerSmelter extends PowerBlock {
    protected final int timerDump = timers++;
    protected final int timerCraft = timers++;

    /**Recipe format:
     * First item in each array: result
     * Everything else in each array: requirements. Can have duplicates.*/
    protected Item[][] recipes;
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

        if(true) return;

        //TODO

        ObjectSet<Item> set = new ObjectSet<>();

        for(Item[] items : recipes){
            for(int i = 1; i < items.length; i ++) {
                set.add(items[i]);
            }
        }

        Array<Item> items = new Array<>();
        set.forEach(items::add);

        items.sort();

        for(Item item : items){
            bars.add(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.inventory.getItem(item) / capacity));
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        //TODO input/outputs
       // stats.add("input", Arrays.toString(inputs));
        stats.add("powersecond", Strings.toFixed(powerDrain*60f, 2));
        //stats.add("output", result);
        stats.add("fuelduration", Strings.toFixed(burnDuration/60f, 1));
        stats.add("maxoutputsecond", Strings.toFixed(60f/craftTime, 1));
        stats.add("inputcapacity", capacity);
        stats.add("outputcapacity", capacity);
    }

    @Override
    public void update(Tile tile){
        /*
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
        Effects.effect(craftEffect, entity);*/
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        boolean isInput = false;

        out:
        for(Item[] items : recipes){
            for(int i = 1; i < items.length; i ++){
                if(items[i] == item){
                    isInput = true;
                    break out;
                }
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
