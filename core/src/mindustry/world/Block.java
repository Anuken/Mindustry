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
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import java.lang.reflect.*;
import java.util.*;

import static mindustry.Vars.*;

public class Block extends UnlockableContent{
    public boolean hasItems;
    public boolean hasLiquids;
    public boolean hasPower;

    public boolean outputsLiquid = false;
    public boolean consumesPower = true;
    public boolean outputsPower = false;
    public boolean outputsPayload = false;
    public boolean outputFacing = true;
    public boolean acceptsItems = false;

    public int itemCapacity = 10;
    public float liquidCapacity = 10f;
    public float liquidPressure = 1f;

    public final BlockBars bars = new BlockBars();
    public final Consumers consumes = new Consumers();

    /** whether to display flow rate */
    public boolean displayFlow = true;
    /** whether this block is visible in the editor */
    public boolean inEditor = true;
    /** the last configuration value applied to this block. */
    public @Nullable Object lastConfig;
    /** whether to save the last config and apply it to newly placed blocks */
    public boolean saveConfig = false;
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
    /** for static blocks only: if true, tile data() is saved in world data. */
    public boolean saveData;
    /** whether you can break this with rightclick */
    public boolean breakable;
    /** whether to add this block to brokenblocks */
    public boolean rebuildable = true;
    /** whether this block can only be placed on water */
    public boolean requiresWater = false;
    /** whether this block can be placed on any liquids, anywhere */
    public boolean placeableLiquid = false;
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
    /** Whether to use this block's color in the minimap. Only used for overlays. */
    public boolean useColor = true;
    /** tile entity health */
    public int health = -1;
    /** base block explosiveness */
    public float baseExplosiveness = 0f;
    /** whether this block can be placed on edges of liquids. */
    public boolean floating = false;
    /** multiblock size */
    public int size = 1;
    /** multiblock offset */
    public float offset = 0f;
    /** Whether to draw this block in the expanded draw range. */
    public boolean expanded = false;
    /** Max of timers used. */
    public int timers = 0;
    /** Cache layer. Only used for 'cached' rendering. */
    public CacheLayer cacheLayer = CacheLayer.normal;
    /** Special flag; if false, floor will be drawn under this block even if it is cached. */
    public boolean fillsTile = true;
    /** whether this block can be replaced in all cases */
    public boolean alwaysReplace = false;
    /** The block group. Unless {@link #canReplace} is overriden, blocks in the same group can replace each other. */
    public BlockGroup group = BlockGroup.none;
    /** List of block flags. Used for AI indexing. */
    public EnumSet<BlockFlag> flags = EnumSet.of();
    /** Targeting priority of this block, as seen by enemies .*/
    public TargetPriority priority = TargetPriority.base;
    /** How much this block affects the unit cap by.
     * The block flags must contain unitModifier in order for this to work. */
    public int unitCapModifier = 0;
    /** Whether the block can be tapped and selected to configure. */
    public boolean configurable;
    /** If true, this block can be configured by logic. */
    public boolean logicConfigurable = false;
    /** Whether this block consumes touchDown events when tapped. */
    public boolean consumesTap;
    /** Whether to draw the glow of the liquid for this block, if it has one. */
    public boolean drawLiquidLight = true;
    /** Whether to periodically sync this block across the network. */
    public boolean sync;
    /** Whether this block uses conveyor-type placement mode. */
    public boolean conveyorPlacement;
    /** Whether to swap the diagonal placement modes. */
    public boolean swapDiagonalPlacement;
    /**
     * The color of this block when displayed on the minimap or map preview.
     * Do not set manually! This is overridden when loading for most blocks.
     */
    public Color mapColor = new Color(0, 0, 0, 1);
    /** Whether this block has a minimap color. */
    public boolean hasColor = false;
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
    /** How reflective this block is. */
    public float albedo = 0f;
    /** Environmental passive light color. */
    public Color lightColor = Color.white.cpy();
    /**
     * Whether this environmental block passively emits light.
     * Not valid for non-environmental blocks. */
    public boolean emitLight = false;
    /** Radius of the light emitted by this block. */
    public float lightRadius = 60f;

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
    /** Cost of building this block; do not modify directly! */
    public float buildCost;
    /** Whether this block is visible and can currently be built. */
    public BuildVisibility buildVisibility = BuildVisibility.hidden;
    /** Multiplier for speed of building this block. */
    public float buildCostMultiplier = 1f;
    /** Build completion at which deconstruction finishes. */
    public float deconstructThreshold = 0f;
    /** Multiplier for cost of research in tech tree. */
    public float researchCostMultiplier = 1;
    /** Whether this block has instant transfer.*/
    public boolean instantTransfer = false;
    /** Whether you can rotate this block with Keybind rotateplaced + Scroll Wheel. */
    public boolean quickRotate = true;
    /** Main subclass. Non-anonymous. */
    public @Nullable Class<?> subclass;

    public Prov<Building> buildType = null; //initialized later
    public ObjectMap<Class<?>, Cons2> configurations = new ObjectMap<>();

    protected TextureRegion[] generatedIcons;
    protected TextureRegion[] variantRegions, editorVariantRegions;

    public TextureRegion region, editorIcon;
    public @Load("@-team") TextureRegion teamRegion;
    public TextureRegion[] teamRegions;

    protected static final Seq<Tile> tempTiles = new Seq<>();
    protected static final Seq<Building> tempTileEnts = new Seq<>();

    /** Dump timer ID.*/
    protected final int timerDump = timers++;
    /** How often to try dumping items in ticks, e.g. 5 = 12 times/sec*/
    protected final int dumpTime = 5;

    public Block(String name){
        super(name);
        initBuilding();
    }

    public void drawBase(Tile tile){
        //delegates to entity unless it is null
        if(tile.build != null){
            tile.build.draw();
        }else{
            Draw.rect(region, tile.drawx(), tile.drawy());
        }
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

    public float sumAttribute(@Nullable Attribute attr, int x, int y){
        if(attr == null) return 0;
        Tile tile = world.tile(x, y);
        if(tile == null) return 0;
        return tile.getLinkedTilesAs(this, tempTiles)
            .sumf(other -> !floating && other.floor().isDeep() ? 0 : other.floor().attributes.get(attr));
    }

    public TextureRegion getDisplayIcon(Tile tile){
        return tile.build == null ? icon(Cicon.medium) : tile.build.getDisplayIcon();
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

    /** Returns whether or not this block can be place on the specified  */
    public boolean canPlaceOn(Tile tile, Team team){
        return true;
    }

    public boolean canBreak(Tile tile){
        return true;
    }

    public boolean rotatedOutput(int x, int y){
        return rotate;
    }

    public boolean synthetic(){
        return update || destructible;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.size, "@x@", size, size);
        stats.add(Stat.health, health, StatUnit.none);
        if(canBeBuilt()){
            stats.add(Stat.buildTime, buildCost / 60, StatUnit.seconds);
            stats.add(Stat.buildCost, new ItemListValue(false, requirements));
        }

        if(instantTransfer){
            stats.add(Stat.maxConsecutive, 2, StatUnit.none);
        }

        consumes.display(stats);

        //Note: Power stats are added by the consumers.
        if(hasLiquids) stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if(hasItems && itemCapacity > 0) stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
    }

    public void setBars(){
        bars.add("health", entity -> new Bar("stat.health", Pal.health, entity::healthf).blink(Color.white));

        if(hasLiquids){
            Func<Building, Liquid> current;
            if(consumes.has(ConsumeType.liquid) && consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid){
                Liquid liquid = consumes.<ConsumeLiquid>get(ConsumeType.liquid).liquid;
                current = entity -> liquid;
            }else{
                current = entity -> entity.liquids == null ? Liquids.water : entity.liquids.current();
            }
            bars.add("liquid", entity -> new Bar(() -> entity.liquids.get(current.get(entity)) <= 0.001f ? Core.bundle.get("bar.liquid") : current.get(entity).localizedName,
            () -> current.get(entity).barColor(), () -> entity == null || entity.liquids == null ? 0f : entity.liquids.get(current.get(entity)) / liquidCapacity));
        }

        if(hasPower && consumes.hasPower()){
            ConsumePower cons = consumes.getPower();
            boolean buffered = cons.buffered;
            float capacity = cons.capacity;

            bars.add("power", entity -> new Bar(() -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : (int)(entity.power.status * capacity)) :
                Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> Mathf.zero(cons.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status));
        }

        if(hasItems && configurable){
            bars.add("items", entity -> new Bar(() -> Core.bundle.format("bar.items", entity.items.total()), () -> Pal.items, () -> (float)entity.items.total() / itemCapacity));
        }
        
        if(flags.contains(BlockFlag.unitModifier)) stats.add(Stat.maxUnits, (unitCapModifier < 0 ? "-" : "+") + Math.abs(unitCapModifier));
    }

    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        return (other != this || rotate) && this.group != BlockGroup.none && other.group == this.group &&
            (size == other.size || (size >= other.size && ((subclass != null && subclass == other.subclass) || group.anyReplace)));
    }

    /** @return a possible replacement for this block when placed in a line by the player. */
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> requests){
        return this;
    }

    /** Mutates the given list of points used during line placement. */
    public void changePlacementPath(Seq<Point2> points, int rotation){

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

    public void drawPlan(BuildPlan req, Eachable<BuildPlan> list, boolean valid){
        drawPlan(req, list, valid, 1f);
    }

    public void drawPlan(BuildPlan req, Eachable<BuildPlan> list, boolean valid, float alpha){
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(alpha);
        float prevScale = Draw.scl;
        Draw.scl *= req.animScale;
        drawRequestRegion(req, list);
        Draw.scl = prevScale;
        Draw.reset();
    }

    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        TextureRegion reg = getRequestRegion(req, list);
        Draw.rect(reg, req.drawx(), req.drawy(), !rotate ? 0 : req.rotation * 90);

        drawRequestConfig(req, list);
    }

    public TextureRegion getRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        return icon(Cicon.full);
    }

    public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){

    }

    public void drawRequestConfigCenter(BuildPlan req, Object content, String region, boolean cross){
        if(content == null){
            if(cross){
                Draw.rect("cross", req.drawx(), req.drawy());
            }
            return;
        }
        Color color = content instanceof Item i ? i.color : content instanceof Liquid l ? l.color : null;
        if(color == null) return;

        Draw.color(color);
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.color();
    }

    public void drawRequestConfigCenter(BuildPlan req, Object content, String region){
        drawRequestConfigCenter(req, content, region, false);
    }

    public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){

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

    protected TextureRegion[] icons(){
        //use team region in vanilla team blocks
        return teamRegion.found() && minfo.mod == null ? new TextureRegion[]{region, teamRegions[Team.sharded.id]} : new TextureRegion[]{region};
    }

    public TextureRegion[] getGeneratedIcons(){
        return generatedIcons == null ? (generatedIcons = icons()) : generatedIcons;
    }

    public TextureRegion[] variantRegions(){
        return variantRegions == null ? (variantRegions = new TextureRegion[]{icon(Cicon.full)}) : variantRegions;
    }

    public boolean hasBuilding(){
        return destructible || update;
    }

    public final Building newBuilding(){
        return buildType.get();
    }

    public Rect bounds(int x, int y, Rect rect){
        return rect.setSize(size * tilesize).setCenter(x * tilesize + offset, y * tilesize + offset);
    }

    public boolean isMultiblock(){
        return size > 1;
    }

    public boolean isVisible(){
        return !isHidden();
    }

    public boolean isPlaceable(){
        return isVisible() && !state.rules.bannedBlocks.contains(this);
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

    public boolean isStatic(){
        return cacheLayer == CacheLayer.walls;
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
        ItemStack[] out = new ItemStack[requirements.length];
        for(int i = 0; i < out.length; i++){
            int quantity = 60 + Mathf.round(Mathf.pow(requirements[i].amount, 1.1f) * 20 * researchCostMultiplier, 10);

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
        consumes.each(c -> {
            if(c.isOptional()) return;

            if(c instanceof ConsumeItems i){
                for(ItemStack stack : i.items){
                    cons.get(stack.item);
                }
            }else if(c instanceof ConsumeLiquid i){
                cons.get(i.liquid);
            }
        });
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

        if(group == BlockGroup.transportation || consumes.has(ConsumeType.item) || category == Category.distribution){
            acceptsItems = true;
        }

        offset = ((size + 1) % 2) * tilesize / 2f;

        buildCost = 0f;
        for(ItemStack stack : requirements){
            buildCost += stack.amount * stack.item.cost;
        }
        buildCost *= buildCostMultiplier;

        if(consumes.has(ConsumeType.power)) hasPower = true;
        if(consumes.has(ConsumeType.item)) hasItems = true;
        if(consumes.has(ConsumeType.liquid)) hasLiquids = true;

        setBars();

        stats.useCategories = true;

        consumes.init();

        if(!logicConfigurable){
            configurations.each((key, val) -> {
                if(UnlockableContent.class.isAssignableFrom(key)){
                    logicConfigurable = true;
                }
            });
        }

        if(!outputsPower && consumes.hasPower() && consumes.getPower().buffered){
            throw new IllegalArgumentException("Consumer using buffered power: " + name);
        }
    }

    @CallSuper
    @Override
    public void load(){
        region = Core.atlas.find(name);

        ContentRegions.loadRegions(this);

        //load specific team regions
        teamRegions = new TextureRegion[Team.all.length];
        for(Team team : Team.all){
            teamRegions[team.id] = teamRegion.found() ? Core.atlas.find(name + "-team-" + team.name, teamRegion) : teamRegion;
        }
    }

    @Override
    public boolean isHidden(){
        return !buildVisibility.visible() && !state.rules.revealedBlocks.contains(this);
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        packer.add(PageType.editor, name + "-icon-editor", Core.atlas.getPixmap((AtlasRegion)icon(Cicon.full)));

        if(!synthetic()){
            PixmapRegion image = Core.atlas.getPixmap((AtlasRegion)icon(Cicon.full));
            mapColor.set(image.getPixel(image.width/2, image.height/2));
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
                                if(Structs.inBounds(rx + x, ry + y, region.width, region.height) && Mathf.within(rx, ry, radius) && color.set(region.getPixel(rx + x, ry + y)).a > 0.01f){
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

            packer.add(PageType.main, name, out);
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
            packer.add(PageType.main, "block-" + name + "-full", base);
            generatedIcons = null;
            Arrays.fill(cicons, null);
        }
    }

}
