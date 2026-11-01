package mindustry.world.blocks.production;

import arc.*;
import arc.Graphics.Cursor;
import arc.Graphics.Cursor.SystemCursor;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class Drill extends Block{
    public float hardnessDrillMultiplier = 50f;

    protected final Seq<Item> returnItemArray = new Seq<>();

    protected final Seq<Item> tmpDrops = new Seq<>();

    /** Maximum tier of blocks this drill can mine. */
    public int tier;
    /** Base time to drill one ore, in frames. */
    public float drillTime = 300;
    /** How many times faster the drill will progress when boosted by liquid. */
    public float liquidBoostIntensity = 1.6f;
    /** Speed at which the drill speeds up. */
    public float warmupSpeed = 0.015f;
    /** Special exemption item that this drill can't mine. */
    public @Nullable Item blockedItem;
    /** Special exemption items that this drill can't mine. */
    public @Nullable Seq<Item> blockedItems;

    //return variables for countOre
    protected final ObjectIntMap<Item> returnOreCounts = new ObjectIntMap<>();
    protected @Nullable Item recommendedReturnItem;

    /** Whether to draw the item this drill is mining. */
    public boolean drawMineItem = true;
    /** Effect played when an item is produced. This is colored. */
    public Effect drillEffect = Fx.mine;
    /** Drill effect randomness. Block size by default. */
    public float drillEffectRnd = -1f;
    /** Chance of displaying the effect. Useful for extremely fast drills. */
    public float drillEffectChance = 0.02f;
    /** Speed the drill bit rotates at. */
    public float rotateSpeed = 2f;
    /** Effect randomly played while drilling. */
    public Effect updateEffect = Fx.pulverizeSmall;
    /** Chance the update effect will appear. */
    public float updateEffectChance = 0.02f;

    /** Multipliers of drill speed for each item. Defaults to 1. */
    public ObjectFloatMap<Item> drillMultipliers = new ObjectFloatMap<>();

    public boolean drawRim = false;
    public boolean drawSpinSprite = true;
    public Color heatColor = Color.valueOf("ff5512");
    public @Load("@-rim") TextureRegion rimRegion;
    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-top") TextureRegion topRegion;
    public @Load(value = "@-item", fallback = "drill-item-@size") TextureRegion itemRegion;

    public Drill(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.drills;
        hasLiquids = true;
        hasItems = true;
        ambientSound = Sounds.loopDrill;
        ambientSoundVolume = 0.019f;
        //drills work in space I guess
        envEnabled |= Env.space;
        flags = EnumSet.of(BlockFlag.drill);

        clearOnDoubleTap = true;
        configurable = true;

        config(Item.class, (DrillBuild tile, Item item) -> tile.setDrillingItem(item));
        configClear((DrillBuild tile) -> tile.setDrillingItem(tile.recommendedItem));
    }

    @Override
    public void init(){
        super.init();
        if(blockedItems == null && blockedItem != null){
            blockedItems = Seq.with(blockedItem);
        }
        if(drillEffectRnd < 0) drillEffectRnd = size;
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        if(!plan.worldContext) return;
        Tile tile = plan.tile();
        if(tile == null) return;

        countOre(tile);
        if(recommendedReturnItem == null || !drawMineItem) return;

        Draw.tint(recommendedReturnItem.color);
        Draw.rect(itemRegion, plan.drawx(), plan.drawy());
        Draw.color();
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("drillspeed", (DrillBuild e) ->
             new Bar(() -> Core.bundle.format("bar.drillspeed", Strings.fixed(e.lastDrillSpeed * 60 * e.timeScale(), 2)), () -> Pal.ammo, () -> e.warmup));
    }

    public Item getDrop(Tile tile){
        return tile.drop();
    }

    public void getDrops(Tile tile, Seq<Item> out){
        Item primary = getDrop(tile);
        if(primary != null) out.add(primary);
        Item floor = tile.floor().itemDrop;
        if(floor != null && floor != primary) out.add(floor);
    }


    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
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
        super.drawPlace(x, y, rotation, valid);

        Tile tile = world.tile(x, y);
        if(tile == null) return;

        countOre(tile);

        if (recommendedReturnItem != null){
            //TODO show all drillable ores here instead of only the recommended one?
            float width = drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / getDrillTime(recommendedReturnItem) * returnOreCounts.get(recommendedReturnItem), 2), x, y, valid);
            float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(recommendedReturnItem.fullIcon, dx, dy - 1, s, s);
            Draw.reset();
            Draw.rect(recommendedReturnItem.fullIcon, dx, dy, s, s);

            if(drawMineItem){
                Draw.color(recommendedReturnItem.color);
                Draw.rect(itemRegion, tile.worldx() + offset, tile.worldy() + offset);
                Draw.color();
            }
        }else{
            Tile to = tile.getLinkedTilesAs(this, tempTiles).find(t -> t.drop() != null && (t.drop().hardness > tier || (blockedItems != null && blockedItems.contains(t.drop()))));
            Item item = to == null ? null : to.drop();
            if(item != null){
                drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y, valid);
            }
        }
    }

    public float getDrillTime(Item item){
        return (drillTime + hardnessDrillMultiplier * item.hardness) / drillMultipliers.get(item, 1f);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.drillTier, StatValues.drillables(drillTime, hardnessDrillMultiplier, size * size, drillMultipliers, b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null &&
            f.itemDrop.hardness <= tier && (blockedItems == null || !blockedItems.contains(f.itemDrop)) && (indexer.isBlockPresent(f) || state.isMenu())));

        stats.add(Stat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);

        if(liquidBoostIntensity != 1 && findConsumer(f -> f instanceof ConsumeLiquidBase && f.booster) instanceof ConsumeLiquidBase consBase){
            stats.remove(Stat.booster);
            stats.add(Stat.booster,
                StatValues.speedBoosters("{0}" + StatUnit.timesSpeed.localized(),
                consBase.amount,
                liquidBoostIntensity * liquidBoostIntensity, false, consBase::consumes)
            );
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, rotatorRegion, topRegion};
    }

    protected void countOre(Tile tile){
        recommendedReturnItem = null;

        returnOreCounts.clear();
        returnItemArray.clear();

        for (Tile other : tile.getLinkedTilesAs(this, tempTiles)) {
            if (canMine(other)) {
                tmpDrops.clear();
                getDrops(other, tmpDrops);
                for (Item item : tmpDrops) {
                    if (canMine(item))
                        returnOreCounts.increment(item, 0, 1);
                }
            }
        }

        for(Item item : returnOreCounts.keys()){
            returnItemArray.add(item);
        }

        returnItemArray.sort((item1, item2) -> {
            int type = Boolean.compare(!item1.lowPriority, !item2.lowPriority);
            if(type != 0) return type;
            int amounts = Integer.compare(returnOreCounts.get(item1, 0), returnOreCounts.get(item2, 0));
            if(amounts != 0) return amounts;
            return Integer.compare(item1.id, item2.id);
        });

        if(returnItemArray.size == 0){
            return;
        }

        recommendedReturnItem = returnItemArray.peek();
    }

    public boolean canMine(Tile tile){
        tmpDrops.clear();
        getDrops(tile, tmpDrops);
        boolean canMineAny = false;
        for (Item drop : tmpDrops) {
            if (canMine(drop)) {
                canMineAny = true;
            }
        }
        if (!canMineAny) return false;
        if (tile == null || tile.block().isStatic()) return false;
        return true;
    }
    
    public boolean canMine(Item item){
        return item != null && item.hardness <= tier && (blockedItems == null || !blockedItems.contains(item));
    }

    public class DrillBuild extends Building{
        public float progress;
        public float warmup;
        public float timeDrilled;
        public float lastDrillSpeed;

        public Item drillingItem;
        public int drillingItemCount;
        public Item recommendedItem;
        public final ObjectIntMap<Item> drillableOreCounts = new ObjectIntMap<>();
        public Seq<Item> drillableOres;

        @Override
        public boolean shouldConsume(){
            return items.total() < itemCapacity && enabled && drillingItem != null;
        }

        @Override
        public boolean shouldAmbientSound(){
            return efficiency > 0.01f && items.total() < itemCapacity;
        }

        @Override
        public float ambientVolume(){
            return efficiency * (size * size) / 4f;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            drawItemSelection(drillingItem);
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(Drill.this, table, drillableOres, () -> drillingItem, this::configure,
                    selectionRows, selectionColumns);
        }
        
        @Override
        public Item config(){
            return drillingItem;
        }
        
        @Override
        public boolean shouldShowConfigure(Player player){
            return drillableOres != null && drillableOres.size > 1;
        }

        @Override
        public Cursor getCursor(){
            //the default shows a hand cursor whenever block.configurable is true
            return shouldShowConfigure(player) ? super.getCursor() : SystemCursor.arrow;
        }

        @Override
        public void pickedUp(){
            // keep drillingItem in case we drop it back on the ground
            recommendedItem = null;
            drillingItemCount = 0;
            drillableOreCounts.clear();
            drillableOres.clear();
        }
        
        public void updateOre(){
            countOre(tile);
            drillableOreCounts.clear();
            drillableOreCounts.putAll(returnOreCounts);
            drillableOres = returnItemArray.copy().reverse();
            recommendedItem = recommendedReturnItem;

            if(drillingItem == null || !returnOreCounts.containsKey(drillingItem)){
                setDrillingItem(recommendedItem);
            } else {
                refreshItemCount();
            }
        }

        private void refreshItemCount(){
            drillingItemCount = drillableOreCounts.get(drillingItem);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            updateOre();
        }
        
        public void setDrillingItem(@Nullable Item newItem){
            if(newItem == null){
                drillingItem = null;
                drillingItemCount = 0;
            }else if (drillableOreCounts.containsKey(newItem)){
                drillingItem = newItem;
                refreshItemCount();
            }
        }
        
        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.firstItem) return drillingItem;
            return super.senseObject(sensor);
        }

        @Override
        public void updateTile(){
            if(timer(timerDump, dumpTime / timeScale)){
                dump(drillingItem != null && items.has(drillingItem) ? drillingItem : null);
            }

            if(drillingItem == null){
                return;
            }

            timeDrilled += warmup * delta();

            float delay = getDrillTime(drillingItem);

            if(items.total() < itemCapacity && drillingItemCount > 0 && efficiency > 0){
                float speed = Mathf.lerp(1f, liquidBoostIntensity, optionalEfficiency) * efficiency;

                lastDrillSpeed = (speed * drillingItemCount * warmup) / delay;
                warmup = Mathf.approachDelta(warmup, speed, warmupSpeed);
                progress += delta() * drillingItemCount * speed * warmup;

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }else{
                lastDrillSpeed = 0f;
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
                return;
            }

            if(drillingItemCount > 0 && progress >= delay && items.total() < itemCapacity){
                int amount = (int)(progress / delay);
                for(int i = 0; i < amount; i++){
                    offload(drillingItem);
                }

                progress %= delay;

                if(wasVisible && Mathf.chanceDelta(drillEffectChance * warmup)) drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), drillingItem.color);
            }
        }

        @Override
        public float progress(){
            return drillingItem == null ? 0f : Mathf.clamp(progress / getDrillTime(drillingItem));
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress && drillingItem != null) return progress;
            return super.sense(sensor);
        }

        @Override
        public void drawCracks(){}

        public void drawDefaultCracks(){
            super.drawCracks();
        }

        @Override
        public void draw(){
            float s = 0.3f;
            float ts = 0.6f;

            Draw.rect(region, x, y);
            Draw.z(Layer.blockCracks);
            drawDefaultCracks();

            Draw.z(Layer.blockAfterCracks);
            if(drawRim){
                Draw.color(heatColor);
                Draw.alpha(warmup * ts * (1f - s + Mathf.absin(Time.time, 3f, s)));
                Draw.blend(Blending.additive);
                Draw.rect(rimRegion, x, y);
                Draw.blend();
                Draw.color();
            }

            if(drawSpinSprite){
                Drawf.spinSprite(rotatorRegion, x, y, timeDrilled * rotateSpeed);
            }else{
                Draw.rect(rotatorRegion, x, y, timeDrilled * rotateSpeed);
            }

            Draw.rect(topRegion, x, y);

            if(drillingItem != null && drawMineItem){
                Draw.color(drillingItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(warmup);
            write.s(drillingItem == null ? -1 : drillingItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if (revision >= 1) {
                progress = read.f();
                warmup = read.f();
            }
            
            // if it's below revision 2 drillingItem will be null then set to recommended ore by onProximityUpdate
            if (revision >= 2) {
                drillingItem = content.item(read.s());
                updateOre();
            }
        }
    }
}

