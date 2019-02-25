package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerSmelter extends PowerBlock{
    protected final int timerDump = timers++;

    protected Item output;

    protected float heatUpTime = 80f;
    protected float minHeat = 0.5f;

    protected float craftTime = 20f; //time to craft one item, so max 3 items per second by default
    protected float burnEffectChance = 0.01f;
    protected Effect craftEffect = Fx.smelt,
            burnEffect = Fx.fuelburn;
    protected Color flameColor = Color.valueOf("ffc999");

    protected TextureRegion topRegion;

    public PowerSmelter(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
    }

    @Override
    public void init(){
        super.init();

        produces.set(output);
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.outputItem, output);
        stats.add(BlockStat.craftSpeed, 60f / craftTime, StatUnit.itemsSecond);
        stats.add(BlockStat.inputItemCapacity, itemCapacity, StatUnit.items);
        stats.add(BlockStat.outputItemCapacity, itemCapacity, StatUnit.items);
    }

    @Override
    public void update(Tile tile){

        PowerSmelterEntity entity = tile.entity();

        if(entity.timer.get(timerDump, 5) && entity.items.has(output)){
            tryDump(tile, output);
        }

        //heat it up if there's enough power
        if(entity.cons.valid()){
            entity.heat += 1f / heatUpTime * entity.delta();
            if(Mathf.chance(entity.delta() * burnEffectChance))
                Effects.effect(burnEffect, entity.x + Mathf.range(size * 4f), entity.y + Mathf.range(size * 4));
        }else{
            entity.heat -= 1f / heatUpTime * Time.delta();
        }

        entity.heat = Mathf.clamp(entity.heat);
        entity.time += entity.heat * entity.delta();

        if(!entity.cons.valid()){
            return;
        }

        entity.craftTime += entity.delta() * entity.power.satisfaction;

        if(entity.items.get(output) >= itemCapacity //output full
                || entity.heat <= minHeat //not burning
                || entity.craftTime < craftTime){ //not yet time
            return;
        }

        entity.craftTime = 0f;

        for(ItemStack item : consumes.items()){
            entity.items.remove(item.item, item.amount);
        }

        offloadNear(tile, output);
        Effects.effect(craftEffect, flameColor, tile.drawx(), tile.drawy());
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){

        for(ItemStack stack : consumes.items()){
            if(stack.item == item){
                return tile.entity.items.get(item) < itemCapacity;
            }
        }

        return false;
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

            Draw.alpha(((1f - g) + Mathf.absin(Time.time(), 8f, g) + Mathf.random(r) - r) * entity.heat);

            Draw.tint(flameColor);
            Fill.circle(tile.drawx(), tile.drawy(), 3f + Mathf.absin(Time.time(), 5f, 2f) + cr);
            Draw.color(1f, 1f, 1f, entity.heat);
            Draw.rect(topRegion, tile.drawx(), tile.drawy());
            Fill.circle(tile.drawx(), tile.drawy(), 1.9f + Mathf.absin(Time.time(), 5f, 1f) + cr);

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
