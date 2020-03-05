package mindustry.entities.def;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

@EntityDef(value = {Tilec.class}, isFinal = false, genio = false, serialize = false)
@Component
abstract class TileComp implements Posc, Teamc, Healthc, Tilec, Timerc{
    //region vars and initialization
    static final float timeToSleep = 60f * 1;
    static final ObjectSet<Tilec> tmpTiles = new ObjectSet<>();
    static int sleepingEntities = 0;

    transient Tile tile;
    transient Block block;
    transient Array<Tilec> proximity = new Array<>(8);

    PowerModule power;
    ItemModule items;
    LiquidModule liquids;
    ConsumeModule cons;

    private transient float timeScale = 1f, timeScaleDuration;

    private transient @Nullable SoundLoop sound;

    private transient boolean sleeping;
    private transient float sleepTime;

    /** Sets this tile entity data to this tile, and adds it if necessary. */
    public Tilec init(Tile tile, boolean shouldAdd){
        this.tile = tile;
        this.block = tile.block();

        set(tile.drawx(), tile.drawy());
        if(block.activeSound != Sounds.none){
            sound = new SoundLoop(block.activeSound, block.activeSoundVolume);
        }

        health(block.health);
        maxHealth(block.health);
        timer(new Interval(block.timers));

        if(shouldAdd){
            add();
        }

        return this;
    }

    //endregion
    //region io

    public final void writeBase(Writes write){
        write.f(health());
        write.b(tile.rotation());
        write.b(tile.getTeamID());
        if(items != null) items.write(write);
        if(power != null) power.write(write);
        if(liquids != null) liquids.write(write);
        if(cons != null) cons.write(write);
    }

    public final void readBase(Reads read){
        health(read.f());
        tile.rotation(read.b());
        tile.setTeam(Team.get(read.b()));
        if(items != null) items.read(read);
        if(power != null) power.read(read);
        if(liquids != null) liquids.read(read);
        if(cons != null) cons.read(read);
    }

    public void writeAll(Writes write){
        writeBase(write);
        write(write);
    }

    public void readAll(Reads read, byte revision){
        readBase(read);
        read(read, revision);
    }

    @CallSuper
    public void write(Writes write){
        //overriden by subclasses!
    }

    @CallSuper
    public void read(Reads read, byte revision){
        //overriden by subclasses!
    }

    //endregion
    //region utility methods

    public void applyBoost(float intensity, float  duration){
        timeScale = Math.max(timeScale, intensity);
        timeScaleDuration = Math.max(timeScaleDuration, duration);
    }

    public int pos(){
        return tile.pos();
    }

    public int rotation(){
        return tile.rotation();
    }

    public void rotation(int rotation){
        tile.rotation(rotation);
    }

    public Floor floor(){
        return tile.floor();
    }

    public boolean interactable(Team team){
        return state.teams.canInteract(team, team());
    }

    public float timeScale(){
        return timeScale;
    }

    public boolean consValid(){
        return cons.valid();
    }

    public void consume(){
        cons.trigger();
    }

    /** Scaled delta. */
    public float delta(){
        return Time.delta() * timeScale;
    }

    /** Base efficiency. If this entity has non-buffered power, returns the power %, otherwise returns 1. */
    public float efficiency(){
        return power != null && (block.consumes.has(ConsumeType.power) && !block.consumes.getPower().buffered) ? power.status : 1f;
    }

    /** Call when nothing is happening to the entity. This increments the internal sleep timer. */
    public void sleep(){
        sleepTime += Time.delta();
        if(!sleeping && sleepTime >= timeToSleep){
            remove();
            sleeping = true;
            sleepingEntities++;
        }
    }

    /** Call when this entity is updating. This wakes it up. */
    public void noSleep(){
        sleepTime = 0f;
        if(sleeping){
            add();
            sleeping = false;
            sleepingEntities--;
        }
    }

    /** Returns the version of this TileEntity IO code.*/
    public byte version(){
        return 0;
    }

    //endregion
    //region handler methods


    public boolean shouldConsume(Tilec tile){
        return true;
    }

    public boolean productionValid(Tilec tile){
        return true;
    }

    public float getPowerProduction(Tilec tile){
        return 0f;
    }

    /** Returns the amount of items this block can accept. */
    public int acceptStack(Tilec tile, Item item, int amount, Teamc source){
        if(acceptItem(tile, tile, item) && hasItems && (source == null || source.team() == tile.team())){
            return Math.min(getMaximumAccepted(tile, item) - tile.items().get(item), amount);
        }else{
            return 0;
        }
    }

    public int getMaximumAccepted(Tilec tile, Item item){
        return itemCapacity;
    }

    /** Remove a stack from this inventory, and return the amount removed. */
    public int removeStack(Tilec tile, Item item, int amount){
        if(tile.entity == null || tile.items() == null) return 0;
        amount = Math.min(amount, tile.items().get(item));
        tile.noSleep();
        tile.items().remove(item, amount);
        return amount;
    }

    /** Handle a stack input. */
    public void handleStack(Tilec tile, Item item, int amount, Teamc source){
        tile.noSleep();
        tile.items().add(item, amount);
    }

    public boolean outputsItems(){
        return hasItems;
    }

    /** Returns offset for stack placement. */
    public void getStackOffset(Tilec tile, Item item, Vec2 trns){

    }

    public void onProximityUpdate(Tilec tile){
        tile.noSleep();
    }

    public void handleItem(Tilec tile, Tilec source, Item item){
        tile.items().add(item, 1);
    }

    public boolean acceptItem(Tilec tile, Tilec source, Item item){
        return consumes.itemFilters.get(item.id) && tile.items().get(item) < getMaximumAccepted(tile, item);
    }

    public boolean acceptLiquid(Tilec tile, Tilec source, Liquid liquid, float amount){
        return hasLiquids && tile.liquids().get(liquid) + amount < liquidCapacity && consumes.liquidfilters.get(liquid.id);
    }

    public void handleLiquid(Tilec tile, Tilec source, Liquid liquid, float amount){
        tile.liquids().add(liquid, amount);
    }

    public void tryDumpLiquid(Tilec tile, Liquid liquid){
        Array<Tilec> proximity = tile.proximity();
        int dump = tile.rotation();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(tile, proximity.size);
            Tilec other = proximity.get((i + dump) % proximity.size);
            //TODO fix, this is incorrect
            Tilec in = Edges.getFacingEdge(tile.tile(), other.tile()).entity;

            other = other.block().getLiquidDestination(other, in, liquid);

            if(other != null && other.team() == tile.team() && other.block().hasLiquids && canDumpLiquid(tile, other, liquid) && other.liquids() != null){
                float ofract = other.liquids().get(liquid) / other.block().liquidCapacity;
                float fract = tile.liquids().get(liquid) / liquidCapacity;

                if(ofract < fract) tryMoveLiquid(tile, in, other, (fract - ofract) * liquidCapacity / 2f, liquid);
            }
        }

    }

    public boolean canDumpLiquid(Tilec tile, Tilec to, Liquid liquid){
        return true;
    }

    public void tryMoveLiquid(Tilec tile, Tilec tileSource, Tilec next, float amount, Liquid liquid){
        float flow = Math.min(next.block().liquidCapacity - next.liquids().get(liquid) - 0.001f, amount);

        if(next.block().acceptLiquid(next, tileSource, liquid, flow)){
            next.block().handleLiquid(next, tileSource, liquid, flow);
            tile.liquids().remove(liquid, flow);
        }
    }

    public float tryMoveLiquid(Tilec tile, Tilec next, boolean leak, Liquid liquid){
        return tryMoveLiquid(tile, next, leak ? 1.5f : 100, liquid);
    }

    public float tryMoveLiquid(Tilec tile, Tilec next, float leakResistance, Liquid liquid){
        if(next == null) return 0;

        next = next.block().getLiquidDestination(next, tile, liquid);

        if(next.team() == tile.team() && next.block().hasLiquids && tile.liquids().get(liquid) > 0f){

            if(next.block().acceptLiquid(next, tile, liquid, 0f)){
                float ofract = next.liquids().get(liquid) / next.block().liquidCapacity;
                float fract = tile.liquids().get(liquid) / liquidCapacity * liquidPressure;
                float flow = Math.min(Mathf.clamp((fract - ofract) * (1f)) * (liquidCapacity), tile.liquids().get(liquid));
                flow = Math.min(flow, next.block().liquidCapacity - next.liquids().get(liquid) - 0.001f);

                if(flow > 0f && ofract <= fract && next.block().acceptLiquid(next, tile, liquid, flow)){
                    next.block().handleLiquid(next, tile, liquid, flow);
                    tile.liquids().remove(liquid, flow);
                    return flow;
                }else if(ofract > 0.1f && fract > 0.1f){
                    //TODO these are incorrect effect positions
                    float fx = (tile.x() + next.x()) / 2f, fy = (tile.y() + next.y()) / 2f;

                    Liquid other = next.liquids().current();
                    if((other.flammability > 0.3f && liquid.temperature > 0.7f) || (liquid.flammability > 0.3f && other.temperature > 0.7f)){
                        tile.damage(1 * Time.delta());
                        next.damage(1 * Time.delta());
                        if(Mathf.chance(0.1 * Time.delta())){
                            Fx.fire.at(fx, fy);
                        }
                    }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) || (other.temperature > 0.7f && liquid.temperature < 0.55f)){
                        tile.liquids().remove(liquid, Math.min(tile.liquids().get(liquid), 0.7f * Time.delta()));
                        if(Mathf.chance(0.2f * Time.delta())){
                            Fx.steam.at(fx, fy);
                        }
                    }
                }
            }
        }else if(leakResistance != 100f && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = tile.liquids().get(liquid) / leakResistance;
            Puddles.deposit(next.tile(), tile.tile(), liquid, leakAmount);
            tile.liquids().remove(liquid, leakAmount);
        }
        return 0;
    }

    public Tilec getLiquidDestination(Tilec tile, Tilec from, Liquid liquid){
        return tile;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.
     */
    public void offloadNear(Tilec tile, Item item){
        Array<Tilec> proximity = tile.proximity();
        int dump = tile.rotation();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(tile, proximity.size);
            Tilec other = proximity.get((i + dump) % proximity.size);
            //TODO fix position
            Tilec in = Edges.getFacingEdge(tile.tile(), other.tile()).entity;
            if(other.team() == tile.team() && other.block().acceptItem(other, in, item) && canDump(tile, other, item)){
                other.block().handleItem(other, in, item);
                return;
            }
        }

        handleItem(tile, tile, item);
    }

    /** Try dumping any item near the tile. */
    public boolean tryDump(Tilec tile){
        return tryDump(tile, null);
    }

    /**
     * Try dumping a specific item near the tile.
     * @param todump Item to dump. Can be null to dump anything.
     */
    public boolean tryDump(Tilec entity, Item todump){
        if(entity == null || !hasItems || entity.items().total() == 0 || (todump != null && !entity.items().has(todump)))
            return false;

        Array<Tilec> proximity = entity.proximity();
        int dump = entity.rotation();

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Tilec other = proximity.get((i + dump) % proximity.size);
            //TODO fix position
            Tilec in = Edges.getFacingEdge(entity.tile(), other.tile()).entity;

            if(todump == null){

                for(int ii = 0; ii < content.items().size; ii++){
                    Item item = content.item(ii);

                    if(other.team() == entity.team() && entity.items().has(item) && other.block().acceptItem(other, in, item) && canDump(entity, other, item)){
                        other.block().handleItem(other, in, item);
                        entity.items().remove(item, 1);
                        incrementDump(entity, proximity.size);
                        return true;
                    }
                }
            }else{
                if(other.team() == entity.team() && other.block().acceptItem(other, in, todump) && canDump(entity, other, todump)){
                    other.block().handleItem(other, in, todump);
                    entity.items().remove(todump, 1);
                    incrementDump(entity, proximity.size);
                    return true;
                }
            }

            incrementDump(entity, proximity.size);
        }

        return false;
    }

    protected void incrementDump(Tilec tile, int prox){
        tile.rotation((byte)((tile.rotation() + 1) % prox));
    }

    /** Used for dumping items. */
    public boolean canDump(Tilec tile, Tilec to, Item item){
        return true;
    }

    /** Try offloading an item to a nearby container in its facing direction. Returns true if success. */
    public boolean offloadDir(Tilec tile, Item item){
        Tilec other = tile.tile().front();
        if(other != null && other.team() == tile.team() && other.block().acceptItem(other, tile, item)){
            other.block().handleItem(other, tile, item);
            return true;
        }
        return false;
    }

    public boolean canBreak(Tile tile){
        return true;
    }

    public void onProximityRemoved(Tilec tile){
        if(tile.power() != null){
            tile.block().powerGraphRemoved(tile);
        }
    }

    public void onProximityAdded(Tilec tile){
        if(tile.block().hasPower) tile.block().updatePowerGraph(tile);
    }

    protected void updatePowerGraph(Tilec tile){

        for(Tilec other : getPowerConnections(tile, tempTileEnts)){
            if(other.power() != null){
                other.power().graph.add(tile.power().graph);
            }
        }
    }

    protected void powerGraphRemoved(Tilec tile){
        if(tile.entity == null || tile.power() == null){
            return;
        }

        tile.power().graph.remove(tile);
        for(int i = 0; i < tile.power().links.size; i++){
            Tile other = world.tile(tile.power().links.get(i));
            if(other != null && other.entity != null && other.entity.power() != null){
                other.entity.power().links.removeValue(tile.pos());
            }
        }
    }

    public Array<Tilec> getPowerConnections(Tilec tile, Array<Tilec> out){
        out.clear();
        if(tile == null || tile.entity == null || tile.power() == null) return out;

        for(Tilec other : tile.proximity()){
            if(other != null && other.entity != null && other.power() != null
            && !(consumesPower && other.block().consumesPower && !outputsPower && !other.block().outputsPower)
            && !tile.power().links.contains(other.pos())){
                out.add(other);
            }
        }

        for(int i = 0; i < tile.power().links.size; i++){
            Tile link = world.tile(tile.power().links.get(i));
            if(link != null && link.entity != null && link.entity.power() != null) out.add(link.entity);
        }
        return out;
    }

    protected float getProgressIncrease(Tilec entity, float baseTime){
        return 1f / baseTime * entity.delta() * entity.efficiency();
    }

    /** @return whether this block should play its active sound.*/
    public boolean shouldActiveSound(Tilec tile){
        return false;
    }

    /** @return whether this block should play its idle sound.*/
    public boolean shouldIdleSound(Tilec tile){
        return shouldConsume(tile);
    }

    public void drawLayer(Tilec tile){
    }

    public void drawLayer2(Tilec tile){
    }

    public void drawCracks(Tilec tile){
        if(!tile.damaged() || size > maxCrackSize) return;
        int id = tile.pos();
        TextureRegion region = cracks[size - 1][Mathf.clamp((int)((1f - tile.healthf()) * crackRegions), 0, crackRegions-1)];
        Draw.colorl(0.2f, 0.1f + (1f - tile.healthf())* 0.6f);
        Draw.rect(region, tile.x(), tile.y(), (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(Tilec tile){
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

    public void draw(Tilec tile){
        Draw.rect(region, tile.x(), tile.y(), rotate ? tile.rotation() * 90 : 0);
    }

    public void drawLight(Tilec tile){
        if(tile.entity != null && hasLiquids && drawLiquidLight && tile.liquids().current().lightColor.a > 0.001f){
            drawLiquidLight(tile, tile.liquids().current(), tile.liquids().smoothAmount());
        }
    }

    public void drawLiquidLight(Tilec tile, Liquid liquid, float amount){
        if(amount > 0.01f){
            Color color = liquid.lightColor;
            float fract = 1f;
            float opacity = color.a * fract;
            if(opacity > 0.001f){
                renderer.lights.add(tile.x(), tile.y(), size * 30f * fract, color, opacity);
            }
        }
    }

    public void drawTeam(Tilec tile){
        Draw.color(tile.team().color);
        Draw.rect("block-border", tile.x() - size * tilesize / 2f + 4, tile.y() - size * tilesize / 2f + 4);
        Draw.color();
    }

    /** Called after the block is placed by this client. */
    @CallSuper
    public void playerPlaced(Tilec tile){

    }

    /** Called after the block is placed by anyone. */
    @CallSuper
    public void placed(Tilec tile){
        if(net.client()) return;

        if((consumesPower && !outputsPower) || (!consumesPower && outputsPower)){
            int range = 10;
            tempTiles.clear();
            Geometry.circle(tile.tileX(), tile.tileY(), range, (x, y) -> {
                Tilec other = world.ent(x, y);
                if(other != null && other.block() instanceof PowerNode && ((PowerNode)other.block()).linkValid(other, tile) && !PowerNode.insulated(other, tile) && !other.proximity().contains(tile) &&
                !(outputsPower && tile.proximity().contains(p -> p.entity != null && p.power() != null && p.power().graph == other.power().graph))){
                    tempTiles.add(other.tile());
                }
            });
            tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
            if(!tempTiles.isEmpty()){
                Tile toLink = tempTiles.first();
                if(!toLink.entity.power().links.contains(tile.pos())){
                    toLink.configureAny(tile.pos());
                }
            }
        }
    }

    public void removed(Tilec tile){
    }

    /** Called every frame a unit is on this tile. */
    public void unitOn(Tilec tile, Unitc unit){
    }

    /** Called when a unit that spawned at this tile is removed. */
    public void unitRemoved(Tilec tile, Unitc unit){
    }

    /** Returns whether ot not this block can be place on the specified tile. */
    public boolean canPlaceOn(Tile tile){
        return true;
    }

    /** Call when some content is produced. This unlocks the content if it is applicable. */
    public void useContent(Tilec tile, UnlockableContent content){
        //only unlocks content in zones
        if(!headless && tile.team() == player.team() && state.isCampaign()){
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

    public float percentSolid(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return 0;
        float sum = 0;
        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            sum += !other.floor.isLiquid ? 1f : 0f;
        }
        return sum / size / size;
    }

    /** Called when the block is tapped. This is equivalent to being configured with null. */
    public void tapped(Tilec tile, Playerc player){

    }

    /** Called when the block is destroyed. */
    public void onDestroyed(Tilec tile){
        float x = tile.x(), y = tile.y();
        float explosiveness = baseExplosiveness;
        float flammability = 0f;
        float power = 0f;

        if(hasItems){
            for(Item item : content.items()){
                int amount = tile.items().get(item);
                explosiveness += item.explosiveness * amount;
                flammability += item.flammability * amount;
            }
        }

        if(hasLiquids){
            flammability += tile.liquids().sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += tile.liquids().sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(consumes.hasPower() && consumes.getPower().buffered){
            power += tile.power().status * consumes.getPower().capacity;
        }

        if(hasLiquids){

            tile.liquids().each((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Time.run(i / 2f, () -> {
                        Tile other = world.tile(tile.tileX() + Mathf.range(size / 2), tile.tileY() + Mathf.range(size / 2));
                        if(other != null){
                            Puddles.deposit(other, liquid, splash);
                        }
                    });
                }
            });
        }

        Damage.dynamicExplosion(x, y, flammability, explosiveness * 3.5f, power, tilesize * size / 2f, Pal.darkFlame);
        if(!tile.floor().solid && !tile.floor().isLiquid){
            Effects.rubble(tile.x(), tile.y(), size);
        }
    }

    /**
     * Returns the flammability of the tile. Used for fire calculations.
     * Takes flammability of floor liquid into account.
     */
    public float getFlammability(Tilec tile){
        if(!hasItems || tile.entity == null){
            if(tile.floor().isLiquid && !solid){
                return tile.floor().liquidDrop.flammability;
            }
            return 0;
        }else{
            float result = tile.items().sum((item, amount) -> item.flammability * amount);

            if(hasLiquids){
                result += tile.liquids().sum((liquid, amount) -> liquid.flammability * amount / 3f);
            }

            return result;
        }
    }

    public String getDisplayName(Tilec tile){
        return block.localizedName;
    }

    public TextureRegion getDisplayIcon(Tilec tile){
        return block.icon(Cicon.medium);
    }

    public void display(Tilec entity, Table table){

        if(entity != null){
            table.table(bars -> {
                bars.defaults().growX().height(18f).pad(4);

                displayBars(entity, bars);
            }).growX();
            table.row();
            table.table(ctable -> {
                displayConsumption(entity, ctable);
            }).growX();

            table.marginBottom(-5);
        }
    }

    public void displayConsumption(Tilec tile, Table table){
        table.left();
        for(Consume cons : consumes.all()){
            if(cons.isOptional() && cons.isBoost()) continue;
            cons.build(tile, table);
        }
    }

    public void displayBars(Tilec tile, Table table){
        for(Func<Tilec, Bar> bar : bars.list()){
            table.add(bar.get(tile)).growX();
            table.row();
        }
    }

     /** Called when this block is tapped to build a UI on the table.
      * configurable must be true for this to be called.*/
    public void buildConfiguration(Tilec tile, Table table){
    }

    /** Update table alignment after configuring.*/
    public void updateTableAlign(Tilec tile, Table table){
        Vec2 pos = Core.input.mouseScreen(tile.x(), tile.y() - tile.block().size * tilesize / 2f - 1);
        table.setPosition(pos.x, pos.y, Align.top);
    }

    /** Returns whether or not a hand cursor should be shown over this block. */
    public Cursor getCursor(Tilec tile){
        return configurable ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when another tile is tapped while this block is selected.
     * Returns whether or not this block should be deselected.
     */
    public boolean onConfigureTileTapped(Tilec tile, Tilec other){
        return tile != other;
    }

    /** Returns whether this config menu should show when the specified player taps it. */
    public boolean shouldShowConfigure(Tilec tile, Playerc player){
        return true;
    }

    /** Whether this configuration should be hidden now. Called every frame the config is open. */
    public boolean shouldHideConfigure(Tilec tile, Playerc player){
        return false;
    }

    public void drawConfigure(Tilec tile){
        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.square(tile.x(), tile.y(), tile.block().size * tilesize / 2f + 1f);
        Draw.reset();
    }

    public boolean isSolidFor(Tilec tile){
        return false;
    }


    public float handleDamage(Tilec tile, float amount){
        return amount;
    }

    public void handleBulletHit(Tilec entity, Bulletc bullet){
        entity.damage(bullet.damage());
    }

    /** @return a custom minimap color for this tile, or 0 to use default colors. */
    public int minimapColor(Tile tile){
        return 0;
    }

    public void update(Tilec tile){
    }

    public boolean collide(Bulletc other){
        return true;
    }

    public void collision(Bulletc other){
        block.handleBulletHit(this, other);
    }

    public void removeFromProximity(){
        block.onProximityRemoved(this);

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tilec other = world.ent(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                other.block().onProximityUpdate(other);
                other.proximity().remove(this, true);
            }
        }
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tilec other = world.ent(tile.x + point.x, tile.y + point.y);

            if(other == null || !(other.tile().interactable(tile.team()))) continue;

            //add this tile to proximity of nearby tiles
            if(!other.proximity().contains(this, true)){
                other.proximity().add(this);
            }

            tmpTiles.add(other);
        }

        //using a set to prevent duplicates
        for(Tilec tile : tmpTiles){
            proximity.add(tile);
        }

        block.onProximityAdded(this);
        block.onProximityUpdate(this);

        for(Tilec other : tmpTiles){
            other.block().onProximityUpdate(other);
        }
    }

    //endregion
    //region overrides

    /** Tile configuration. Defaults to null. Used for block rebuilding. */
    @Nullable
    public Object config(){
        return null;
    }

    @Override
    public void remove(){
        if(sound != null){
            sound.stop();
        }
    }

    @Override
    public void killed(){
        Events.fire(new BlockDestroyEvent(tile));
        block.breakSound.at(tile);
        block.onDestroyed(this);
        tile.remove();
    }

    @Override
    public void update(){
        timeScaleDuration -= Time.delta();
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(sound != null){
            sound.update(x(), y(), block.shouldActiveSound(this));
        }

        if(block.idleSound != Sounds.none && block.shouldIdleSound(this)){
            loops.play(block.idleSound, this, block.idleSoundVolume);
        }

        block.update(this);

        if(liquids != null){
            liquids.update();
        }

        if(cons != null){
            cons.update();
        }

        if(power != null){
            power.graph.update();
        }
    }

    //endregion
}
