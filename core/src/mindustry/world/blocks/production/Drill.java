package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
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
    protected final Seq<Item> itemArray = new Seq<>();

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
    public @Load("@-rim") TextureRegion rimRegion;
    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-top") TextureRegion topRegion;

    public Drill(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.drills;
        hasLiquids = true;
        liquidCapacity = 5f;
        hasItems = true;
        idleSound = Sounds.drill;
        idleSoundVolume = 0.003f;
    }

    @Override
    public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
        if(!req.worldContext) return;
        Tile tile = req.tile();
        if(tile == null) return;

        countOre(req.tile());
        if(returnItem == null) return;

        Draw.color(returnItem.color);
        Draw.rect("drill-top", req.drawx(), req.drawy());
        Draw.color();
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("drillspeed", (DrillEntity e) ->
             new Bar(() -> Core.bundle.format("bar.drillspeed", Strings.fixed(e.lastDrillSpeed * 60 * e.timeScale(), 2)), () -> Pal.ammo, () -> e.warmup));
    }

    public Item getDrop(Tile tile){
        return tile.drop();
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team){
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(canMine(other)){
                    return true;
                }
            }
            return false;
        }else{
            return canMine(tile);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);
        if(tile == null) return;

        countOre(tile);

        if(returnItem != null){
            float width = drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / (drillTime + hardnessDrillMultiplier * returnItem.hardness) * returnCount, 2), x, y, valid);
            float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5;
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
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.drillTier, table -> {
            Seq<Block> list = content.blocks().select(b -> b.isFloor() && b.asFloor().itemDrop != null && b.asFloor().itemDrop.hardness <= tier);

            table.table(l -> {
                l.left();

                for(int i = 0; i < list.size; i++){
                    Block item = list.get(i);

                    l.image(item.icon(Cicon.small)).size(8 * 3).padRight(2).padLeft(2).padTop(3).padBottom(3);
                    l.add(item.localizedName).left().padLeft(1).padRight(4);
                    if(i % 5 == 4){
                        l.row();
                    }
                }
            });


        });

        stats.add(BlockStat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);
        if(liquidBoostIntensity != 1){
            stats.add(BlockStat.boostEffect, liquidBoostIntensity * liquidBoostIntensity, StatUnit.timesSpeed);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, rotatorRegion, topRegion};
    }

    void countOre(Tile tile){
        returnItem = null;
        returnCount = 0;

        oreCount.clear();
        itemArray.clear();

        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            if(canMine(other)){
                oreCount.increment(getDrop(other), 0, 1);
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

    public boolean canMine(Tile tile){
        if(tile == null) return false;
        Item drops = tile.drop();
        return drops != null && drops.hardness <= tier;
    }

    public class DrillEntity extends Building{
        public float progress;
        public int index;
        public float warmup;
        public float timeDrilled;
        public float lastDrillSpeed;

        public int dominantItems;
        public Item dominantItem;

        @Override
        public boolean shouldConsume(){
            return items.total() < itemCapacity;
        }

        @Override
        public boolean shouldIdleSound(){
            return efficiency() > 0.01f;
        }

        @Override
        public void drawSelect(){
            if(dominantItem != null){
                float dx = x - size * tilesize/2f, dy = y + size * tilesize/2f;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(dominantItem.icon(Cicon.small), dx, dy - 1);
                Draw.reset();
                Draw.rect(dominantItem.icon(Cicon.small), dx, dy);
            }
        }

        @Override
        public void onProximityUpdate(){
            countOre(tile);
            dominantItem = returnItem;
            dominantItems = returnCount;
        }

        @Override
        public void updateTile(){
            if(dominantItem == null){
                return;
            }

            if(timer(timerDump, dumpTime)){
                dump(dominantItem);
            }

            timeDrilled += warmup * delta();

            if(items.total() < itemCapacity && dominantItems > 0 && consValid()){

                float speed = 1f;

                if(cons.optionalValid()){
                    speed = liquidBoostIntensity;
                }

                speed *= efficiency(); // Drill slower when not at full power

                lastDrillSpeed = (speed * dominantItems * warmup) / (drillTime + hardnessDrillMultiplier * dominantItem.hardness);
                warmup = Mathf.lerpDelta(warmup, speed, warmupSpeed);
                progress += delta() * dominantItems * speed * warmup;

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(getX() + Mathf.range(size * 2f), getY() + Mathf.range(size * 2f));
            }else{
                lastDrillSpeed = 0f;
                warmup = Mathf.lerpDelta(warmup, 0f, warmupSpeed);
                return;
            }

            float delay = drillTime + hardnessDrillMultiplier * dominantItem.hardness;

            if(dominantItems > 0 && progress >= delay && items.total() < itemCapacity){
                offload(dominantItem);

                index ++;
                progress %= delay;

                drillEffect.at(getX() + Mathf.range(size), getY() + Mathf.range(size), dominantItem.color);
            }
        }

        @Override
        public void drawCracks(){}

        @Override
        public void draw(){
            float s = 0.3f;
            float ts = 0.6f;

            Draw.rect(region, x, y);
            super.drawCracks();

            if(drawRim){
                Draw.color(heatColor);
                Draw.alpha(warmup * ts * (1f - s + Mathf.absin(Time.time(), 3f, s)));
                Draw.blend(Blending.additive);
                Draw.rect(rimRegion, x, y);
                Draw.blend();
                Draw.color();
            }

            Draw.rect(rotatorRegion, x, y, timeDrilled * rotateSpeed);

            Draw.rect(topRegion, x, y);

            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect("drill-top", x, y);
                Draw.color();
            }
        }
    }

}
