package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeItem;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class GenericCrafter extends Block{
    protected final int timerDump = timers++;

    protected Item output;
    protected float craftTime = 80;
    protected Effect craftEffect = BlockFx.purify;
    protected Effect updateEffect = Fx.none;
    protected float updateEffectChance = 0.04f;

    public GenericCrafter(String name){
        super(name);
        update = true;
        solid = true;
        health = 60;
    }

    @Override
    public void setBars(){
        super.setBars();

        if(consumes.has(ConsumeItem.class)) bars.replace(new BlockBar(BarType.inventory, true,
                tile -> (float) tile.entity.items.get(consumes.item()) / itemCapacity));
    }

    @Override
    public void init(){
        super.init();

        produces.set(output);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.craftSpeed, 60f / craftTime, StatUnit.itemsSecond);
        stats.add(BlockStat.outputItem, output);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name(), tile.drawx(), tile.drawy());

        if(!hasLiquids) return;

        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
        Draw.color();
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name)};
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        if(entity.cons.valid() && tile.entity.items.get(output) < itemCapacity){

            entity.progress += getProgressIncrease(entity, craftTime);
            entity.totalProgress += entity.delta();
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);

            if(Mathf.chance(Timers.delta() * updateEffectChance))
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 4f), entity.y + Mathf.range(size * 4));
        }else{
            entity.warmup = Mathf.lerp(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){

            if(consumes.has(ConsumeItem.class)) tile.entity.items.remove(consumes.item(), consumes.itemAmount());

            useContent(tile, output);

            offloadNear(tile, output);
            Effects.effect(craftEffect, tile.drawx(), tile.drawy());
            entity.progress = 0f;
        }

        if(tile.entity.timer.get(timerDump, 5)){
            tryDump(tile, output);
        }
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
