package mindustry.world;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.EnumSet;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;
import java.util.*;

import static mindustry.Vars.*;

public class Block extends UnlockableContent implements Senseable{
    /** If true, buildings have an ItemModule. */
    public boolean hasItems;
    /** If true, buildings have a LiquidModule. */
    public boolean hasLiquids;
    /** If true, buildings have a PowerModule. */
    public boolean hasPower;
    /** Flag for determining whether this block outputs liquid somewhere; used for connections. */
    public boolean outputsLiquid = false;
    /** Used by certain power blocks (nodes) to flag as non-consuming of power. True by default, even if this block has no power. */
    public boolean consumesPower = true;
    /** If true, this block is a generator that can produce power. */
    public boolean outputsPower = false;
    /** If false, power nodes cannot connect to this block. */
    public boolean connectedPower = true;
    /** If true, this block can conduct power like a cable. */
    public boolean conductivePower = false;
    /** If true, this block can output payloads; affects blending. */
    public boolean outputsPayload = false;
    /** If true, payloads will attempt to move into this block. */
    public boolean acceptsPayload = false;
    /** Visual flag use for blending of certain transportation blocks. */
    public boolean acceptsItems = false;
    /** If true, all item capacities of this block are separate instead of pooled as one number. */
    public boolean separateItemCapacity = false;
    /** maximum items this block can carry (usually, this is per-type of item) */
    public int itemCapacity = 10;
    /** maximum total liquids this block can carry if hasLiquids = true */
    public float liquidCapacity = 10f;
    /** higher numbers increase liquid output speed; TODO remove and replace with better liquids system */
    public float liquidPressure = 1f;
    /** If true, this block outputs to its facing direction, when applicable.
     * Used for blending calculations. */
    public boolean outputFacing = true;
    /** if true, this block does not accept input from the sides (used for armored conveyors) */
    public boolean noSideBlend = false;
    /** whether to display flow rate */
    public boolean displayFlow = true;
    /** whether this block is visible in the editor */
    public boolean inEditor = true;
    /** the last configuration value applied to this block. */
    public @Nullable Object lastConfig;
    /** whether to save the last config and apply it to newly placed blocks */
    public boolean saveConfig = false;
    /** whether to allow copying the config through middle click */
    public boolean copyConfig = true;
    /** if true, double-tapping this configurable block clears configuration. */
    public boolean clearOnDoubleTap = false;
    /** whether this block has a tile entity that updates */
    public boolean update;
    /** whether this block has health and can be destroyed */
    public boolean destructible;
    /** whether unloaders work on this block */
    public boolean unloadable = true;
    /** if true, this block acts a duct and will connect to armored ducts from the side. */
    public boolean isDuct = false;
    /** whether units can resupply by taking items from this block */
    public boolean allowResupply = false;
    /** whether this is solid */
    public boolean solid;
    /** whether this block CAN be solid. */
    public boolean solidifes;
    /** if true, this counts as a non-solid block to this team. */
    public boolean teamPassable;
    /** if true, this block cannot be hit by bullets unless explicitly targeted. */
    public boolean underBullets;
    /** whether this is rotatable */
    public boolean rotate;
    /** if rotate is true and this is false, the region won't rotate when drawing */
    public boolean rotateDraw = true;
    /** if true, schematic flips with this block are inverted. */
    public boolean invertFlip = false;
    /** number of different variant regions to use */
    public int variants = 0;
    /** whether to draw a rotation arrow - this does not apply to lines of blocks */
    public boolean drawArrow = true;
    /** whether to draw the team corner by default */
    public boolean drawTeamOverlay = true;
    /** for static blocks only: if true, tile data() is saved in world data. */
    public boolean saveData;
    /** whether you can break this with rightclick */
    public boolean breakable;
    /** whether to add this block to brokenblocks */
    public boolean rebuildable = true;
    /** if true, this logic-related block can only be used with privileged processors (or is one itself) */
    public boolean privileged = false;
    /** whether this block can only be placed on water */
    public boolean requiresWater = false;
    /** whether this block can be placed on any liquids, anywhere */
    public boolean placeableLiquid = false;
    /** whether this block can be placed directly by the player via PlacementFragment */
    public boolean placeablePlayer = true;
    /** whether this floor can be placed on. */
    public boolean placeableOn = true;
    /** whether this block has insulating properties. */
    public boolean insulated = false;
    /** whether the sprite is a full square. */
    public boolean squareSprite = true;
    /** whether this block absorbs laser attacks. */
    public boolean absorbLasers = false;
    /** if false, the status is never drawn */
    public boolean enableDrawStatus = true;
    /** whether to draw disabled status */
    public boolean drawDisabled = true;
    /** whether to automatically reset enabled status after a logic block has not interacted for a while. */
    public boolean autoResetEnabled = true;
    /** if true, the block stops updating when disabled */
    public boolean noUpdateDisabled = false;
    /** if true, this block updates when it's a payload in a unit. */
    public boolean updateInUnits = true;
    /** if true, this block updates in payloads in units regardless of the experimental game rule */
    public boolean alwaysUpdateInUnits = false;
    /** Whether to use this block's color in the minimap. Only used for overlays. */
    public boolean useColor = true;
    /** item that drops from this block, used for drills */
    public @Nullable Item itemDrop = null;
    /** if true, this block cannot be mined by players. useful for annoying things like sand. */
    public boolean playerUnmineable = false;
    /** Array of affinities to certain things. */
    public Attributes attributes = new Attributes();
    /** Health per square tile that this block occupies; essentially, this is multiplied by size * size. Overridden if health is > 0. If <0, the default is 40. */
    public float scaledHealth = -1;
    /** building health; -1 to use scaledHealth */
    public int health = -1;
    /** damage absorption, similar to unit armor */
    public float armor = 0f;
    /** base block explosiveness */
    public float baseExplosiveness = 0f;
    /** bullet that this block spawns when destroyed */
    public @Nullable BulletType destroyBullet = null;
    /** liquid used for lighting */
    public @Nullable Liquid lightLiquid;
    /** whether cracks are drawn when this block is damaged */
    public boolean drawCracks = true;
    /** whether rubble is created when this block is destroyed */
    public boolean createRubble = true;
    /** whether this block can be placed on edges of liquids. */
    public boolean floating = false;
    /** multiblock size */
    public int size = 1;
    /** multiblock offset */
    public float offset = 0f;
    /** offset for iteration (internal use only) */
    public int sizeOffset = 0;
    /** Clipping size of this block. Should be as large as the block will draw. */
    public float clipSize = -1f;
    /** When placeRangeCheck is enabled, this is the range checked for enemy blocks. */
    public float placeOverlapRange = 50f;
    /** Multiplier of damage dealt to this block by tanks. Does not apply to crawlers. */
    public float crushDamageMultiplier = 1f;
    /** Max of timers used. */
    public int timers = 0;
    /** Cache layer. Only used for 'cached' rendering. */
    public CacheLayer cacheLayer = CacheLayer.normal;
    /** Special flag; if false, floor will be drawn under this block even if it is cached. */
    public boolean fillsTile = true;
    /** If true, this block can be covered by darkness / fog even if synthetic. */
    public boolean forceDark = false;
    /** whether this block can be replaced in all cases */
    public boolean alwaysReplace = false;
    /** if false, this block can never be replaced. */
    public boolean replaceable = true;
    /** The block group. Unless {@link #canReplace} is overridden, blocks in the same group can replace each other. */
    public BlockGroup group = BlockGroup.none;
    /** List of block flags. Used for AI indexing. */
    public EnumSet<BlockFlag> flags = EnumSet.of();
    /** Targeting priority of this block, as seen by enemies. */
    public float priority = TargetPriority.base;
    /** How much this block affects the unit cap by.
     * The block flags must contain unitModifier in order for this to work. */
    public int unitCapModifier = 0;
    /** Whether the block can be tapped and selected to configure. */
    public boolean configurable;
    /** If true, this building can be selected like a unit when commanding. */
    public boolean commandable;
    /** If true, the building inventory can be shown with the config. */
    public boolean allowConfigInventory = true;
    /** Defines how large selection menus, such as that of sorters, should be. */
    public int selectionRows = 5, selectionColumns = 4;
    /** If true, this block can be configured by logic. */
    public boolean logicConfigurable = false;
    /** Whether this block consumes touchDown events when tapped. */
    public boolean consumesTap;
    /** Whether to draw the glow of the liquid for this block, if it has one. */
    public boolean drawLiquidLight = true;
    /** Environmental flags that are *all* required for this block to function. 0 = any environment */
    public int envRequired = 0;
    /** The environment flags that this block can function in. If the env matches any of these, it will be enabled. */
    public int envEnabled = Env.terrestrial;
    /** The environment flags that this block *cannot* function in. If the env matches any of these, it will be *disabled*. */
    public int envDisabled = 0;
    /** Whether to periodically sync this block across the network. */
    public boolean sync;
    /** Whether this block uses conveyor-type placement mode. */
    public boolean conveyorPlacement;
    /** If false, diagonal placement (ctrl) for this block is not allowed. */
    public boolean allowDiagonal = true;
    /** Whether to swap the diagonal placement modes. */
    public boolean swapDiagonalPlacement;
    /** Build queue priority in schematics. */
    public int schematicPriority = 0;
    /**
     * The color of this block when displayed on the minimap or map preview.
     * Do not set manually! This is overridden when loading for most blocks.
     */
    public Color mapColor = new Color(0, 0, 0, 1);
    /** Whether this block has a minimap color. */
    public boolean hasColor = false;
    /** Whether units target this block. */
    public boolean targetable = true;
    /** If true, this block attacks and is considered a turret in the indexer. Building must implement Ranged. */
    public boolean attacks = false;
    /** If true, this block is mending-related and can be suppressed with special units/missiles. */
    public boolean suppressable = false;
    /** Whether the overdrive core has any effect on this block. */
    public boolean canOverdrive = true;
    /** Outlined icon color.*/
    public Color outlineColor = Color.valueOf("404049");
    /** Whether any icon region has an outline added. */
    public boolean outlineIcon = false;
    /** Outline icon radius. */
    public int outlineRadius = 4;
    /** Which of the icon regions gets the outline added. Uses last icon if <= 0. */
    public int outlinedIcon = -1;
    /** Whether this block has a shadow under it. */
    public boolean hasShadow = true;
    /** If true, a custom shadow (name-shadow) is drawn under this block. */
    public boolean customShadow = false;
    /** Should the sound made when this block is built change in pitch. */
    public boolean placePitchChange = true;
    /** Should the sound made when this block is deconstructed change in pitch. */
    public boolean breakPitchChange = true;
    /** Sound made when this block is built. */
    public Sound placeSound = Sounds.place;
    /** Sound made when this block is deconstructed. */
    public Sound breakSound = Sounds.breaks;
    /** Sounds made when this block is destroyed.*/
    public Sound destroySound = Sounds.boom;
    /** How reflective this block is. */
    public float albedo = 0f;
    /** Environmental passive light color. */
    public Color lightColor = Color.white.cpy();
    /**
     * Whether this environmental block passively emits light.
     * Does not change behavior for non-environmental blocks, but still updates clipSize. */
    public boolean emitLight = false;
    /** Radius of the light emitted by this block. */
    public float lightRadius = 60f;

    /** How much fog this block uncovers, in tiles. Cannot be dynamic. <= 0 to disable. */
    public int fogRadius = -1;

    /** The sound that this block makes while active. One sound loop. Do not overuse. */
    public Sound loopSound = Sounds.none;
    /** Active sound base volume. */
    public float loopSoundVolume = 0.5f;

    /** The sound that this block makes while idle. Uses one sound loop for all blocks. */
    public Sound ambientSound = Sounds.none;
    /** Idle sound base volume. */
    public float ambientSoundVolume = 0.05f;

    /** Cost of constructing this block. */
    public ItemStack[] requirements = {};
    /** Category in place menu. */
    public Category category = Category.distribution;
    /** Time to build this block in ticks; do not modify directly! */
    public float buildCost = 20f;
    /** Whether this block is visible and can currently be built. */
    public BuildVisibility buildVisibility = BuildVisibility.hidden;
    /** Multiplier for speed of building this block. */
    public float buildCostMultiplier = 1f;
    /** Build completion at which deconstruction finishes. */
    public float deconstructThreshold = 0f;
    /** If true, this block deconstructs immediately. Instant deconstruction implies no resource refund. */
    public boolean instantDeconstruct = false;
    /** Effect for placing the block. Passes size as rotation. */
    public Effect placeEffect = Fx.placeBlock;
    /** Effect for breaking the block. Passes size as rotation. */
    public Effect breakEffect = Fx.breakBlock;
    /** Effect for destroying the block. */
    public Effect destroyEffect = Fx.dynamicExplosion;
    /** Multiplier for cost of research in tech tree. */
    public float researchCostMultiplier = 1;
    /** Cost multipliers per-item. */
    public ObjectFloatMap<Item> researchCostMultipliers = new ObjectFloatMap<>();
    /** Override for research cost. Uses multipliers above and building requirements if not set. */
    public @Nullable ItemStack[] researchCost;
    /** Whether this block has instant transfer.*/
    public boolean instantTransfer = false;
    /** Whether you can rotate this block after it is placed. */
    public boolean quickRotate = true;
    /** Main subclass. Non-anonymous. */
    public @Nullable Class<?> subclass;
    /** Scroll position for certain blocks. */
    public float selectScroll;
    /** Building that is created for this block. Initialized in init() via reflection. Set manually if modded. */
    public Prov<Building> buildType = null;
    /** Configuration handlers by type. */
    public ObjectMap<Class<?>, Cons2> configurations = new ObjectMap<>();
    /** Consumption filters. */
    public boolean[] itemFilter = {}, liquidFilter = {};
    /** Array of consumers used by this block. Only populated after init(). */
    public Consume[] consumers = {}, optionalConsumers = {}, nonOptionalConsumers = {}, updateConsumers = {};
    /** Set to true if this block has any consumers in its array. */
    public boolean hasConsumers;
    /** The single power consumer, if applicable. */
    public @Nullable ConsumePower consPower;

    /** Map of bars by name. */
    protected OrderedMap<String, Func<Building, Bar>> barMap = new OrderedMap<>();
    /** List for building up consumption before init(). */
    protected Seq<Consume> consumeBuilder = new Seq<>();

    protected TextureRegion[] generatedIcons;
    protected TextureRegion[] editorVariantRegions;

    /** Regions indexes from icons() that are rotated. If either of these is not -1, other regions won't be rotated in ConstructBlocks. */
    public int regionRotated1 = -1, regionRotated2 = -1;
    public TextureRegion region, editorIcon;
    public @Load("@-shadow") TextureRegion customShadowRegion;
    public @Load("@-team") TextureRegion teamRegion;
    public TextureRegion[] teamRegions, variantRegions, variantShadowRegions;

    protected static final Seq<Tile> tempTiles = new Seq<>();
    protected static final Seq<Building> tempBuilds = new Seq<>();

    /** Dump timer ID.*/
    protected final int timerDump = timers++;
    /** How often to try dumping items in ticks, e.g. 5 = 12 times/sec*/
    protected final int dumpTime = 5;

    public Block(String name){
        super(name);
        initBuilding();
        selectionSize = 28f;
    }

    public void drawBase(Tile tile){
        //delegates to entity unless it is null
        if(tile.build != null){
            tile.build.draw();
        }else{
            Draw.rect(
                variants == 0 ? region :
                variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))],
            tile.drawx(), tile.drawy());
        }
    }

    public void drawShadow(Tile tile){
        Draw.color(0f, 0f, 0f, BlockRenderer.shadowColor.a);
        Draw.rect(
            variants == 0 ? customShadowRegion :
            variantShadowRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantShadowRegions.length - 1))],
        tile.drawx(), tile.drawy(), tile.build == null ? 0f : tile.build.drawrot());
        Draw.color();
    }

    public float percentSolid(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return 0;
        return tile.getLinkedTilesAs(this, tempTiles)
            .sumf(other -> !other.floor().isLiquid ? 1f : 0f) / size / size;
    }

    public void drawEnvironmentLight(Tile tile){
        Drawf.light(tile.worldx(), tile.worldy(), lightRadius, lightColor, lightColor.a);
    }

    /** Drawn when you are placing a block. */
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);
        drawOverlay(x * tilesize + offset, y * tilesize + offset, rotation);
    }

    public void drawPotentialLinks(int x, int y){
        if((consumesPower || outputsPower) && hasPower && connectedPower){
            Tile tile = world.tile(x, y);
            if(tile != null){
                PowerNode.getNodeLinks(tile, this, player.team(), other -> {
                    PowerNode node = (PowerNode)other.block;
                    Draw.color(node.laserColor1, Renderer.laserOpacity * 0.5f);
                    node.drawLaser(x * tilesize + offset, y * tilesize + offset, other.x, other.y, size, other.block.size);

                    Drawf.square(other.x, other.y, other.block.size * tilesize / 2f + 2f, Pal.place);
                });
            }
        }
    }

    public float drawPlaceText(String text, int x, int y, boolean valid){
        if(renderer.pixelator.enabled()) return 0;

        Color color = valid ? Pal.accent : Pal.remove;
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f / 4f / Scl.scl(1f));
        layout.setText(font, text);

        float width = layout.width;

        font.setColor(color);
        float dx = x * tilesize + offset, dy = y * tilesize + offset + size * tilesize / 2f + 3;
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

    /** Drawn when placing and when hovering over. */
    public void drawOverlay(float x, float y, int rotation){
    }

    public float sumAttribute(@Nullable Attribute attr, int x, int y){
        if(attr == null) return 0;
        Tile tile = world.tile(x, y);
        if(tile == null) return 0;
        return tile.getLinkedTilesAs(this, tempTiles)
            .sumf(other -> !floating && other.floor().isDeep() ? 0 : other.floor().attributes.get(attr));
    }

    public TextureRegion getDisplayIcon(Tile tile){
        return tile.build == null ? uiIcon : tile.build.getDisplayIcon();
    }

    public String getDisplayName(Tile tile){
        return tile.build == null ? localizedName : tile.build.getDisplayName();
    }

    /** @return a custom minimap color for this or 0 to use default colors. */
    public int minimapColor(Tile tile){
        return 0;
    }

    public boolean outputsItems(){
        return hasItems;
    }

    /** @return whether this block can be placed on the specified tile. */
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return true;
    }

    /** @return whether this block can be broken on the specified tile. */
    public boolean canBreak(Tile tile){
        return true;
    }

    public boolean rotatedOutput(int x, int y){
        return rotate;
    }

    public boolean synthetic(){
        return update || destructible;
    }

    public boolean checkForceDark(Tile tile){
        return forceDark;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.size, "@x@", size, size);

        if(synthetic()){
            stats.add(Stat.health, health, StatUnit.none);
            if(armor > 0){
                stats.add(Stat.armor, armor, StatUnit.none);
            }
        }

        if(canBeBuilt() && requirements.length > 0){
            stats.add(Stat.buildTime, buildCost / 60, StatUnit.seconds);
            stats.add(Stat.buildCost, StatValues.items(false, requirements));
        }

        if(instantTransfer){
            stats.add(Stat.maxConsecutive, 2, StatUnit.none);
        }

        for(var c : consumers){
            c.display(stats);
        }

        //Note: Power stats are added by the consumers.
        if(hasLiquids) stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if(hasItems && itemCapacity > 0) stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
    }

    public <T extends Building> void addBar(String name, Func<T, Bar> sup){
        barMap.put(name, (Func<Building, Bar>)sup);
    }

    public void removeBar(String name){
        barMap.remove(name);
    }

    public Iterable<Func<Building, Bar>> listBars(){
        return barMap.values();
    }

    public void addLiquidBar(Liquid liq){
        addBar("liquid-" + liq.name, entity -> !liq.unlockedNow() ? null : new Bar(
            () -> liq.localizedName,
            liq::barColor,
            () -> entity.liquids.get(liq) / liquidCapacity
        ));
    }

    /** Adds a liquid bar that dynamically displays a liquid type. */
    public <T extends Building> void addLiquidBar(Func<T, Liquid> current){
        addBar("liquid", entity -> new Bar(
            () -> current.get((T)entity) == null || entity.liquids.get(current.get((T)entity)) <= 0.001f ? Core.bundle.get("bar.liquid") : current.get((T)entity).localizedName,
            () -> current.get((T)entity) == null ? Color.clear : current.get((T)entity).barColor(),
            () -> current.get((T)entity) == null ? 0f : entity.liquids.get(current.get((T)entity)) / liquidCapacity)
        );
    }

    public void setBars(){
        addBar("health", entity -> new Bar("stat.health", Pal.health, entity::healthf).blink(Color.white));

        if(consPower != null){
            boolean buffered = consPower.buffered;
            float capacity = consPower.capacity;

            addBar("power", entity -> new Bar(
                () -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * capacity))) :
                Core.bundle.get("bar.power"),
                () -> Pal.powerBar,
                () -> Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status)
            );
        }

        if(hasItems && configurable){
            addBar("items", entity -> new Bar(
                () -> Core.bundle.format("bar.items", entity.items.total()),
                () -> Pal.items,
                () -> (float)entity.items.total() / itemCapacity)
            );
        }

        if(unitCapModifier != 0){
            stats.add(Stat.maxUnits, (unitCapModifier < 0 ? "-" : "+") + Math.abs(unitCapModifier));
        }

        //liquids added last
        if(hasLiquids){
            //TODO liquids need to be handled VERY carefully. there are several potential possibilities:
            //1. no consumption or output (conduit/tank)
            // - display current(), 1 bar
            //2. static set of inputs and outputs
            // - create bars for each input/output, straightforward
            //3. TODO dynamic input/output combo???
            // - confusion

            boolean added = false;

            //TODO handle in consumer
            //add bars for *specific* consumed liquids
            for(var consl : consumers){
                if(consl instanceof ConsumeLiquid liq){
                    added = true;
                    addLiquidBar(liq.liquid);
                }else if(consl instanceof ConsumeLiquids multi){
                    added = true;
                    for(var stack : multi.liquids){
                        addLiquidBar(stack.liquid);
                    }
                }
            }

            //nothing was added, so it's safe to add a dynamic liquid bar (probably?)
            if(!added){
                addLiquidBar(build -> build.liquids.current());
            }
        }
    }

    public boolean consumesItem(Item item){
        return itemFilter[item.id];
    }

    public boolean consumesLiquid(Liquid liq){
        return liquidFilter[liq.id];
    }

    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        if(other.privileged) return false;
        return other.replaceable && (other != this || (rotate && quickRotate)) && this.group != BlockGroup.none && other.group == this.group &&
            (size == other.size || (size >= other.size && ((subclass != null && subclass == other.subclass) || group.anyReplace)));
    }

    /** @return a possible replacement for this block when placed in a line by the player. */
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> plans){
        return this;
    }

    /** Mutates the given list of points used during line placement. */
    public void changePlacementPath(Seq<Point2> points, int rotation, boolean diagonalOn){
        changePlacementPath(points, rotation);
    }

    /** Mutates the given list of points used during line placement. */
    public void changePlacementPath(Seq<Point2> points, int rotation){

    }

    /** Mutates the given list of plans used during line placement. */
    public void handlePlacementLine(Seq<BuildPlan> plans){

    }

    public boolean configSenseable(){
        return configurations.containsKey(Item.class) || configurations.containsKey(Liquid.class);
    }

    public Object nextConfig(){
        if(saveConfig && lastConfig != null){
            return lastConfig;
        }
        return null;
    }

    /** Called when a new build plan is created in the player's queue. Blocks can maintain a reference to this plan and add configs to it later. */
    public void onNewPlan(BuildPlan plan){

    }

    public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid){
        drawPlan(plan, list, valid, 1f);
    }

    public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid, float alpha){
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(alpha);
        float prevScale = Draw.scl;
        Draw.scl *= plan.animScale;
        drawPlanRegion(plan, list);
        Draw.scl = prevScale;
        Draw.reset();
    }

    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawDefaultPlanRegion(plan, list);
    }

    /** this is a different method so subclasses can call it even after overriding the base */
    public void drawDefaultPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        TextureRegion reg = getPlanRegion(plan, list);
        Draw.rect(reg, plan.drawx(), plan.drawy(), !rotate || !rotateDraw ? 0 : plan.rotation * 90);

        if(plan.worldContext && player != null && teamRegion != null && teamRegion.found()){
            if(teamRegions[player.team().id] == teamRegion) Draw.color(player.team().color);
            Draw.rect(teamRegions[player.team().id], plan.drawx(), plan.drawy());
            Draw.color();
        }

        drawPlanConfig(plan, list);
    }

    public TextureRegion getPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        return fullIcon;
    }

    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){

    }

    public void drawPlanConfigCenter(BuildPlan plan, Object content, String region, boolean cross){
        if(content == null){
            if(cross){
                Draw.rect("cross", plan.drawx(), plan.drawy());
            }
            return;
        }
        Color color = content instanceof Item i ? i.color : content instanceof Liquid l ? l.color : null;
        if(color == null) return;

        Draw.color(color);
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.color();
    }

    public void drawPlanConfigCenter(BuildPlan plan, Object content, String region){
        drawPlanConfigCenter(plan, content, region, false);
    }

    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list){

    }

    /** Transforms the internal position of this config using the specified function, and return the result. */
    public Object pointConfig(Object config, Cons<Point2> transformer){
        return config;
    }

    /** Configure when a null value is passed.*/
    public <E extends Building> void configClear(Cons<E> cons){
        configurations.put(void.class, (tile, value) -> cons.get((E)tile));
    }

    /** Listen for a config by class type. */
    public <T, E extends Building> void config(Class<T> type, Cons2<E, T> config){
        configurations.put(type, config);
    }

    public boolean isAccessible(){
        return (hasItems && itemCapacity > 0);
    }

    /** sets {@param out} to the index-th side outside of this block, using the given rotation. */
    public void nearbySide(int x, int y, int rotation, int index, Point2 out){
        int cornerX = x - (size-1)/2, cornerY = y - (size-1)/2, s = size;
        switch(rotation){
            case 0 -> out.set(cornerX + s, cornerY + index);
            case 1 -> out.set(cornerX + index, cornerY + s);
            case 2 -> out.set(cornerX - 1, cornerY + index);
            case 3 -> out.set(cornerX + index, cornerY - 1);
        }
    }

    public Point2[] getEdges(){
        return Edges.getEdges(size);
    }

    public Point2[] getInsideEdges(){
        return Edges.getInsideEdges(size);
    }

    /** Iterate through ever grid position taken up by this block. */
    public void iterateTaken(int x, int y, Intc2 placer){
        if(isMultiblock()){
            int offsetx = -(size - 1) / 2;
            int offsety = -(size - 1) / 2;

            for(int dx = 0; dx < size; dx++){
                for(int dy = 0; dy < size; dy++){
                    placer.get(dx + offsetx + x, dy + offsety + y);
                }
            }

        }else{
            placer.get(x, y);
        }
    }

    /** Never use outside of the editor! */
    public TextureRegion editorIcon(){
        return editorIcon == null ? (editorIcon = Core.atlas.find(name + "-icon-editor")) : editorIcon;
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

    /** @return special icons to outline and save with an -outline variant. Vanilla only. */
    public TextureRegion[] makeIconRegions(){
        return new TextureRegion[0];
    }

    protected TextureRegion[] icons(){
        //use team region in vanilla team blocks
        TextureRegion r = variants > 0 ? Core.atlas.find(name + "1") : region;
        return teamRegion.found() && minfo.mod == null ? new TextureRegion[]{r, teamRegions[Team.sharded.id]} : new TextureRegion[]{r};
    }

    public void getRegionsToOutline(Seq<TextureRegion> out){

    }

    public TextureRegion[] getGeneratedIcons(){
        return generatedIcons == null ? (generatedIcons = icons()) : generatedIcons;
    }

    public void resetGeneratedIcons(){
        generatedIcons = null;
    }

    public TextureRegion[] variantRegions(){
        return variantRegions == null ? (variantRegions = new TextureRegion[]{fullIcon}) : variantRegions;
    }

    public boolean hasBuilding(){
        return destructible || update;
    }

    public final Building newBuilding(){
        return buildType.get();
    }

    public void updateClipRadius(float size){
        clipSize = Math.max(clipSize, size * tilesize + size * 2f);
    }

    public Rect bounds(int x, int y, Rect rect){
        return rect.setSize(size * tilesize).setCenter(x * tilesize + offset, y * tilesize + offset);
    }

    public boolean isMultiblock(){
        return size > 1;
    }

    public boolean isVisible(){
        return !isHidden() && (state.rules.editor || (!state.rules.hideBannedBlocks || !state.rules.isBanned(this)));
    }

    public boolean isVisibleOn(Planet planet){
        return !Structs.contains(requirements, i -> planet.hiddenItems.contains(i.item));
    }

    public boolean isPlaceable(){
        return isVisible() && (!state.rules.isBanned(this) || state.rules.editor) && supportsEnv(state.rules.env);
    }

    /** @return whether this block supports a specific environment. */
    public boolean supportsEnv(int env){
        return (envEnabled & env) != 0 && (envDisabled & env) == 0 && (envRequired == 0 || (envRequired & env) == envRequired);
    }

    /** Called when building of this block begins. */
    public void placeBegan(Tile tile, Block previous){

    }

    /** Called right before building of this block begins. */
    public void beforePlaceBegan(Tile tile, Block previous){

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

    public boolean isAir(){
        return id == 0;
    }

    public boolean canBeBuilt(){
        return buildVisibility != BuildVisibility.hidden && buildVisibility != BuildVisibility.debugOnly;
    }

    public boolean environmentBuildable(){
        return (state.rules.hiddenBuildItems.isEmpty() || !Structs.contains(requirements, i -> state.rules.hiddenBuildItems.contains(i.item)));
    }

    public boolean isStatic(){
        return cacheLayer == CacheLayer.walls;
    }

    public <T extends Consume> T findConsumer(Boolf<Consume> filter){
        return consumers.length == 0 ? (T)consumeBuilder.find(filter) : (T)Structs.find(consumers, filter);
    }

    public boolean hasConsumer(Consume cons){
        return consumeBuilder.contains(cons);
    }

    public void removeConsumer(Consume cons){
        if(consumers.length > 0){
            throw new IllegalStateException("You can only remove consumers before init(). After init(), all consumers have already been initialized.");
        }
        consumeBuilder.remove(cons);
    }

    public ConsumeLiquid consumeLiquid(Liquid liquid, float amount){
        return consume(new ConsumeLiquid(liquid, amount));
    }

    public ConsumeLiquids consumeLiquids(LiquidStack... stacks){
        return consume(new ConsumeLiquids(stacks));
    }

    /**
     * Creates a consumer which directly uses power without buffering it.
     * @param powerPerTick The amount of power which is required each tick for 100% efficiency.
     * @return the created consumer object.
     */
    public ConsumePower consumePower(float powerPerTick){
        return consume(new ConsumePower(powerPerTick, 0.0f, false));
    }

    /** Creates a consumer which only consumes power when the condition is met. */
    public <T extends Building> ConsumePower consumePowerCond(float usage, Boolf<T> cons){
        return consume(new ConsumePowerCondition(usage, (Boolf<Building>)cons));
    }

    /** Creates a consumer that consumes a dynamic amount of power. */
    public <T extends Building> ConsumePower consumePowerDynamic(Floatf<T> usage){
        return consume(new ConsumePowerDynamic((Floatf<Building>)usage));
    }

    /**
     * Creates a consumer which stores power.
     * @param powerCapacity The maximum capacity in power units.
     */
    public ConsumePower consumePowerBuffered(float powerCapacity){
        return consume(new ConsumePower(0f, powerCapacity, true));
    }

    public ConsumeItems consumeItem(Item item){
        return consumeItem(item, 1);
    }

    public ConsumeItems consumeItem(Item item, int amount){
        return consume(new ConsumeItems(new ItemStack[]{new ItemStack(item, amount)}));
    }

    public ConsumeItems consumeItems(ItemStack... items){
        return consume(new ConsumeItems(items));
    }

    public ConsumeCoolant consumeCoolant(float amount){
        return consume(new ConsumeCoolant(amount));
    }

    public <T extends Consume> T consume(T consume){
        if(consume instanceof ConsumePower){
            //there can only be one power consumer
            consumeBuilder.removeAll(b -> b instanceof ConsumePower);
            consPower = (ConsumePower)consume;
        }
        consumeBuilder.add(consume);
        return consume;
    }

    public void setupRequirements(Category cat, ItemStack[] stacks){
        requirements(cat, stacks);
    }

    public void setupRequirements(Category cat, BuildVisibility visible, ItemStack[] stacks){
        requirements(cat, visible, stacks);
    }

    public void requirements(Category cat, ItemStack[] stacks, boolean unlocked){
        requirements(cat, BuildVisibility.shown, stacks);
        this.alwaysUnlocked = unlocked;
    }

    public void requirements(Category cat, ItemStack[] stacks){
        requirements(cat, BuildVisibility.shown, stacks);
    }

    /** Sets up requirements. Use only this method to set up requirements. */
    public void requirements(Category cat, BuildVisibility visible, ItemStack[] stacks){
        this.category = cat;
        this.requirements = stacks;
        this.buildVisibility = visible;

        Arrays.sort(requirements, Structs.comparingInt(i -> i.item.id));
    }

    protected void initBuilding(){
        //attempt to find the first declared class and use it as the entity type
        try{
            Class<?> current = getClass();

            if(current.isAnonymousClass()){
                current = current.getSuperclass();
            }

            subclass = current;

            while(buildType == null && Block.class.isAssignableFrom(current)){
                //first class that is subclass of Building
                Class<?> type = Structs.find(current.getDeclaredClasses(), t -> Building.class.isAssignableFrom(t) && !t.isInterface());
                if(type != null){
                    //these are inner classes, so they have an implicit parameter generated
                    Constructor<? extends Building> cons = (Constructor<? extends Building>)type.getDeclaredConstructor(type.getDeclaringClass());
                    buildType = () -> {
                        try{
                            return cons.newInstance(this);
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    };
                }

                //scan through every superclass looking for it
                current = current.getSuperclass();
            }

        }catch(Throwable ignored){
        }

        if(buildType == null){
            //assign default value
            buildType = Building::create;
        }
    }

    @Override
    public ItemStack[] researchRequirements(){
        if(researchCost != null) return researchCost;
        if(researchCostMultiplier <= 0f) return ItemStack.empty;
        ItemStack[] out = new ItemStack[requirements.length];
        for(int i = 0; i < out.length; i++){
            int quantity = Mathf.round(60 * researchCostMultiplier + Mathf.pow(requirements[i].amount, 1.11f) * 20 * researchCostMultiplier * researchCostMultipliers.get(requirements[i].item, 1f), 10);

            out[i] = new ItemStack(requirements[i].item, UI.roundAmount(quantity));
        }

        return out;
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        //just requires items
        for(ItemStack stack : requirements){
            cons.get(stack.item);
        }

        //also requires inputs
        for(var c : consumeBuilder){
            if(c.optional) continue;

            if(c instanceof ConsumeItems i){
                for(ItemStack stack : i.items){
                    cons.get(stack.item);
                }
            }
            //TODO: requiring liquid dependencies is usually a bad idea, because there is no reason to pump/produce something until you actually need it.
            /*else if(c instanceof ConsumeLiquid i){
                cons.get(i.liquid);
            }else if(c instanceof ConsumeLiquids i){
                for(var stack : i.liquids){
                    cons.get(stack.liquid);
                }
            }*/
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.block;
    }

    @Override
    public boolean logicVisible(){
        return buildVisibility != BuildVisibility.hidden;
    }

    /** Called after all blocks are created. */
    @Override
    @CallSuper
    public void init(){
        //disable standard shadow
        if(customShadow){
            hasShadow = false;
        }

        if(fogRadius > 0){
            flags = flags.with(BlockFlag.hasFogRadius);
        }

        //initialize default health based on size
        if(health == -1){
            boolean round = false;
            if(scaledHealth < 0){
                scaledHealth = 40;

                float scaling = 1f;
                for(var stack : requirements){
                    scaling += stack.item.healthScaling;
                }

                scaledHealth *= scaling;
                round = true;
            }

            health = round ?
                Mathf.round(size * size * scaledHealth, 5) :
                (int)(size * size * scaledHealth);
        }

        clipSize = Math.max(clipSize, size * tilesize);

        if(hasLiquids && drawLiquidLight){
            clipSize = Math.max(size * 30f * 2f, clipSize);
        }

        if(emitLight){
            clipSize = Math.max(clipSize, lightRadius * 2f);
        }

        if(group == BlockGroup.transportation || category == Category.distribution){
            acceptsItems = true;
        }

        offset = ((size + 1) % 2) * tilesize / 2f;
        sizeOffset = -((size - 1) / 2);

        if(requirements.length > 0){
            buildCost = 0f;
            for(ItemStack stack : requirements){
                buildCost += stack.amount * stack.item.cost;
            }
        }

        buildCost *= buildCostMultiplier;

        consumers = consumeBuilder.toArray(Consume.class);
        optionalConsumers = consumeBuilder.select(consume -> consume.optional && !consume.ignore()).toArray(Consume.class);
        nonOptionalConsumers = consumeBuilder.select(consume -> !consume.optional && !consume.ignore()).toArray(Consume.class);
        updateConsumers = consumeBuilder.select(consume -> consume.update && !consume.ignore()).toArray(Consume.class);
        hasConsumers = consumers.length > 0;
        itemFilter = new boolean[content.items().size];
        liquidFilter = new boolean[content.liquids().size];

        for(Consume cons : consumers){
            cons.apply(this);
        }

        setBars();

        stats.useCategories = true;

        //TODO check for double power consumption

        if(!logicConfigurable){
            configurations.each((key, val) -> {
                if(UnlockableContent.class.isAssignableFrom(key)){
                    logicConfigurable = true;
                }
            });
        }

        if(!outputsPower && consPower != null && consPower.buffered){
            Log.warn("Consumer using buffered power: @. Disabling buffered power.", name);
            consPower.buffered = false;
        }

        if(buildVisibility == BuildVisibility.sandboxOnly){
            hideDetails = false;
        }
    }

    @Override
    public void load(){
        super.load();

        region = Core.atlas.find(name);

        ContentRegions.loadRegions(this);

        //load specific team regions
        teamRegions = new TextureRegion[Team.all.length];
        for(Team team : Team.all){
            teamRegions[team.id] = teamRegion.found() && team.hasPalette ? Core.atlas.find(name + "-team-" + team.name, teamRegion) : teamRegion;
        }

        if(variants != 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Core.atlas.find(name + (i + 1));
            }
            region = variantRegions[0];

            if(customShadow){
                variantShadowRegions = new TextureRegion[variants];
                for(int i = 0; i < variants; i++){
                    variantShadowRegions[i] = Core.atlas.find(name + "-shadow" + (i + 1));
                }
            }
        }
    }

    @Override
    public boolean isHidden(){
        return !buildVisibility.visible() && !state.rules.revealedBlocks.contains(this);
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        if(!synthetic()){
            PixmapRegion image = Core.atlas.getPixmap(fullIcon);
            mapColor.set(image.get(image.width/2, image.height/2));
        }

        if(variants > 0){
            for(int i = 0; i < variants; i++){
                String rname = name + (i + 1);
                packer.add(PageType.editor, "editor-" + rname, Core.atlas.getPixmap(rname));
            }
        }

        //generate paletted team regions
        if(teamRegion != null && teamRegion.found()){
            for(Team team : Team.all){
                //if there's an override, don't generate anything
                if(team.hasPalette && !Core.atlas.has(name + "-team-" + team.name)){
                    var base = Core.atlas.getPixmap(teamRegion);
                    Pixmap out = new Pixmap(base.width, base.height);

                    for(int x = 0; x < base.width; x++){
                        for(int y = 0; y < base.height; y++){
                            int color = base.get(x, y);
                            int index = switch(color){
                                case 0xffffffff -> 0;
                                case 0xdcc6c6ff, 0xdbc5c5ff -> 1;
                                case 0x9d7f7fff, 0x9e8080ff -> 2;
                                default -> -1;
                            };
                            out.setRaw(x, y, index == -1 ? base.get(x, y) : team.palettei[index]);
                        }
                    }

                    Drawf.checkBleed(out);

                    packer.add(PageType.main, name + "-team-" + team.name, out);
                }
            }

            teamRegions = new TextureRegion[Team.all.length];
            for(Team team : Team.all){
                teamRegions[team.id] = teamRegion.found() && team.hasPalette ? Core.atlas.find(name + "-team-" + team.name, teamRegion) : teamRegion;
            }
        }

        Pixmap last = null;

        var gen = icons();

        if(outlineIcon){
            AtlasRegion atlasRegion = (AtlasRegion)gen[outlinedIcon >= 0 ? Math.min(outlinedIcon, gen.length - 1) : gen.length -1];
            PixmapRegion region = Core.atlas.getPixmap(atlasRegion);
            Pixmap out = last = Pixmaps.outline(region, outlineColor, outlineRadius);
            Drawf.checkBleed(out);
            packer.add(PageType.main, atlasRegion.name, out);
        }

        var toOutline = new Seq<TextureRegion>();
        getRegionsToOutline(toOutline);

        for(var region : toOutline){
            if(region instanceof AtlasRegion atlas){
                String regionName = atlas.name;
                Pixmap outlined = Pixmaps.outline(Core.atlas.getPixmap(region), outlineColor, outlineRadius);

                Drawf.checkBleed(outlined);

                packer.add(PageType.main, regionName + "-outline", outlined);
            }
        }

        PixmapRegion editorBase;

        if(gen.length > 1){
            Pixmap base = Core.atlas.getPixmap(gen[0]).crop();
            for(int i = 1; i < gen.length; i++){
                if(i == gen.length - 1 && last != null){
                    base.draw(last, 0, 0, true);
                }else{
                    base.draw(Core.atlas.getPixmap(gen[i]), true);
                }
            }
            packer.add(PageType.main, "block-" + name + "-full", base);

            editorBase = new PixmapRegion(base);
        }else{
            if(gen[0] != null) packer.add(PageType.main, "block-" + name + "-full", Core.atlas.getPixmap(gen[0]));
            editorBase = gen[0] == null ? Core.atlas.getPixmap(fullIcon) : Core.atlas.getPixmap(gen[0]);
        }

        packer.add(PageType.editor, name + "-icon-editor", editorBase);
    }

    public void flipRotation(BuildPlan req, boolean x){
        if((x == (req.rotation % 2 == 0)) != invertFlip){
            req.rotation = Mathf.mod(req.rotation + 2, 4);
        }
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case color -> mapColor.toDoubleBits();
            case health, maxHealth -> health;
            case size -> size * tilesize;
            case itemCapacity -> itemCapacity;
            case liquidCapacity -> liquidCapacity;
            case powerCapacity -> consPower != null && consPower.buffered ? consPower.capacity : 0f;
            default -> Double.NaN;
        };
    }

    @Override
    public double sense(Content content){
        return Double.NaN;
    }

    @Override
    public Object senseObject(LAccess sensor){
        if(sensor == LAccess.name) return name;
        return noSensed;
    }
}
