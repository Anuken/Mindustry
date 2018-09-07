package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import static io.anuke.mindustry.Vars.content;
public class Drill extends Block{
    protected final static float hardnessDrillMultiplier = 50f;
    protected final int timerDump = timers++;

    protected final Array<Tile> drawTiles = new Array<>();

    /**Maximum tier of blocks this drill can mine.*/
    protected int tier;
    /**Base time to drill one ore, in frames.*/
    protected float drillTime = 300;
    /**Whether the liquid is required to drill. If false, then it will be used as a speed booster.*/
    protected boolean liquidRequired = false;
    /**How many times faster the drill will progress when boosted by liquid.*/
    protected float liquidBoostIntensity = 1.6f;
    /**Speed at which the drill speeds up.*/
    protected float warmupSpeed = 0.02f;

    /**Whether to draw the item this drill is mining.*/
    protected boolean drawMineItem = false;
    /**Effect played when an item is produced. This is colored.*/
    protected Effect drillEffect = BlockFx.mine;
    /**Speed the drill bit rotates at.*/
    protected float rotateSpeed = 2f;
    /**Effect randomly played while drilling.*/
    protected Effect updateEffect = BlockFx.pulverizeSmall;
    /**Chance the update effect will appear.*/
    protected float updateEffectChance = 0.02f;

    protected boolean drawRim = false;

    protected Color heatColor = Color.valueOf("ff5512");
    protected TextureRegion rimRegion;
    protected TextureRegion rotatorRegion;
    protected TextureRegion topRegion;

    public Drill(String name){
        super(name);
        update = true;
        solid = true;
        layer = Layer.overlay;
        itemCapacity = 5;
        group = BlockGroup.drills;
        hasLiquids = true;
        liquidCapacity = 5f;
        hasItems = true;

        consumes.add(new ConsumeLiquid(Liquids.water, 0.05f)).optional(true);
    }

    @Override
    public void load(){
        super.load();
        rimRegion = Draw.region(name + "-rim");
        rotatorRegion = Draw.region(name + "-rotator");
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        float s = 0.3f;
        float ts = 0.6f;

        DrillEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());

        if(drawRim){
            Graphics.setAdditiveBlending();
            Draw.color(heatColor);
            Draw.alpha(entity.warmup * ts * (1f - s + Mathf.absin(Timers.time(), 3f, s)));
            Draw.rect(rimRegion, tile.drawx(), tile.drawy());
            Draw.color();
            Graphics.setNormalBlending();
        }

        Draw.rect(rotatorRegion, tile.drawx(), tile.drawy(), entity.drillTime * rotateSpeed);

        Draw.rect(topRegion, tile.drawx(), tile.drawy());

        if(entity.dominantItem != null && drawMineItem){
            Draw.color(entity.dominantItem.color);
            Draw.rect("blank", tile.drawx(), tile.drawy(), 2f, 2f);
            Draw.color();
        }
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.drillTier, table -> {
            Array<Item> list = new Array<>();

            for(Item item : content.items()){
                if(tier >= item.hardness && Draw.hasRegion(item.name + "1")){
                    list.add(item);
                }
            }

            for(int i = 0; i < list.size; i++){
                Item item = list.get(i);
                table.addImage(item.name + "1").size(8 * 3).padRight(2).padLeft(2).padTop(3).padBottom(3);
                if(i != list.size - 1){
                    table.add("/");
                }
            }
        });

        stats.add(BlockStat.drillSpeed, 60f / drillTime, StatUnit.itemsSecond);
    }

    @Override
    public void update(Tile tile){
        DrillEntity entity = tile.entity();

        if(entity.dominantItem == null){
            boolean foundDominant = false;

            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other)){
                    Item drop = getDrop(other);

                    if(!foundDominant){
                        entity.dominantItem = drop;
                        foundDominant = true;
                    }else if(entity.dominantItem != drop && drop.id < entity.dominantItem.id){
                        entity.dominantItem = drop;
                    }
                }
            }

            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other) && getDrop(other) == entity.dominantItem){
                    entity.dominantItems ++;
                }
            }
        }

        if(entity.dominantItem == null){
            return;
        }

        float totalHardness = entity.dominantItems * entity.dominantItem.hardness;

        if(entity.timer.get(timerDump, 15)){
            tryDump(tile);
        }

        entity.drillTime += entity.warmup * entity.delta();

        if(entity.items.total() < itemCapacity && entity.dominantItems > 0 && entity.cons.valid()){

            float speed = 1f;

            if(entity.consumed(ConsumeLiquid.class) && !liquidRequired){
                speed = liquidBoostIntensity;
            }

            entity.warmup = Mathf.lerpDelta(entity.warmup, speed, warmupSpeed);
            entity.progress += entity.delta()
            * entity.dominantItems * speed * entity.warmup;

            if(Mathf.chance(Timers.delta() * updateEffectChance * entity.warmup))
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 2f), entity.y + Mathf.range(size * 2f));
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, warmupSpeed);
            return;
        }

        if(entity.dominantItems > 0 && entity.progress >= drillTime + hardnessDrillMultiplier * Math.max(totalHardness, 1f) / entity.dominantItems
                && tile.entity.items.total() < itemCapacity){

            offloadNear(tile, entity.dominantItem);

            useContent(entity.dominantItem);

            entity.index++;
            entity.progress = 0f;

            Effects.effect(drillEffect, entity.dominantItem.color,
                    entity.x + Mathf.range(size), entity.y + Mathf.range(size));
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTilesAs(this, drawTiles)){
                if(isValid(other)){
                    return true;
                }
            }
            return false;
        }else{
            return isValid(tile);
        }
    }

    @Override
    public TileEntity getEntity(){
        return new DrillEntity();
    }

    public Item getDrop(Tile tile){
        return tile.floor().drops.item;
    }

    public boolean isValid(Tile tile){
        if(tile == null) return false;
        ItemStack drops = tile.floor().drops;
        return drops != null && drops.item.hardness <= tier;
    }

    public static class DrillEntity extends TileEntity{
        public float progress;
        public int index;
        public float warmup;
        public float drillTime;

        public int dominantItems;
        public Item dominantItem;
    }

}
