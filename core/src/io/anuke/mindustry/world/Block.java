package io.anuke.mindustry.world;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.Graphics.*;
import io.anuke.arc.Graphics.Cursor.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.power.*;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.*;
import io.anuke.mindustry.world.meta.values.*;

import java.util.*;

import static io.anuke.mindustry.Vars.*;

public class Block extends BlockStorage{
    public static final int crackRegions = 8, maxCrackSize = 5;

    /** whether this block has a tile entity that updates */
    public boolean update;
    /** whether this block has health and can be destroyed */
    public boolean destructible;
    /** whether unloaders work on this block*/
    public boolean unloadable = true;
    /** whether this is solid */
    public boolean solid;
    /** whether this block CAN be solid. */
    public boolean solidifes;
    /** whether this is rotateable */
    public boolean rotate;
    /** whether you can break this with rightclick */
    public boolean breakable;
    /** whether this floor can be placed on. */
    public boolean placeableOn = true;
    /** whether this block has insulating properties. */
    public boolean insulated = false;
    /** tile entity health */
    public int health = -1;
    /** base block explosiveness */
    public float baseExplosiveness = 0f;
    /** whether this block can be placed on edges of liquids. */
    public boolean floating = false;
    /** multiblock size */
    public int size = 1;
    /** Whether to draw this block in the expanded draw range. */
    public boolean expanded = false;
    /** Max of timers used. */
    public int timers = 0;
    /** Cache layer. Only used for 'cached' rendering. */
    public CacheLayer cacheLayer = CacheLayer.normal;
    /** Special flag; if false, floor will be drawn under this block even if it is cached. */
    public boolean fillsTile = true;
    /** Layer to draw extra stuff on. */
    public Layer layer = null;
    /** Extra layer to draw extra extra stuff on. */
    public Layer layer2 = null;
    /** whether this block can be replaced in all cases */
    public boolean alwaysReplace = false;
    /** The block group. Unless {@link #canReplace} is overriden, blocks in the same group can replace each other. */
    public BlockGroup group = BlockGroup.none;
    /** List of block flags. Used for AI indexing. */
    public EnumSet<BlockFlag> flags = EnumSet.of();
    /** Targeting priority of this block, as seen by enemies.*/
    public TargetPriority priority = TargetPriority.base;
    /** Whether the block can be tapped and selected to configure. */
    public boolean configurable;
    /** Whether this block consumes touchDown events when tapped. */
    public boolean consumesTap;
    /** Whether the config is positional and needs to be shifted. */
    public boolean posConfig;
    /** Whether this block uses conveyor-type placement mode.*/
    public boolean conveyorPlacement;
    /**
     * The color of this block when displayed on the minimap or map preview.
     * Do not set manually! This is overriden when loading for most blocks.
     */
    public Color color = new Color(0, 0, 0, 1);
    /** Whether units target this block. */
    public boolean targetable = true;
    /** Whether the overdrive core has any effect on this block. */
    public boolean canOverdrive = true;
    /** Outlined icon color.*/
    public Color outlineColor = Color.valueOf("404049");
    /** Whether the icon region has an outline added. */
    public boolean outlineIcon = false;
    /** Whether this block has a shadow under it. */
    public boolean hasShadow = true;
    /** Sounds made when this block breaks.*/
    public Sound breakSound = Sounds.boom;

    /** The sound that this block makes while active. One sound loop. Do not overuse.*/
    public Sound activeSound = Sounds.none;
    /** Active sound base volume. */
    public float activeSoundVolume = 0.5f;

    /** The sound that this block makes while idle. Uses one sound loop for all blocks.*/
    public Sound idleSound = Sounds.none;
    /** Idle sound base volume. */
    public float idleSoundVolume = 0.5f;

    /** Cost of constructing this block. */
    public ItemStack[] requirements = {};
    /** Category in place menu. */
    public Category category = Category.distribution;
    /** Cost of building this block; do not modify directly! */
    public float buildCost;
    /** Whether this block is visible and can currently be built. */
    public BuildVisibility buildVisibility = BuildVisibility.hidden;
    /** Multiplier for speed of building this block. */
    public float buildCostMultiplier = 1f;
    /** Whether this block has instant transfer.*/
    public boolean instantTransfer = false;
    public boolean alwaysUnlocked = false;

    protected TextureRegion[] cacheRegions = {};
    protected Array<String> cacheRegionStrings = new Array<>();

    protected Array<Tile> tempTiles = new Array<>();
    protected TextureRegion[] generatedIcons;
    protected TextureRegion[] variantRegions, editorVariantRegions;
    protected TextureRegion region, editorIcon;

    protected static TextureRegion[][] cracks;

    /** Dump timer ID.*/
    protected final int timerDump = timers++;
    /** How often to try dumping items in ticks, e.g. 5 = 12 times/sec*/
    protected final int dumpTime = 5;

    public Block(String name){
        super(name);
        this.description = Core.bundle.getOrNull("block." + name + ".description");
        this.solid = false;
    }

    public boolean canBreak(Tile tile){
        return true;
    }

    public boolean isBuildable(){
        return buildVisibility != BuildVisibility.hidden && buildVisibility != BuildVisibility.debugOnly;
    }

    public boolean isStatic(){
        return cacheLayer == CacheLayer.walls;
    }

    public void onProximityRemoved(Tile tile){
        if(tile.entity.power != null){
            tile.block().powerGraphRemoved(tile);
        }
    }

    public void onProximityAdded(Tile tile){
        if(tile.block().hasPower) tile.block().updatePowerGraph(tile);
    }

    protected void updatePowerGraph(Tile tile){
        TileEntity entity = tile.entity();

        for(Tile other : getPowerConnections(tile, tempTiles)){
            if(other.entity.power != null){
                other.entity.power.graph.add(entity.power.graph);
            }
        }
    }

    protected void powerGraphRemoved(Tile tile){
        if(tile.entity == null || tile.entity.power == null){
            return;
        }

        tile.entity.power.graph.remove(tile);
        for(int i = 0; i < tile.entity.power.links.size; i++){
            Tile other = world.tile(tile.entity.power.links.get(i));
            if(other != null && other.entity != null && other.entity.power != null){
                other.entity.power.links.removeValue(tile.pos());
            }
        }
    }

    public Array<Tile> getPowerConnections(Tile tile, Array<Tile> out){
        out.clear();
        if(tile == null || tile.entity == null || tile.entity.power == null) return out;

        for(Tile other : tile.entity.proximity()){
            if(other != null && other.entity != null && other.entity.power != null && !(consumesPower && other.block().consumesPower && !outputsPower && !other.block().outputsPower)
            && !tile.entity.power.links.contains(other.pos())){
                out.add(other);
            }
        }

        for(int i = 0; i < tile.entity.power.links.size; i++){
            Tile link = world.tile(tile.entity.power.links.get(i));
            if(link != null && link.entity != null && link.entity.power != null) out.add(link);
        }
        return out;
    }

    protected float getProgressIncrease(TileEntity entity, float baseTime){
        float progressIncrease = 1f / baseTime * entity.delta();
        if(hasPower){
            progressIncrease *= entity.power.satisfaction; // Reduced increase in case of low power
        }
        return progressIncrease;
    }

    /** @return whether this block should play its active sound.*/
    public boolean shouldActiveSound(Tile tile){
        return false;
    }

    /** @return whether this block should play its idle sound.*/
    public boolean shouldIdleSound(Tile tile){
        return shouldConsume(tile);
    }

    public void drawLayer(Tile tile){
    }

    public void drawLayer2(Tile tile){
    }

    public void drawCracks(Tile tile){
        if(!tile.entity.damaged() || size > maxCrackSize) return;
        int id = tile.pos();
        TextureRegion region = cracks[size - 1][Mathf.clamp((int)((1f - tile.entity.healthf()) * crackRegions), 0, crackRegions-1)];
        Draw.colorl(0.2f, 0.1f + (1f - tile.entity.healthf())* 0.6f);
        Draw.rect(region, tile.drawx(), tile.drawy(), (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(Tile tile){
    }

    /** Drawn when you are placing a block. */
    public void drawPlace(int x, int y, int rotation, boolean valid){
    }

    public float drawPlaceText(String text, int x, int y, boolean valid){
        if(renderer.pixelator.enabled()) return 0;

        Color color = valid ? Pal.accent : Pal.remove;
        BitmapFont font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f / 4f / Scl.scl(1f));
        layout.setText(font, text);

        float width = layout.width;

        font.setColor(color);
        float dx = x * tilesize + offset(), dy = y * tilesize + offset() + size * tilesize / 2f + 3;
        font.draw(text, dx, dy + layout.height + 1, Align.center);
        dy -= 1f;
        Lines.stroke(2f, Color.darkGray);
        Lines.line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 1.5f, dy);
        Lines.stroke(1f, color);
        Lines.line(dx - layout.width / 2f - 2f, dy, dx + layout.width / 2f + 1.5f, dy);

        font.setUseIntegerPositions(ints);
        font.setColor(Color.white);
        font.getData().setScale(1f);
        Draw.reset();
        Pools.free(layout);
        return width;
    }

    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), rotate ? tile.rotation() * 90 : 0);
    }

    public void drawTeam(Tile tile){
        Draw.color(tile.getTeam().color);
        Draw.rect("block-border", tile.drawx() - size * tilesize / 2f + 4, tile.drawy() - size * tilesize / 2f + 4);
        Draw.color();
    }

    /** Called after the block is placed by this client. */
    @CallSuper
    public void playerPlaced(Tile tile){

    }

    /** Called after the block is placed by anyone. */
    @CallSuper
    public void placed(Tile tile){
        if(net.client()) return;

        if((consumesPower && !outputsPower) || (!consumesPower && outputsPower)){
            int range = 10;
            tempTiles.clear();
            Geometry.circle(tile.x, tile.y, range, (x, y) -> {
                Tile other = world.ltile(x, y);
                if(other != null && other.block instanceof PowerNode && ((PowerNode)other.block).linkValid(other, tile) && !PowerNode.insulated(other, tile) && !other.entity.proximity().contains(tile) &&
                !(outputsPower && tile.entity.proximity().contains(p -> p.entity != null && p.entity.power != null && p.entity.power.graph == other.entity.power.graph))){
                    tempTiles.add(other);
                }
            });
            tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
            if(!tempTiles.isEmpty()){
                Tile toLink = tempTiles.first();
                if(!toLink.entity.power.links.contains(tile.pos())){
                    toLink.configureAny(tile.pos());
                }
            }
        }
    }

    public void removed(Tile tile){
    }

    /** Called every frame a unit is on this tile. */
    public void unitOn(Tile tile, Unit unit){
    }

    /** Called when a unit that spawned at this tile is removed. */
    public void unitRemoved(Tile tile, Unit unit){
    }

    /** Returns whether ot not this block can be place on the specified tile. */
    public boolean canPlaceOn(Tile tile){
        return true;
    }

    /** Call when some content is produced. This unlocks the content if it is applicable. */
    public void useContent(Tile tile, UnlockableContent content){
        //only unlocks content in zones
        if(!headless && tile.getTeam() == player.getTeam() && world.isZone()){
            logic.handleContent(content);
        }
    }

    public float sumAttribute(Attribute attr, int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return 0;
        float sum = 0;
        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            sum += other.floor().attributes.get(attr);
        }
        return sum;
    }

    @Override
    public String localizedName(){
        return localizedName;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayBlock(table, this);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.block;
    }

    /** Called after all blocks are created. */
    @Override
    @CallSuper
    public void init(){
        //initialize default health based on size
        if(health == -1){
            health = size * size * 40;
        }

        buildCost = 0f;
        for(ItemStack stack : requirements){
            buildCost += stack.amount * stack.item.cost;
        }
        buildCost *= buildCostMultiplier;

        if(consumes.has(ConsumeType.power)) hasPower = true;
        if(consumes.has(ConsumeType.item)) hasItems = true;
        if(consumes.has(ConsumeType.liquid)) hasLiquids = true;

        setStats();
        setBars();

        consumes.init();

        if(!outputsPower && consumes.hasPower() && consumes.getPower().buffered){
            throw new IllegalArgumentException("Consumer using buffered power: " + name);
        }
    }

    @Override
    public void load(){
        region = Core.atlas.find(name);

        cacheRegions = new TextureRegion[cacheRegionStrings.size];
        for(int i = 0; i < cacheRegions.length; i++){
            cacheRegions[i] = Core.atlas.find(cacheRegionStrings.get(i));
        }

        if(cracks == null || (cracks[0][0].getTexture() != null && cracks[0][0].getTexture().isDisposed())){
            cracks = new TextureRegion[maxCrackSize][crackRegions];
            for(int size = 1; size <= maxCrackSize; size++){
                for(int i = 0; i < crackRegions; i++){
                    cracks[size - 1][i] = Core.atlas.find("cracks-" + size + "-" + i);
                }
            }
        }
    }

    /** Adds a region by name to be loaded, with the final name "{name}-suffix". Returns an ID to looks this region up by in {@link #reg(int)}. */
    protected int reg(String suffix){
        cacheRegionStrings.add(name + suffix);
        return cacheRegionStrings.size - 1;
    }

    /** Returns an internally cached region by ID. */
    protected TextureRegion reg(int id){
        return cacheRegions[id];
    }

    /** Called when the block is tapped. */
    public void tapped(Tile tile, Player player){

    }

    /** Called when arbitrary configuration is applied to a tile. */
    public void configured(Tile tile, @Nullable Player player, int value){

    }

    /** Returns whether or not a hand cursor should be shown over this block. */
    public Cursor getCursor(Tile tile){
        return configurable ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when this block is tapped to build a UI on the table.
     * {@link #configurable} must return true for this to be called.
     */
    public void buildTable(Tile tile, Table table){
    }

    /** Update table alignment after configuring.*/
    public void updateTableAlign(Tile tile, Table table){
        Vector2 pos = Core.input.mouseScreen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
        table.setPosition(pos.x, pos.y, Align.top);
    }

    /**
     * Called when another tile is tapped while this block is selected.
     * Returns whether or not this block should be deselected.
     */
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        return tile != other;
    }

    /** Returns whether this config menu should show when the specified player taps it. */
    public boolean shouldShowConfigure(Tile tile, Player player){
        return true;
    }

    /** Whether this configuration should be hidden now. Called every frame the config is open. */
    public boolean shouldHideConfigure(Tile tile, Player player){
        return false;
    }

    public boolean synthetic(){
        return update || destructible;
    }

    public void drawConfigure(Tile tile){
        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 1f);
        Draw.reset();
    }

    public void setStats(){
        stats.add(BlockStat.size, "{0}x{0}", size);
        stats.add(BlockStat.health, health, StatUnit.none);
        if(isBuildable()){
            stats.add(BlockStat.buildTime, buildCost / 60, StatUnit.seconds);
            stats.add(BlockStat.buildCost, new ItemListValue(false, requirements));
        }

        consumes.display(stats);

        // Note: Power stats are added by the consumers.
        if(hasLiquids) stats.add(BlockStat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if(hasItems) stats.add(BlockStat.itemCapacity, itemCapacity, StatUnit.items);
    }

    public void setBars(){
        bars.add("health", entity -> new Bar("blocks.health", Pal.health, entity::healthf).blink(Color.white));

        if(hasLiquids){
            Func<TileEntity, Liquid> current;
            if(consumes.has(ConsumeType.liquid) && consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid){
                Liquid liquid = consumes.<ConsumeLiquid>get(ConsumeType.liquid).liquid;
                current = entity -> liquid;
            }else{
                current = entity -> entity.liquids.current();
            }
            bars.add("liquid", entity -> new Bar(() -> entity.liquids.get(current.get(entity)) <= 0.001f ? Core.bundle.get("bar.liquid") : current.get(entity).localizedName(),
                    () -> current.get(entity).barColor(), () -> entity.liquids.get(current.get(entity)) / liquidCapacity));
        }

        if(hasPower && consumes.hasPower()){
            ConsumePower cons = consumes.getPower();
            boolean buffered = cons.buffered;
            float capacity = cons.capacity;

            bars.add("power", entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.satisfaction * capacity) ? "<ERROR>" : (int)(entity.power.satisfaction * capacity)) :
                Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.satisfaction));
        }

        if(hasItems && configurable){
            bars.add("items", entity -> new Bar(() -> Core.bundle.format("bar.items", entity.items.total()), () -> Pal.items, () -> (float)entity.items.total() / itemCapacity));
        }
    }

    public Tile linked(Tile tile){
        return tile;
    }

    public boolean isSolidFor(Tile tile){
        return false;
    }

    public boolean canReplace(Block other){
        return (other != this || rotate) && this.group != BlockGroup.none && other.group == this.group;
    }

    /** @return a possible replacement for this block when placed in a line by the player. */
    public Block getReplacement(BuildRequest req, Array<BuildRequest> requests){
        return this;
    }

    public float handleDamage(Tile tile, float amount){
        return amount;
    }

    public void handleBulletHit(TileEntity entity, Bullet bullet){
        entity.damage(bullet.damage());
    }

    public void update(Tile tile){
    }

    public boolean isAccessible(){
        return (hasItems && itemCapacity > 0);
    }

    /** Called when the block is destroyed. */
    public void onDestroyed(Tile tile){
        float x = tile.worldx(), y = tile.worldy();
        float explosiveness = baseExplosiveness;
        float flammability = 0f;
        float power = 0f;

        if(hasItems){
            for(Item item : content.items()){
                int amount = tile.entity.items.get(item);
                explosiveness += item.explosiveness * amount;
                flammability += item.flammability * amount;
            }
        }

        if(hasLiquids){
            flammability += tile.entity.liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += tile.entity.liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(consumes.hasPower() && consumes.getPower().buffered){
            power += tile.entity.power.satisfaction * consumes.getPower().capacity;
        }

        if(hasLiquids){

            tile.entity.liquids.forEach((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Time.run(i / 2f, () -> {
                        Tile other = world.tile(tile.x + Mathf.range(size / 2), tile.y + Mathf.range(size / 2));
                        if(other != null){
                            Puddle.deposit(other, liquid, splash);
                        }
                    });
                }
            });
        }

        Damage.dynamicExplosion(x, y, flammability, explosiveness * 3.5f, power, tilesize * size / 2f, Pal.darkFlame);
        if(!tile.floor().solid && !tile.floor().isLiquid){
            RubbleDecal.create(tile.drawx(), tile.drawy(), size);
        }
    }

    /**
     * Returns the flammability of the tile. Used for fire calculations.
     * Takes flammability of floor liquid into account.
     */
    public float getFlammability(Tile tile){
        if(!hasItems || tile.entity == null){
            if(tile.floor().isLiquid && !solid){
                return tile.floor().liquidDrop.flammability;
            }
            return 0;
        }else{
            float result = tile.entity.items.sum((item, amount) -> item.flammability * amount);

            if(hasLiquids){
                result += tile.entity.liquids.sum((liquid, amount) -> liquid.flammability * amount / 3f);
            }

            return result;
        }
    }

    public String getDisplayName(Tile tile){
        return localizedName;
    }

    public TextureRegion getDisplayIcon(Tile tile){
        return icon(Cicon.medium);
    }

    public void display(Tile tile, Table table){
        TileEntity entity = tile.entity;

        if(entity != null){
            table.table(bars -> {
                bars.defaults().growX().height(18f).pad(4);

                displayBars(tile, bars);
            }).growX();
            table.row();
            table.table(ctable -> {
                displayConsumption(tile, ctable);
            }).growX();

            table.marginBottom(-5);
        }
    }

    public void displayConsumption(Tile tile, Table table){
        table.left();
        for(Consume cons : consumes.all()){
            if(cons.isOptional() && cons.isBoost()) continue;
            cons.build(tile, table);
        }
    }

    public void displayBars(Tile tile, Table table){
        for(Func<TileEntity, Bar> bar : bars.list()){
            table.add(bar.get(tile.entity)).growX();
            table.row();
        }
    }

    public void drawRequest(BuildRequest req, Eachable<BuildRequest> list, boolean valid){
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime(), 6f, 0.28f));
        Draw.alpha(1f);
        drawRequestRegion(req, list);
        Draw.reset();
    }

    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        TextureRegion reg = icon(Cicon.full);
        Draw.rect(icon(Cicon.full), req.drawx(), req.drawy(),
            reg.getWidth() * req.animScale * Draw.scl,
            reg.getHeight() * req.animScale * Draw.scl,
            !rotate ? 0 : req.rotation * 90);

        if(req.hasConfig){
            drawRequestConfig(req, list);
        }
    }

    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){

    }

    public void drawRequestConfigCenter(BuildRequest req, Content content, String region){
        Color color = content instanceof Item ? ((Item)content).color : content instanceof Liquid ? ((Liquid)content).color : null;
        if(color == null) return;

        Draw.color(color);
        Draw.scl *= req.animScale;
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.scl /= req.animScale;
        Draw.color();
    }

    /** @return a custom minimap color for this tile, or 0 to use default colors. */
    public int minimapColor(Tile tile){
        return 0;
    }

    public void drawRequestConfigTop(BuildRequest req, Eachable<BuildRequest> list){

    }

    @Override
    public void createIcons(PixmapPacker packer, PixmapPacker editor){
        super.createIcons(packer, editor);

        editor.pack(name + "-icon-editor", Core.atlas.getPixmap((AtlasRegion)icon(Cicon.full)).crop());

        if(!synthetic()){
            PixmapRegion image = Core.atlas.getPixmap((AtlasRegion)icon(Cicon.full));
            color.set(image.getPixel(image.width/2, image.height/2));
        }

        getGeneratedIcons();

        Pixmap last = null;

        if(outlineIcon){
            final int radius = 4;
            PixmapRegion region = Core.atlas.getPixmap(getGeneratedIcons()[getGeneratedIcons().length-1]);
            Pixmap out = new Pixmap(region.width, region.height);
            Color color = new Color();
            for(int x = 0; x < region.width; x++){
                for(int y = 0; y < region.height; y++){

                    region.getPixel(x, y, color);
                    out.draw(x, y, color);
                    if(color.a < 1f){
                        boolean found = false;
                        outer:
                        for(int rx = -radius; rx <= radius; rx++){
                            for(int ry = -radius; ry <= radius; ry++){
                                if(Structs.inBounds(rx + x, ry + y, region.width, region.height) && Mathf.dst2(rx, ry) <= radius*radius && color.set(region.getPixel(rx + x, ry + y)).a > 0.01f){
                                    found = true;
                                    break outer;
                                }
                            }
                        }
                        if(found){
                            out.draw(x, y, outlineColor);
                        }
                    }
                }
            }
            last = out;

            packer.pack(name, out);
        }

        if(generatedIcons.length > 1){
            Pixmap base = Core.atlas.getPixmap(generatedIcons[0]).crop();
            for(int i = 1; i < generatedIcons.length; i++){
                if(i == generatedIcons.length - 1 && last != null){
                    base.drawPixmap(last);
                }else{
                    base.draw(Core.atlas.getPixmap(generatedIcons[i]));
                }
            }
            packer.pack("block-" + name + "-full", base);
            generatedIcons = null;
            Arrays.fill(cicons, null);
        }
    }

    /** Never use outside of the editor! */
    public TextureRegion editorIcon(){
        if(editorIcon == null) editorIcon = Core.atlas.find(name + "-icon-editor");
        return editorIcon;
    }

    /** Never use outside of the editor! */
    public TextureRegion[] editorVariantRegions(){
        if(editorVariantRegions == null){
            variantRegions();
            editorVariantRegions = new TextureRegion[variantRegions.length];
            for(int i = 0; i < variantRegions.length; i++){
                AtlasRegion region = (AtlasRegion)variantRegions[i];
                editorVariantRegions[i] = Core.atlas.find("editor-" + region.name);
            }
        }
        return editorVariantRegions;
    }

    protected TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name)};
    }

    public TextureRegion[] getGeneratedIcons(){
        if(generatedIcons == null){
            generatedIcons = generateIcons();
        }
        return generatedIcons;
    }

    public TextureRegion[] variantRegions(){
        if(variantRegions == null){
            variantRegions = new TextureRegion[]{icon(Cicon.full)};
        }
        return variantRegions;
    }

    public boolean hasEntity(){
        return destructible || update;
    }

    public TileEntity newEntity(){
        return new TileEntity();
    }

    /** Offset for placing and drawing multiblocks. */
    public float offset(){
        return ((size + 1) % 2) * tilesize / 2f;
    }

    public Rectangle bounds(int x, int y, Rectangle rect){
        return rect.setSize(size * tilesize).setCenter(x * tilesize + offset(), y * tilesize + offset());
    }

    public boolean isMultiblock(){
        return size > 1;
    }

    public boolean isVisible(){
        return buildVisibility.visible() && !isHidden();
    }

    public boolean isFloor(){
        return this instanceof Floor;
    }

    public boolean isOverlay(){
        return this instanceof OverlayFloor;
    }

    public Floor asFloor(){
        return (Floor)this;
    }

    @Override
    public boolean isHidden(){
        return !buildVisibility.visible();
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    protected void requirements(Category cat, ItemStack[] stacks, boolean unlocked){
        requirements(cat, BuildVisibility.shown, stacks);
        this.alwaysUnlocked = unlocked;
    }

    protected void requirements(Category cat, ItemStack[] stacks){
        requirements(cat, BuildVisibility.shown, stacks);
    }

    /** Sets up requirements. Use only this method to set up requirements. */
    protected void requirements(Category cat, BuildVisibility visible, ItemStack[] stacks){
        this.category = cat;
        this.requirements = stacks;
        this.buildVisibility = visible;

        Arrays.sort(requirements, Structs.comparingInt(i -> i.item.id));
    }

}
