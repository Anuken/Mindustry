package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.LiquidStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidBase;
import io.anuke.mindustry.world.consumers.ConsumeType;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class GenericCrafter extends Block{
    protected final int timerDump = timers++;

    protected ItemStack outputItem;
    protected LiquidStack outputLiquid;

    protected float craftTime = 80;
    protected Effect craftEffect = Fx.none;
    protected Effect updateEffect = Fx.none;
    protected float updateEffectChance = 0.04f;

    protected Consumer<Tile> drawer = null;
    protected Supplier<TextureRegion[]> drawIcons = null;

    public GenericCrafter(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        health = 60;
    }

    @Override
    public void setStats(){
        if(consumes.has(ConsumeType.liquid)){
            ConsumeLiquidBase cons = consumes.get(ConsumeType.liquid);
            cons.timePeriod = craftTime;
        }

        super.setStats();
        stats.add(BlockStat.productionTime, craftTime / 60f, StatUnit.seconds);

        if(outputItem != null){
            stats.add(BlockStat.output, outputItem);
        }

        if(outputLiquid != null){
            stats.add(BlockStat.output, outputLiquid.liquid, outputLiquid.amount, false);
        }
    }

    @Override
    public void draw(Tile tile){
        if(drawer == null){
            super.draw(tile);
        }else{
            drawer.accept(tile);
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return drawIcons == null ? super.generateIcons() : drawIcons.get();
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        if(entity.cons.valid()){

            entity.progress += getProgressIncrease(entity, craftTime);
            entity.totalProgress += entity.delta();
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);

            if(Mathf.chance(Time.delta() * updateEffectChance)){
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 4f), entity.y + Mathf.range(size * 4));
            }
        }else{
            entity.warmup = Mathf.lerp(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){
            entity.cons.trigger();

            if(outputItem != null){
                useContent(tile, outputItem.item);
                for(int i = 0; i < outputItem.amount; i++){
                    offloadNear(tile, outputItem.item);
                }
            }

            if(outputLiquid != null){
                useContent(tile, outputLiquid.liquid);
                handleLiquid(tile, tile, outputLiquid.liquid, outputLiquid.amount);
            }

            Effects.effect(craftEffect, tile.drawx(), tile.drawy());
            entity.progress = 0f;
        }

        if(outputItem != null && tile.entity.timer.get(timerDump, 5)){
            tryDump(tile, outputItem.item);
        }

        if(outputLiquid != null){
            tryDumpLiquid(tile, outputLiquid.liquid);
        }
    }

    @Override
    public boolean canProduce(Tile tile){
        return super.canProduce(tile);
    }

    @Override
    public TileEntity newEntity(){
        return new GenericCrafterEntity();
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity;
    }

    public static class GenericCrafterEntity extends TileEntity{
        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(progress);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            progress = stream.readFloat();
            warmup = stream.readFloat();
        }
    }
}
