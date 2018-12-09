package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.effect.RubbleDecal;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.meta.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.Button;
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
        if(tile.entity.power != null) tile.block().powerGraphRemoved(tile);
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

    /** Called when it is being enabled. */
    public void onEnable(Tile tile){
    }

    /** Called when it is being disabled. */
    public void onDisable(Tile tile){
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
        return hasDetails(tile) ? CursorType.hand : CursorType.normal;
    }

    public boolean hasDetails(Tile tile){
        return buildInfo(tile, new Table()) || buildPower(tile, new Table()) || buildConfig(tile, new Table()) || buildLogic(tile, new Table()) || buildTable(tile, new Table());
    }

    /**
     * Called when this block is tapped to build a info UI on the table.
     */
    public boolean buildInfo(Tile tile, Table table, boolean build){
        if(build){
            for(BlockBar bar : bars.list()){
                if(bar.type == BarType.inventory) continue;
                Label label = table.add(Bundles.format("text.blocks.info." + bar.type.name(), (float)Math.round(bar.value.getValue(tile) * 100f) / 100f, bar.value.getMax(tile))).get();
                label.update(() -> label.setText(Bundles.format("text.blocks.info." + bar.type.name(), (float)Math.round(bar.value.getValue(tile) * 100f) / 100f, bar.value.getMax(tile))));
                table.row();
            }
        }
        return !bars.list().isEmpty();
    }

    public final boolean buildInfo(Tile tile, Table table){
        return buildInfo(tile, table, false);
    }

    /**
     * Called when this block is tapped to build a power UI on the table.
     */
    public boolean buildPower(Tile tile, Table table, boolean build){
        if(build){
            if(hasPower){
                if(consumesPower){
                    ButtonGroup<TextButton> group = new ButtonGroup<>();
                    group.setMinCheckCount(1);
                    for(byte i = 1; i <= 9; i++){
                        TextButton button = table.addButton(String.valueOf(i), "toggle", () -> {}).size(16f).group(group).get();
                        button.setChecked(i == tile.entity.power.priority);
                        button.changed(() -> Call.onPowerPrioritySet(players[0], tile, Byte.parseByte(button.getText().toString())));
                        button.setProgrammaticChangeEvents(false);
                        button.update(() -> button.setChecked(Byte.parseByte(button.getText().toString()) == tile.entity.power.priority));
                    }
                    table.row();
                }

                Label produced = table.add(Bundles.format("text.blocks.power.produced", (float)Math.round(tile.entity.power.graph.getProduced() * 100f) / 100f, Bundles.get("text.unit.powersecond"))).get();
                produced.update(() -> produced.setText(Bundles.format("text.blocks.power.produced", (float)Math.round(tile.entity.power.graph.getProduced() * 100f) / 100f, Bundles.get("text.unit.powersecond"))));
                table.row();

                Label stored = table.add(Bundles.format("text.blocks.power.stored", (float)Math.round(tile.entity.power.graph.getStored() * 100f) / 100f, Bundles.get("text.unit.powerunits"))).get();
                stored.update(() -> stored.setText(Bundles.format("text.blocks.power.stored", (float)Math.round(tile.entity.power.graph.getStored() * 100f) / 100f, Bundles.get("text.unit.powerunits"))));
                table.row();

                Label used = table.add(Bundles.format("text.blocks.power.used", (float)Math.round(tile.entity.power.graph.getUsed() * 100f) / 100f, Bundles.get("text.unit.powersecond"))).get();
                used.update(() -> used.setText(Bundles.format("text.blocks.power.used", (float)Math.round(tile.entity.power.graph.getUsed() * 100f) / 100f, Bundles.get("text.unit.powersecond"))));
                table.row();

                Label change = table.add(Bundles.format("text.blocks.power.change", (float)Math.round(tile.entity.power.graph.getChange() * 100f) / 100f, Bundles.get("text.unit.powersecond"))).get();
                change.update(() -> change.setText(Bundles.format("text.blocks.power.change", (float)Math.round(tile.entity.power.graph.getChange() * 100f) / 100f, Bundles.get("text.unit.powersecond"))));
                table.row();

                Label charged = table.add(Bundles.format("text.blocks.power.charged", (float)Math.round(tile.entity.power.graph.getCharged() * 100f) / 100f, Bundles.get("text.unit.powersecond"))).get();
                charged.update(() -> charged.setText(Bundles.format("text.blocks.power.charged", (float)Math.round(tile.entity.power.graph.getCharged() * 100f) / 100f, Bundles.get("text.unit.powersecond"))));
                table.row();
            }
        }
        return hasPower;
    }

    public final boolean buildPower(Tile tile, Table table){
        return buildPower(tile, table, false);
    }

    @Remote(called = Loc.server, forward = true, targets = Loc.both)
    public static void onPowerPrioritySet(Player player, Tile tile, byte priority){
        tile.entity.power.priority = priority;
        tile.entity.power.graph.sort();
    }

    /**
     * Called when this block is tapped to build a details UI on the table.
     */
    public boolean buildConfig(Tile tile, Table table, boolean build){
        return false;
    }

    public final boolean buildConfig(Tile tile, Table table){
        return buildConfig(tile, table, false);
    }

    /**
     * Called when this block is tapped to build a logic UI on the table.
     */
    public boolean buildLogic(Tile tile, Table table, boolean build){
        if(build){
            CheckBox check = table.addCheck(Bundles.get("text.blocks.config.enable"), tile.entity.enabled, checked -> {}).get();
            check.setProgrammaticChangeEvents(false);
            check.update(() -> check.setChecked(tile.entity.enabled));
            check.changed(() -> Call.onEnableSet(players[0], tile, check.isChecked()));
            table.row();
        }
        return true;
    }

    public final boolean buildLogic(Tile tile, Table table){
        return buildLogic(tile, table, false);
    }

    @Remote(called = Loc.server, forward = true, targets = Loc.both)
    public static void onEnableSet(Player player, Tile tile, boolean checked){
        Block block = tile.block();
        if(tile.entity.enabled = checked) block.onEnable(tile);
        else block.onDisable(tile);
    }

    /**
     * Called when this block is tapped to build a custom UI on the table.
     */
    public boolean buildTable(Tile tile, Table table){
        return false;
    }

    /**
     * Called when another tile is tapped while this block is selected.
     * Returns whether or not this block should be deselected.
     */
    public boolean onDetailsTileTapped(Tile tile, Tile other){
        return tile != other;
    }

    /** Returns whether this details menu should show when the specified player taps it. */
    public boolean shouldShowDetails(Tile tile, Player player){
        return true;
    }

    /** Whether this details should be hidden now. Called every frame the details is open. */
    public boolean shouldHideDetails(Tile tile, Player player){
        return false;
    }

    public boolean synthetic(){
        return update || destructible || solid;
    }

    public void drawConfiguration(Tile tile){
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

        if(hasPower) stats.add(BlockStat.powerCapacity, powerCapacity, StatUnit.powerUnits);
        if(hasLiquids) stats.add(BlockStat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if(hasItems) stats.add(BlockStat.itemCapacity, itemCapacity, StatUnit.items);
    }

    public void setBars(){
        if(hasEntity()) bars.add(new BlockBar(BarType.health, false, new BlockBar.Value(tile -> tile.entity.health, tile -> tile.block().health)));
        if(hasPower) bars.add(new BlockBar(BarType.power, true, new BlockBar.Value(tile -> tile.entity.power.amount, tile -> powerCapacity)));
        if(hasLiquids) bars.add(new BlockBar(BarType.liquid, true, new BlockBar.Value(tile -> tile.entity.liquids.total(), tile -> liquidCapacity)));
        if(hasItems) bars.add(new BlockBar(BarType.inventory, true, new BlockBar.Value(tile -> tile.entity.items.total(), tile -> itemCapacity)));
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
            float scaling = inventoryScaling(tile);
            for(Item item : content.items()){
                int amount = tile.entity.items.get(item);
                explosiveness += item.explosiveness * amount * scaling;
                flammability += item.flammability * amount * scaling;

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

        if(hasPower){
            power += tile.entity.power.amount;
        }

        tempColor.mul(1f / units);

        if(hasLiquids){

            tile.entity.liquids.forEach((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Timers.run(i / 2f, () -> {
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
