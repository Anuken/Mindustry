package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.effect.RubbleDecal;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.meta.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Block extends BaseBlock {
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
    /** whether this block can be placed on liquids. */
    public boolean floating = false;
    /** stuff that drops when broken */
    public ItemStack drops = null;
    /** multiblock size */
    public int size = 1;
    /** Whether to draw this block in the expanded draw range. */
    public boolean expanded = false;
    /** Max of timers used. */
    public int timers = 0;
    /** Cache layer. Only used for 'cached' rendering. */
    public CacheLayer cacheLayer = CacheLayer.normal;
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
    /** list of displayed block status bars. Defaults to health bar. */
    public BlockBars bars = new BlockBars();
    /** List of block stats. */
    public BlockStats stats = new BlockStats(this);
    /** List of block flags. Used for AI indexing. */
    public EnumSet<BlockFlag> flags;
    /** Whether to automatically set the entity to 'sleeping' when created. */
    public boolean autoSleep;
    /** Name of shadow region to load. Null to indicate normal shadow. */
    public String shadow = null;
    /** Whether the block can be tapped and selected to configure. */
    public boolean configurable;
    /** Whether this block consumes touchDown events when tapped. */
    public boolean consumesTap;
    /** The color of this block when displayed on the minimap or map preview. */
    public Color minimapColor = Color.CLEAR;
    /** View range of this block type. Use a value < 0 to disable. */
    public float viewRange = 10;
    /**Whether the top icon is outlined, like a turret.*/
    public boolean turretIcon = false;
    /**Whether units target this block.*/
    public boolean targetable = true;
    /**Whether the overdrive core has any effect on this block.*/
    public boolean canOverdrive = true;

    protected Array<Tile> tempTiles = new Array<>();
    protected Color tempColor = new Color();
    protected TextureRegion[] blockIcon;
    protected TextureRegion[] icon;
    protected TextureRegion[] compactIcon;
    protected TextureRegion editorIcon;

    public TextureRegion shadowRegion;
    public TextureRegion region;

    public Block(String name){
        this.name = name;
        this.formalName = Bundles.get("block." + name + ".name", name);
        this.fullDescription = Bundles.getOrNull("block." + name + ".description");
        this.solid = false;
    }

    /**Populates the array with all blocks that produce this content.*/
    public static void getByProduction(Array<Block> arr, Content result){
        arr.clear();
        for(Block block : content.blocks()){
            if(block.produces.get() == result){
                arr.add(block);
            }
        }
    }

    public boolean canBreak(Tile tile){
        return true;
    }

    public boolean dropsItem(Item item){
        return drops != null && drops.item == item;
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
                other.entity.power.links.removeValue(tile.packedPosition());
            }
        }
    }

    public Array<Tile> getPowerConnections(Tile tile, Array<Tile> out){
        out.clear();
        for(Tile other : tile.entity.proximity()){
            if(other.entity.power != null && !(consumesPower && other.block().consumesPower && !outputsPower && !other.block().outputsPower)
                    && !tile.entity.power.links.contains(other.packedPosition())){
                out.add(other);
            }
        }

        for(int i = 0; i < tile.entity.power.links.size; i++){
            Tile link = world.tile(tile.entity.power.links.get(i));
            if(link != null && link.entity != null && link.entity.power != null) out.add(link);
        }
        return out;
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

        setStats();
        setBars();

        consumes.checkRequired(this);
    }

    @Override
    public void load(){
        shadowRegion = Draw.region(shadow == null ? "shadow-" + size : shadow);
        region = Draw.region(name);
    }

    /**Called when the world is resized.
     * Call super!*/
    public void transformLinks(Tile tile, int oldWidth, int oldHeight, int newWidth, int newHeight, int shiftX, int shiftY){
        if(tile.entity != null && tile.entity.power != null){
            IntArray links = tile.entity.power.links;
            IntArray out = new IntArray();
            for(int i = 0; i < links.size; i++){
                out.add(world.transform(links.get(i), oldWidth, oldHeight, newWidth, shiftX, shiftY));
            }
            tile.entity.power.links = out;
        }
    }

    /** Called when the block is tapped. */
    public void tapped(Tile tile, Player player){

    }

    /** Returns whether or not a hand cursor should be shown over this block. */
    public CursorType getCursor(Tile tile){
        return configurable ? CursorType.hand : CursorType.normal;
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
        return update || destructible || solid;
    }

    public void drawConfigure(Tile tile){
        Draw.color(Palette.accent);
        Lines.stroke(1f);
        Lines.square(tile.drawx(), tile.drawy(),
                tile.block().size * tilesize / 2f + 1f);
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

    public void setBars(){
        if(consumes.has(ConsumePower.class))
            bars.add(new BlockBar(BarType.power, true, tile -> tile.entity.power.satisfaction));
        if(hasLiquids)
            bars.add(new BlockBar(BarType.liquid, true, tile -> tile.entity.liquids.total() / liquidCapacity));
        if(hasItems)
            bars.add(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.items.total() / itemCapacity));
    }

    public String name(){
        return name;
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
        entity.damage(bullet.getDamage());
    }

    public void update(Tile tile){
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
        int units = 1;
        tempColor.set(Palette.darkFlame);

        if(hasItems){
            for(Item item : content.items()){
                int amount = tile.entity.items.get(item);
                explosiveness += item.explosiveness * amount;
                flammability += item.flammability * amount;

                if(item.flammability * amount > 0.5){
                    units++;
                    Hue.addu(tempColor, item.flameColor);
                }
            }
        }

        if(hasLiquids){
            flammability += tile.entity.liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += tile.entity.liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(consumes.has(ConsumePower.class) && consumes.get(ConsumePower.class).isBuffered){
            power += tile.entity.power.satisfaction * consumes.get(ConsumePower.class).powerCapacity;
        }

        tempColor.mul(1f / units);

        if(hasLiquids){

            tile.entity.liquids.forEach((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Timers.run(i / 2, () -> {
                        Tile other = world.tile(tile.x + Mathf.range(size / 2), tile.y + Mathf.range(size / 2));
                        if(other != null){
                            Puddle.deposit(other, liquid, splash);
                        }
                    });
                }
            });
        }

        Damage.dynamicExplosion(x, y, flammability, explosiveness, power, tilesize * size / 2f, tempColor);
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
        return formalName;
    }

    public TextureRegion getDisplayIcon(Tile tile){
        return getEditorIcon();
    }

    public TextureRegion getEditorIcon(){
        if(editorIcon == null){
            editorIcon = Draw.region("block-icon-" + name, Draw.region("clear"));
        }
        return editorIcon;
    }

    /** Returns the icon used for displaying this block in the place menu */
    public TextureRegion[] getIcon(){
        if(icon == null){
            if(Draw.hasRegion(name + "-icon")){
                icon = new TextureRegion[]{Draw.region(name + "-icon")};
            }else if(Draw.hasRegion(name)){
                icon = new TextureRegion[]{Draw.region(name)};
            }else if(Draw.hasRegion(name + "1")){
                icon = new TextureRegion[]{Draw.region(name + "1")};
            }else{
                icon = new TextureRegion[]{};
            }
        }

        return icon;
    }

    /** Returns a list of regions that represent this block in the world */
    public TextureRegion[] getBlockIcon(){
        return getIcon();
    }

    /** Returns a list of icon regions that have been cropped to 8x8 */
    public TextureRegion[] getCompactIcon(){
        if(compactIcon == null){
            compactIcon = new TextureRegion[getIcon().length];
            for(int i = 0; i < compactIcon.length; i++){
                compactIcon[i] = iconRegion(getIcon()[i]);
            }
        }
        return compactIcon;
    }

    /** Crops a regionto 8x8 */
    protected TextureRegion iconRegion(TextureRegion src){
        TextureRegion region = new TextureRegion(src);
        region.setRegionWidth(8);
        region.setRegionHeight(8);
        return region;
    }

    public boolean hasEntity(){
        return destructible || update;
    }

    public TileEntity newEntity(){
        return new TileEntity();
    }

    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), rotate ? tile.getRotation() * 90 : 0);
    }

    public void drawNonLayer(Tile tile){
    }

    public void drawShadow(Tile tile){
        Draw.rect(shadowRegion, tile.drawx(), tile.drawy());
    }

    /** Offset for placing and drawing multiblocks. */
    public float offset(){
        return ((size + 1) % 2) * tilesize / 2;
    }

    public boolean isMultiblock(){
        return size > 1;
    }

    public Array<Object> getDebugInfo(Tile tile){
        return Array.with(
                "block", tile.block().name,
                "floor", tile.floor().name,
                "x", tile.x,
                "y", tile.y,
                "entity.name", tile.entity.getClass(),
                "entity.x", tile.entity.x,
                "entity.y", tile.entity.y,
                "entity.id", tile.entity.id,
                "entity.items.total", hasItems ? tile.entity.items.total() : null,
                "entity.graph", tile.entity.power != null && tile.entity.power.graph != null ? tile.entity.power.graph.getID() : null
        );
    }
}
