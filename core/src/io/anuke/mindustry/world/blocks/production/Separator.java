package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

/**
 * Extracts a random list of items from an input item and an input liquid.
 */
public class Separator extends Block{
    protected final int timerDump = timers++;

    protected ItemStack[] results;
    protected float filterTime;
    protected float spinnerRadius = 2.5f;
    protected float spinnerLength = 1f;
    protected float spinnerThickness = 1f;
    protected float spinnerSpeed = 2f;

    protected Color color = Color.valueOf("858585");
    protected TextureRegion liquidRegion;

    protected boolean offloading = false;

    public Separator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;

        consumes.item(Items.stone);
        consumes.liquid(Liquids.water, 0.1f);
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Draw.region(name + "-liquid");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.outputItem, new ItemFilterValue(item -> {
            for(ItemStack i : results){
                if(item == i.item) return true;
            }
            return false;
        }));
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());

        Draw.color(color);
        Lines.stroke(spinnerThickness);
        Lines.spikes(tile.drawx(), tile.drawy(), spinnerRadius, spinnerLength, 3, entity.totalProgress * spinnerSpeed);
        Draw.reset();
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        entity.totalProgress += entity.warmup * entity.delta();

        if(entity.cons.valid()){
            entity.progress += getProgressIncrease(entity, filterTime);
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){
            entity.progress = 0f;
            int sum = 0;
            for(ItemStack stack : results) sum += stack.amount;

            int i = Mathf.random(sum);
            int count = 0;
            Item item = null;

            //TODO possible desync since items are random
            for(ItemStack stack : results){
                if(i >= count && i < count + stack.amount){
                    item = stack.item;
                    break;
                }
                count += stack.amount;
            }

            entity.items.remove(consumes.item(), consumes.itemAmount());
            if(item != null){
                offloading = true;
                offloadNear(tile, item);
                offloading = false;
            }
        }

        if(entity.timer.get(timerDump, 5)){
            tryDump(tile);
        }
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return offloading || item != consumes.item();
    }

    @Override
    public TileEntity newEntity(){
        return new GenericCrafterEntity();
    }
}
