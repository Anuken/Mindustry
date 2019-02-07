package io.anuke.mindustry.world;

import io.anuke.arc.Core;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.function.BooleanProvider;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.effect.RubbleDecal;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.ui.ContentDisplay;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class Block extends BlockStorage{
    /** internal name */
    public final String name;
    /** display name */
    public String formalName;
    /** Detailed description of the block. Can be as long as necesary. */
    public final String fullDescription;
    /** whether this block has a tile entity that updates */
    public boolean update;
    /** whether this block has health and can be destroyed */
    public boolean destructible;
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
    /**Special flag; if false, floor will be drawn under this block even if it is cached.*/
    public boolean fillsTile = true;
    /** Layer to draw extra stuff on. */
    public Layer layer = null;
    /** Extra layer to draw extra extra stuff on. */
    public Layer layer2 = null;
    /** whether this block can be replaced in all cases */
    public boolean alwaysReplace = false;
    /** whether this block has instant transfer checking. used for calculations to prevent infinite loops. */
    public boolean instantTransfer = false;
    /** The block group. Unless {@link #canReplace} is overriden, blocks in the same group can replace each other. */
    public BlockGroup group = BlockGroup.none;
    /** List of block flags. Used for AI indexing. */
    public EnumSet<BlockFlag> flags = EnumSet.of();
    /** Whether the block can be tapped and selected to configure. */
    public boolean configurable;
    /** Whether this block consumes touchDown events when tapped. */
    public boolean consumesTap;
    /** The color of this block when displayed on the minimap or map preview.
     *  Do not set manually! This is overriden when loading.*/
    public Color color = new Color(0, 0, 0, 1);
    /**Whether units target this block.*/
    public boolean targetable = true;
    /**Whether the overdrive core has any effect on this block.*/
    public boolean canOverdrive = true;

    /**Cost of constructing this block.*/
    public ItemStack[] buildRequirements = new ItemStack[]{};
    /**Category in place menu.*/
    public Category buildCategory = Category.distribution;
    /**Cost of building this block; do not modify directly!*/
    public float buildCost;
    /**Whether this block is visible and can currently be built.*/
    public BooleanProvider buildVisibility = () -> false;
    public boolean alwaysUnlocked = false;

    protected Array<Tile> tempTiles = new Array<>();
    protected TextureRegion[] icons = new TextureRegion[Icon.values().length];
    protected TextureRegion[] generatedIcons;
    protected TextureRegion[] variantRegions;
    protected TextureRegion region;

    public Block(String name){
        this.name = name;
        this.formalName = Core.bundle.get("block." + name + ".name", name);
        this.fullDescription = Core.bundle.getOrNull("block." + name + ".description");
        this.solid = false;
    }

    public boolean canBreak(Tile tile){
        return true;
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
            if(other.entity.power != null && other.entity.power.graph != null){
                other.entity.power.graph.add(entity.power.graph);
            }
        }
    }

    protected void powerGraphRemoved(Tile tile){
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
        for(Tile other : tile.entity.proximity()){
            if(other.entity.power != null && !(consumesPower && other.block().consumesPower && !outputsPower && !other.block().outputsPower)
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

    public boolean isLayer(Tile tile){
        return true;
    }

    public boolean isLayer2(Tile tile){
        return true;
    }

    public void drawLayer(Tile tile){
    }

    public void drawLayer2(Tile tile){
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(Tile tile){
    }

    /** Drawn when you are placing a block. */
    public void drawPlace(int x, int y, int rotation, boolean valid){
    }

    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), rotate ? tile.getRotation() * 90 : 0);
    }

    public void drawShadow(Tile tile){
        draw(tile);
    }

    public void drawTeam(Tile tile){
        Draw.color(tile.getTeam().color);
        Draw.rect("block-border", tile.drawx() - size * tilesize/2f + 4, tile.drawy() - size * tilesize/2f + 4);
        Draw.color();
    }

    /** Called after the block is placed by this client. */
    public void playerPlaced(Tile tile){
    }

    public void removed(Tile tile){
    }

    /** Called after the block is placed by anyone. */
    public void placed(Tile tile){
    }

    /** Called every frame a unit is on this tile. */
    public void unitOn(Tile tile, Unit unit){
    }

    /** Called when a unit that spawned at this tile is removed.*/
    public void unitRemoved(Tile tile, Unit unit){

    }

    /** Returns whether ot not this block can be place on the specified tile. */
    public boolean canPlaceOn(Tile tile){
        return true;
    }

    /**Call when some content is produced. This unlocks the content if it is applicable.*/
    public void useContent(Tile tile, UnlockableContent content){
        if(!headless && tile.getTeam() == players[0].getTeam()){
            logic.handleContent(content);
        }
    }

    @Override
    public String localizedName(){
        return formalName;
    }

    @Override
    public TextureRegion getContentIcon(){
        return icon(Icon.medium);
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayBlock(table, this);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.block;
    }

    @Override
    public String getContentName() {
        return name;
    }

    /** Called after all blocks are created. */
    @Override
    public void init(){
        //initialize default health based on size
        if(health == -1){
            health = size * size * 40;
        }

        buildCost = 0f;
        for(ItemStack stack : buildRequirements){
            buildCost += stack.amount * stack.item.cost;
        }

        setStats();

        consumes.checkRequired(this);

        if(buildRequirements.length > 0 && !Core.bundle.has("block." + name + ".name")){
            Log.warn("No name for block '{0}' found. Add the following to bundle.properties:\nblock.{0}.name = {1}", name, Strings.capitalize(name));
        }
    }

    @Override
    public void load(){
        region = Core.atlas.find(name);
    }

    /** Called when the block is tapped. */
    public void tapped(Tile tile, Player player){

    }

    /** Returns whether or not a hand cursor should be shown over this block. */
    public Cursor getCursor(Tile tile){
        return configurable ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when this block is tapped to build a UI on the table.
     * {@link #configurable} able} must return true for this to be called.
     */
    public void buildTable(Tile tile, Table table){
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

        consumes.forEach(cons -> cons.display(stats));

        // Note: Power stats are added by the consumers.
        if(hasLiquids) stats.add(BlockStat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if(hasItems) stats.add(BlockStat.itemCapacity, itemCapacity, StatUnit.items);
    }

    public boolean isSolidFor(Tile tile){
        return false;
    }

    public boolean canReplace(Block other){
        return (other != this || rotate) && this.group != BlockGroup.none && other.group == this.group;
    }

    public float handleDamage(Tile tile, float amount){
        return amount;
    }

    public void handleBulletHit(TileEntity entity, Bullet bullet){
        entity.damage(bullet.damage());
    }

    public void update(Tile tile){
    }

    /**Called when this tile is randomly updated. This only happens on a client, and should be used for effects only.
     * TODO currently unimplemented*/
    public void randomUpdate(Tile tile){
    }

    public boolean isAccessible(){
        return (hasItems && itemCapacity > 0);
    }

    /** Called after the block is destroyed and removed. */
    public void afterDestroyed(Tile tile, TileEntity entity){

    }

    /** Called when the block is destroyed. */
    public void onDestroyed(Tile tile){
        float x = tile.worldx(), y = tile.worldy();
        float explosiveness = baseExplosiveness;
        float flammability = 0f;
        float power = 0f;

        if(hasItems){
            float scaling = inventoryScaling(tile);
            for(Item item : content.items()){
                int amount = tile.entity.items.get(item);
                explosiveness += item.explosiveness * amount * scaling;
                flammability += item.flammability * amount * scaling;
            }
        }

        if(hasLiquids){
            flammability += tile.entity.liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += tile.entity.liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(consumes.has(ConsumePower.class) && consumes.get(ConsumePower.class).isBuffered){
            power += tile.entity.power.satisfaction * consumes.get(ConsumePower.class).powerCapacity;
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

        Damage.dynamicExplosion(x, y, flammability, explosiveness, power, tilesize * size / 2f, Pal.darkFlame);
        if(!tile.floor().solid && !tile.floor().isLiquid){
            RubbleDecal.create(tile.drawx(), tile.drawy(), size);
        }
    }

    /**Returns scaled # of inventories in this block.*/
    public float inventoryScaling(Tile tile){
        return 1f;
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
        return formalName;
    }

    public TextureRegion getDisplayIcon(Tile tile){
        return icon(Icon.medium);
    }

    public void display(Tile tile, Table table){
        TileEntity entity = tile.entity;

        if(entity != null){
            table.table(bars -> {
                bars.defaults().growX().height(18f).pad(4);

                displayBars(tile, bars);
            }).growX();

            table.marginBottom(-5);
        }
    }

    public void displayBars(Tile tile, Table bars){
        TileEntity entity = tile.entity;

        bars.add(new Bar("blocks.health", Pal.health, entity::healthf).blink(Color.WHITE));
        bars.row();

        if(entity.liquids != null){
            bars.add(new Bar(() -> entity.liquids.get(entity.liquids.current()) <= 0.001f ? Core.bundle.get("blocks.liquid") : entity.liquids.current().localizedName(), () -> entity.liquids.current().color, () -> entity.liquids.total() / liquidCapacity)).growX();
            bars.row();
        }

        if(entity.power != null && consumes.has(ConsumePower.class)){
            bars.add(new Bar(consumes.get(ConsumePower.class).isBuffered ? "blocks.power" : "blocks.power.satisfaction", Pal.power, () -> entity.power.satisfaction)).growX();
            bars.row();
        }
    }

    public TextureRegion icon(Icon icon){
        if(icons[icon.ordinal()] == null){
            icons[icon.ordinal()] = Core.atlas.find(name + "-icon-" + icon.name(), icon == Icon.full ? getGeneratedIcons()[0] : Core.atlas.find(name + "-icon-full", getGeneratedIcons()[0]));
        }
        return icons[icon.ordinal()];
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
            variantRegions = new TextureRegion[]{icon(Icon.full)};
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

    public boolean isMultiblock(){
        return size > 1;
    }

    public boolean isVisible(){
        return buildVisibility.get() && !isHidden();
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    protected void requirements(Category cat, ItemStack[] stacks, boolean unlocked){
        requirements(cat, () -> true, stacks);
        this.alwaysUnlocked = unlocked;
    }

    protected void requirements(Category cat, ItemStack[] stacks){
        requirements(cat, () -> true, stacks);
    }

    /**Sets up requirements. Use only this method to set up requirements.*/
    protected void requirements(Category cat, BooleanProvider visible, ItemStack[] stacks){
        this.buildCategory = cat;
        this.buildRequirements = stacks;
        this.buildVisibility = visible;

        Arrays.sort(buildRequirements, (a, b) -> Integer.compare(a.item.id, b.item.id));
    }

    public enum Icon{
        small(8 * 3),
        medium(8 * 4),
        large(8 * 6),
        /**uses whatever the size of the block is*/
        full(0);

        public final int size;

        Icon(int size){
            this.size = size;
        }
    }
}
