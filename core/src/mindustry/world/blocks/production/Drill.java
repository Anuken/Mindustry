package mindustry.world.blocks.production;

import arc.*;
import arc.struct.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Drill extends Block{
    public float hardnessDrillMultiplier = 50f;

    protected final ObjectIntMap<Item> oreCount = new ObjectIntMap<>();
    protected final Array<Item> itemArray = new Array<>();

    /** Maximum tier of blocks this drill can mine. */
    public int tier;
    /** Base time to drill one ore, in frames. */
    public float drillTime = 300;
    /** How many times faster the drill will progress when boosted by liquid. */
    public float liquidBoostIntensity = 1.6f;
    /** Speed at which the drill speeds up. */
    public float warmupSpeed = 0.02f;

    //return variables for countOre
    protected Item returnItem;
    protected int returnCount;

    /** Whether to draw the item this drill is mining. */
    public boolean drawMineItem = false;
    /** Effect played when an item is produced. This is colored. */
    public Effect drillEffect = Fx.mine;
    /** Speed the drill bit rotates at. */
    public float rotateSpeed = 2f;
    /** Effect randomly played while drilling. */
    public Effect updateEffect = Fx.pulverizeSmall;
    /** Chance the update effect will appear. */
    public float updateEffectChance = 0.02f;

    public boolean drawRim = false;
    public Color heatColor = Color.valueOf("ff5512");
    public TextureRegion rimRegion;
    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;

    public Drill(String name){
        super(name);
        update = true;
        solid = true;
        layer = Layer.overlay;
        group = BlockGroup.drills;
        hasLiquids = true;
        liquidCapacity = 5f;
        hasItems = true;
        entityType = DrillEntity::new;

        idleSound = Sounds.drill;
        idleSoundVolume = 0.003f;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("drillspeed", e -> {
            DrillEntity entity = (DrillEntity)e;

            return new Bar(() -> Core.bundle.format("bar.drillspeed", Strings.fixed(entity.lastDrillSpeed * 60 * entity.timeScale, 2)), () -> Pal.ammo, () -> entity.warmup);
        });
    }

    @Override
    public void load(){
        super.load();
        rimRegion = Core.atlas.find(name + "-rim");
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void drawCracks(Tile tile){}

    @Override
    public void draw(Tile tile){
        float s = 0.3f;
        float ts = 0.6f;

        DrillEntity entity = tile.ent();

        Draw.rect(region, tile.drawx(), tile.drawy());
        super.drawCracks(tile);

        if(drawRim){
            Draw.color(heatColor);
            Draw.alpha(entity.warmup * ts * (1f - s + Mathf.absin(Time.time(), 3f, s)));
            Draw.blend(Blending.additive);
            Draw.rect(rimRegion, tile.drawx(), tile.drawy());
            Draw.blend();
            Draw.color();
        }

        Draw.rect(rotatorRegion, tile.drawx(), tile.drawy(), entity.drillTime * rotateSpeed);

        Draw.rect(topRegion, tile.drawx(), tile.drawy());

        if(entity.dominantItem != null && drawMineItem){
            Draw.color(entity.dominantItem.color);
            Draw.rect("drill-top", tile.drawx(), tile.drawy(), 1f);
            Draw.color();
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator"), Core.atlas.find(name + "-top")};
    }

    @Override
    public boolean shouldConsume(Tile tile){
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean shouldIdleSound(Tile tile){
        return tile.entity.efficiency() > 0.01f;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);
        if(tile == null) return;

        countOre(tile);

        if(returnItem != null){
            float width = drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / (drillTime + hardnessDrillMultiplier * returnItem.hardness) * returnCount, 2), x, y, valid);
            float dx = x * tilesize + offset() - width/2f - 4f, dy = y * tilesize + offset() + size * tilesize / 2f + 5;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(returnItem.icon(Cicon.small), dx, dy - 1);
            Draw.reset();
            Draw.rect(returnItem.icon(Cicon.small), dx, dy);
        }else{
            Tile to = tile.getLinkedTilesAs(this, tempTiles).find(t -> t.drop() != null && t.drop().hardness > tier);
            Item item = to == null ? null : to.drop();
            if(item != null){
                drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y, valid);
            }
        }
    }

    @Override
    public void drawSelect(Tile tile){
        DrillEntity entity = tile.ent();

        if(entity.dominantItem != null){
            float dx = tile.drawx() - size * tilesize/2f, dy = tile.drawy() + size * tilesize/2f;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(entity.dominantItem.icon(Cicon.small), dx, dy - 1);
            Draw.reset();
            Draw.rect(entity.dominantItem.icon(Cicon.small), dx, dy);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.drillTier, table -> {
            Array<Block> list = content.blocks().select(b -> b.isFloor() && b.asFloor().itemDrop != null && b.asFloor().itemDrop.hardness <= tier);

            table.table(l -> {
                l.left();

                for(int i = 0; i < list.size; i++){
                    Block item = list.get(i);

                    l.addImage(item.icon(Cicon.small)).size(8 * 3).padRight(2).padLeft(2).padTop(3).padBottom(3);
                    l.add(item.localizedName).left().padLeft(1).padRight(4);
                    if(i % 5 == 4){
                        l.row();
                    }
                }
            });


        });

        stats.add(BlockStat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);
        if(liquidBoostIntensity > 0){
            stats.add(BlockStat.boostEffect, liquidBoostIntensity * liquidBoostIntensity, StatUnit.timesSpeed);
        }
    }

    void countOre(Tile tile){
        returnItem = null;
        returnCount = 0;

        oreCount.clear();
        itemArray.clear();

        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            if(isValid(other)){
                oreCount.getAndIncrement(getDrop(other), 0, 1);
            }
        }

        for(Item item : oreCount.keys()){
            itemArray.add(item);
        }

        itemArray.sort((item1, item2) -> {
            int type = Boolean.compare(item1 != Items.sand, item2 != Items.sand);
            if(type != 0) return type;
            int amounts = Integer.compare(oreCount.get(item1, 0), oreCount.get(item2, 0));
            if(amounts != 0) return amounts;
            return Integer.compare(item1.id, item2.id);
        });

        if(itemArray.size == 0){
            return;
        }

        returnItem = itemArray.peek();
        returnCount = oreCount.get(itemArray.peek(), 0);
    }

    @Override
    public void update(Tile tile){
        DrillEntity entity = tile.ent();

        if(entity.dominantItem == null){
            countOre(tile);
            if(returnItem == null) return;
            entity.dominantItem = returnItem;
            entity.dominantItems = returnCount;
        }

        if(entity.timer.get(timerDump, dumpTime)){
            tryDump(tile, entity.dominantItem);
        }

        entity.drillTime += entity.warmup * entity.delta();

        if(entity.items.total() < itemCapacity && entity.dominantItems > 0 && entity.cons.valid()){

            float speed = 1f;

            if(entity.cons.optionalValid()){
                speed = liquidBoostIntensity;
            }

            speed *= entity.efficiency(); // Drill slower when not at full power

            entity.lastDrillSpeed = (speed * entity.dominantItems * entity.warmup) / (drillTime + hardnessDrillMultiplier * entity.dominantItem.hardness);
            entity.warmup = Mathf.lerpDelta(entity.warmup, speed, warmupSpeed);
            entity.progress += entity.delta()
            * entity.dominantItems * speed * entity.warmup;

            if(Mathf.chance(Time.delta() * updateEffectChance * entity.warmup))
                Effects.effect(updateEffect, entity.x + Mathf.range(size * 2f), entity.y + Mathf.range(size * 2f));
        }else{
            entity.lastDrillSpeed = 0f;
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, warmupSpeed);
            return;
        }

        if(entity.dominantItems > 0 && entity.progress >= drillTime + hardnessDrillMultiplier * entity.dominantItem.hardness && tile.entity.items.total() < itemCapacity){

            offloadNear(tile, entity.dominantItem);

            useContent(tile, entity.dominantItem);

            entity.index++;
            entity.progress = 0f;

            Effects.effect(drillEffect, entity.dominantItem.color,
            entity.x + Mathf.range(size), entity.y + Mathf.range(size));
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(isValid(other)){
                    return true;
                }
            }
            return false;
        }else{
            return isValid(tile);
        }
    }

    public int tier(){
        return tier;
    }

    public Item getDrop(Tile tile){
        return tile.drop();
    }

    public boolean isValid(Tile tile){
        if(tile == null) return false;
        Item drops = tile.drop();
        return drops != null && drops.hardness <= tier;
    }

    public static class DrillEntity extends TileEntity{
        float progress;
        int index;
        float warmup;
        float drillTime;
        float lastDrillSpeed;

        int dominantItems;
        Item dominantItem;
    }

}
