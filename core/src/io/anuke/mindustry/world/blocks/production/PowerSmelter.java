package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class PowerSmelter extends PowerBlock {
    protected final int timerDump = timers++;
    protected final int timerCraft = timers++;

    /**Recipe format:
     * First item in each array: result
     * Everything else in each array: requirements. Can have duplicates.*/
    protected ItemStack[] inputs;
    protected Item result;
    protected float powerUse;

    protected float minFlux = 0.2f;
    protected int fluxNeeded = 1;
    protected float baseFluxChance = 0.15f;
    protected boolean useFlux = false;

    protected float heatUpTime = 80f;
    protected float minHeat = 0.5f;

    protected float craftTime = 20f; //time to craft one item, so max 3 items per second by default
    protected float burnEffectChance = 0.01f;
    protected Effect craftEffect = BlockFx.smelt,
            burnEffect = BlockFx.fuelburn;
    protected Color flameColor = Color.valueOf("ffc999");

    public PowerSmelter(String name) {
        super(name);
        hasItems = true;
        update = true;
        solid = true;
        itemCapacity = 20;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove(BarType.inventory);

        for(ItemStack item : inputs){
            bars.add(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.items.getItem(item.item) / itemCapacity));
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        //TODO input/outputs

        stats.add(BlockStat.inputItems, Arrays.toString(inputs));
        stats.add(BlockStat.powerUse, powerUse * 60f);
        stats.add(BlockStat.outputItem, result.toString());
        stats.add(BlockStat.craftSpeed, 60f/craftTime);
        stats.add(BlockStat.inputItemCapacity, itemCapacity);
        stats.add(BlockStat.outputItemCapacity, itemCapacity);
    }

    @Override
    public void update(Tile tile){

        PowerSmelterEntity entity = tile.entity();

        if(entity.timer.get(timerDump, 5) && entity.items.hasItem(result)){
            tryDump(tile, result);
        }

        float used = powerUse * Timers.delta();

        //heat it up if there's enough power
        if(entity.power.amount > used){
            entity.power.amount -= used;
            entity.heat += 1f / heatUpTime;
            if(Mathf.chance(Timers.delta() * burnEffectChance))
                Effects.effect(burnEffect, entity.x + Mathf.range(size*4f), entity.y + Mathf.range(size*4));
        }else{
            entity.heat -= 1f / heatUpTime;
        }

        entity.heat = Mathf.clamp(entity.heat);

        //make sure it has all the items
        for(ItemStack item : inputs){
            if(!entity.items.hasItem(item.item, item.amount)){
                return;
            }
        }

        if(entity.items.getItem(result) >= itemCapacity //output full
                || entity.heat <= minHeat //not burning
                || !entity.timer.get(timerCraft, craftTime)){ //not yet time
            return;
        }

        boolean consumeInputs = true;

        if(useFlux){
            //remove flux materials if present
            for(Item item : Item.all()){
                if(item.fluxiness >= minFlux && tile.entity.items.getItem(item) >= fluxNeeded){
                    tile.entity.items.removeItem(item, fluxNeeded);

                    //chance of not consuming inputs if flux material present
                    consumeInputs = !Mathf.chance(item.fluxiness * baseFluxChance);
                    break;
                }
            }
        }

        if(consumeInputs) {
            for (ItemStack item : inputs) {
                entity.items.removeItem(item.item, item.amount);
            }
        }

        offloadNear(tile, result);
        Effects.effect(craftEffect, flameColor, tile.drawx(), tile.drawy());
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){

        for(ItemStack stack : inputs){
            if(stack.item == item){
                return tile.entity.items.getItem(item) < itemCapacity;
            }
        }

        if(useFlux && item.fluxiness >= minFlux){
            return tile.entity.items.getItem(item) < itemCapacity;
        }

        return false;
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item) {
        return itemCapacity - tile.entity.items.getItem(item);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        PowerSmelterEntity entity = tile.entity();

        //draw glowing center
        if(entity.heat > 0f){
            float g = 0.3f;
            float r = 0.06f;
            float cr = Mathf.random(0.1f);

            Draw.alpha(((1f-g) + Mathf.absin(Timers.time(), 8f, g) + Mathf.random(r) - r) * entity.heat);

            Draw.tint(flameColor);
            Fill.circle(tile.drawx(), tile.drawy(), 3f + Mathf.absin(Timers.time(), 5f, 2f) + cr);
            Draw.color(1f, 1f, 1f, entity.heat);
            Draw.rect(name + "-top", tile.drawx(), tile.drawy());
            Fill.circle(tile.drawx(), tile.drawy(), 1.9f + Mathf.absin(Timers.time(), 5f, 1f) + cr);

            Draw.color();
        }
    }

    @Override
    public TileEntity getEntity() {
        return new PowerSmelterEntity();
    }

    class PowerSmelterEntity extends TileEntity{
        public float heat;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            heat = stream.readFloat();
        }
    }
}
