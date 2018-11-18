package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeItemFilter;
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

import static io.anuke.mindustry.Vars.tilesize;

public abstract class ItemGenerator extends PowerGenerator{
    protected float minItemEfficiency = 0.2f;
    protected float itemDuration = 70f;
    protected Effect generateEffect = BlockFx.generatespark, explodeEffect =
            BlockFx.generatespark;
    protected Color heatColor = Color.valueOf("ff9b59");
    protected TextureRegion topRegion;

    public ItemGenerator(String name){
        super(name);
        itemCapacity = 20;
        hasItems = true;

        consumes.add(new ConsumeItemFilter(item -> getItemEfficiency(item) >= minItemEfficiency)).update(false).optional(true);
    }

    @Override
    public void load(){
        super.load();
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.replace(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.items.total() / itemCapacity));
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GeneratorEntity entity = tile.entity();

        if(entity.generateTime > 0){
            Draw.color(heatColor);
            float alpha = (entity.items.total() > 0 ? 1f : Mathf.clamp(entity.generateTime));
            alpha = alpha * 0.7f + Mathf.absin(Timers.time(), 12f, 0.3f) * alpha;
            Draw.alpha(alpha);
            Draw.rect(topRegion, tile.drawx(), tile.drawy());
            Draw.reset();
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return getItemEfficiency(item) >= minItemEfficiency && tile.entity.items.total() < itemCapacity;
    }

    @Override
    public void update(Tile tile){
        ItemGeneratorEntity entity = tile.entity();

        if(entity.generateTime <= 0f && entity.items.total() > 0){
            Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
            Item item = entity.items.take();
            entity.productionEfficiency = getItemEfficiency(item);
            entity.explosiveness = item.explosiveness;
            entity.generateTime = 1f;
        }

        if(entity.generateTime > 0f){
            entity.generateTime -= Math.min(1f / itemDuration * entity.delta(), entity.generateTime);

            if(Mathf.chance(entity.delta() * 0.06 * Mathf.clamp(entity.explosiveness - 0.25f))){
                //this block is run last so that in the event of a block destruction, no code relies on the block type
                entity.damage(Mathf.random(8f));
                Effects.effect(explodeEffect, tile.worldx() + Mathf.range(size * tilesize / 2f), tile.worldy() + Mathf.range(size * tilesize / 2f));
            }
        }
    }

    protected abstract float getItemEfficiency(Item item);

    @Override
    public TileEntity newEntity(){
        return new ItemGeneratorEntity();
    }

    public static class ItemGeneratorEntity extends GeneratorEntity{
        public float explosiveness;
    }
}
