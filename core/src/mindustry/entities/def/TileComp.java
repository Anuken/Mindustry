package mindustry.entities.def;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

@EntityDef(value = {Tilec.class}, isFinal = false, genio = false, serialize = false)
@Component
abstract class TileComp implements Posc, Teamc, Healthc, Tilec, Timerc, QuadTreeObject{
    //region vars and initialization
    static final float timeToSleep = 60f * 1;
    static final ObjectSet<Tilec> tmpTiles = new ObjectSet<>();
    static final Array<Tilec> tempTileEnts = new Array<>();
    static final Array<Tile> tempTiles = new Array<>();
    static int sleepingEntities = 0;
    
    @Import float x, y, health;
    @Import Team team;

    transient Tile tile;
    transient Block block;
    transient Array<Tilec> proximity = new Array<>(8);
    transient boolean updateFlow;

    PowerModule power;
    ItemModule items;
    LiquidModule liquids;
    ConsumeModule cons;

    private transient float timeScale = 1f, timeScaleDuration;

    private transient @Nullable SoundLoop sound;

    private transient boolean sleeping;
    private transient float sleepTime;

    /** Sets this tile entity data to this and adds it if necessary. */
    public Tilec init(Tile tile, Team team, boolean shouldAdd){
        this.tile = tile;
        this.block = tile.block();
        this.team = team;

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

    @Override
    @Replace
    public int tileX(){
        return tile.x;
    }

    @Override
    @Replace
    public int tileY(){
        return tile.y;
    }

    //endregion
    //region io

    public final void writeBase(Writes write){
        write.f(health);
        write.b(rotation());
        write.b(team.id);
        if(items != null) items.write(write);
        if(power != null) power.write(write);
        if(liquids != null) liquids.write(write);
        if(cons != null) cons.write(write);
    }

    public final void readBase(Reads read){
        health = read.f();
        rotation(read.b());
        team = Team.get(read.b());
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

    public Tilec nearby(int dx, int dy){
        return world.ent(tile.x + dx, tile.y + dy);
    }

    public Tilec nearby(int rotation){
        if(rotation == 0) return world.ent(tile.x + 1, tile.y);
        if(rotation == 1) return world.ent(tile.x, tile.y + 1);
        if(rotation == 2) return world.ent(tile.x - 1, tile.y);
        if(rotation == 3) return world.ent(tile.x, tile.y - 1);
        return null;
    }

    public byte relativeTo(Tile tile){
        return relativeTo(tile.x, tile.y);
    }

    /** Return relative rotation to a coordinate. Returns -1 if the coordinate is not near this tile. */
    public byte relativeTo(int cx, int cy){
        int x = tile.x, y = tile.y;
        if(x == cx && y == cy - 1) return 1;
        if(x == cx && y == cy + 1) return 3;
        if(x == cx - 1 && y == cy) return 0;
        if(x == cx + 1 && y == cy) return 2;
        return -1;
    }

    public byte absoluteRelativeTo(int cx, int cy){
        int x = tile.x, y = tile.y;
        if(Math.abs(x - cx) > Math.abs(y - cy)){
            if(x <= cx - 1) return 0;
            if(x >= cx + 1) return 2;
        }else{
            if(y <= cy - 1) return 1;
            if(y >= cy + 1) return 3;
        }

        return -1;
    }

    public @Nullable Tilec front(){
        return nearby((rotation() + 4) % 4);
    }

    public @Nullable Tilec right(){
        return nearby((rotation() + 3) % 4);
    }

    public @Nullable Tilec back(){
        return nearby((rotation() + 2) % 4);
    }

    public @Nullable Tilec left(){
        return nearby((rotation() + 1) % 4);
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

    /** Efficiency * delta. */
    public float edelta(){
        return efficiency() * delta();
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
    
    public boolean shouldConsume(){
        return true;
    }

    public boolean productionValid(){
        return true;
    }

    public float getPowerProduction(){
        return 0f;
    }

    /** Returns the amount of items this block can accept. */
    public int acceptStack(Item item, int amount, Teamc source){
        if(acceptItem(this, item) && block.hasItems && (source == null || source.team() == team())){
            return Math.min(getMaximumAccepted(item) - items.get(item), amount);
        }else{
            return 0;
        }
    }

    public int getMaximumAccepted(Item item){
        return block.itemCapacity;
    }

    /** Remove a stack from this inventory, and return the amount removed. */
    public int removeStack(Item item, int amount){
        if(items == null) return 0;
        amount = Math.min(amount, items.get(item));
        noSleep();
        items.remove(item, amount);
        return amount;
    }

    /** Handle a stack input. */
    public void handleStack(Item item, int amount, Teamc source){
        noSleep();
        items.add(item, amount);
    }

    /** Returns offset for stack placement. */
    public void getStackOffset(Item item, Vec2 trns){

    }

    public void onProximityUpdate(){
        noSleep();
    }

    public boolean acceptPayload(Tilec source, Payload payload){
        return false;
    }

    public void handlePayload(Tilec source, Payload payload){

    }

    /**
     * Tries dumping a payload.
     * @param todump payload to dump.
     * @return whether the payload was moved successfully
     */
    public boolean dumpPayload(@NonNull Payload todump){
        Array<Tilec> proximity = proximity();
        int dump = rotation();

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Tilec other = proximity.get((i + dump) % proximity.size);

            if(other.team() == team() && other.acceptPayload(this, todump)){
                other.handlePayload(this, todump);
                incrementDump(proximity.size);
                return true;
            }

            incrementDump(proximity.size);
        }

        return false;
    }

    public void handleItem(Tilec source, Item item){
        items.add(item, 1);
    }

    public boolean acceptItem(Tilec source, Item item){
        return block.consumes.itemFilters.get(item.id) && items.get(item) < getMaximumAccepted(item);
    }

    public boolean acceptLiquid(Tilec source, Liquid liquid, float amount){
        return block.hasLiquids && liquids().get(liquid) + amount < block.liquidCapacity && block.consumes.liquidfilters.get(liquid.id);
    }

    public void handleLiquid(Tilec source, Liquid liquid, float amount){
        liquids().add(liquid, amount);
    }

    public void dumpLiquid(Liquid liquid){
        int dump = rotation() / block.dumpIncrement;

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Tilec other = proximity.get((i + dump) % proximity.size);
            other = other.getLiquidDestination(this, liquid);

            if(other != null && other.team() == team() && other.block().hasLiquids && canDumpLiquid(other, liquid) && other.liquids() != null){
                float ofract = other.liquids().get(liquid) / other.block().liquidCapacity;
                float fract = liquids().get(liquid) / block.liquidCapacity;

                if(ofract < fract) transferLiquid(other, (fract - ofract) * block.liquidCapacity / 2f, liquid);
            }
        }

    }

    public boolean canDumpLiquid(Tilec to, Liquid liquid){
        return true;
    }

    public void transferLiquid(Tilec next, float amount, Liquid liquid){
        float flow = Math.min(next.block().liquidCapacity - next.liquids().get(liquid) - 0.001f, amount);

        if(next.acceptLiquid(this, liquid, flow)){
            next.handleLiquid(this, liquid, flow);
            liquids().remove(liquid, flow);
        }
    }

    public float moveLiquid(Tilec next, boolean leak, Liquid liquid){
        return moveLiquid(next, leak ? 1.5f : 100, liquid);
    }

    public float moveLiquid(Tilec next, float leakResistance, Liquid liquid){
        if(next == null) return 0;

        next = next.getLiquidDestination(this, liquid);

        if(next.team() == team() && next.block().hasLiquids && liquids().get(liquid) > 0f){

            if(next.acceptLiquid(this, liquid, 0f)){
                float ofract = next.liquids().get(liquid) / next.block().liquidCapacity;
                float fract = liquids().get(liquid) / block.liquidCapacity * block.liquidPressure;
                float flow = Math.min(Mathf.clamp((fract - ofract) * (1f)) * (block.liquidCapacity), liquids().get(liquid));
                flow = Math.min(flow, next.block().liquidCapacity - next.liquids().get(liquid) - 0.001f);

                if(flow > 0f && ofract <= fract && next.acceptLiquid(this, liquid, flow)){
                    next.handleLiquid(this, liquid, flow);
                    liquids().remove(liquid, flow);
                    return flow;
                }else if(ofract > 0.1f && fract > 0.1f){
                    //TODO these are incorrect effect positions
                    float fx = (x() + next.x()) / 2f, fy = (y() + next.y()) / 2f;

                    Liquid other = next.liquids().current();
                    if((other.flammability > 0.3f && liquid.temperature > 0.7f) || (liquid.flammability > 0.3f && other.temperature > 0.7f)){
                        damage(1 * Time.delta());
                        next.damage(1 * Time.delta());
                        if(Mathf.chance(0.1 * Time.delta())){
                            Fx.fire.at(fx, fy);
                        }
                    }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) || (other.temperature > 0.7f && liquid.temperature < 0.55f)){
                        liquids().remove(liquid, Math.min(liquids().get(liquid), 0.7f * Time.delta()));
                        if(Mathf.chance(0.2f * Time.delta())){
                            Fx.steam.at(fx, fy);
                        }
                    }
                }
            }
        }else if(leakResistance != 100f && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = liquids().get(liquid) / leakResistance;
            Puddles.deposit(next.tile(), tile(), liquid, leakAmount);
            liquids().remove(liquid, leakAmount);
        }
        return 0;
    }

    public Tilec getLiquidDestination(Tilec from, Liquid liquid){
        return this;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.
     */
    public void offloadNear(Item item){
        Array<Tilec> proximity = proximity();
        int dump = rotation() / block.dumpIncrement;
        useContent(item);

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Tilec other = proximity.get((i + dump) % proximity.size);
            if(other.team() == team() && other.acceptItem(this, item) && canDump(other, item)){
                other.handleItem(this, item);
                return;
            }
        }

        handleItem(this, item);
    }

    /** Try dumping any item near the  */
    public boolean dump(){
        return dump(null);
    }

    /**
     * Try dumping a specific item near the 
     * @param todump Item to dump. Can be null to dump anything.
     */
    public boolean dump(Item todump){
        if(!block.hasItems || items.total() == 0 || (todump != null && !items.has(todump))) return false;

        Array<Tilec> proximity = proximity();
        int dump = rotation() / block.dumpIncrement;

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Tilec other = proximity.get((i + dump) % proximity.size);

            if(todump == null){

                for(int ii = 0; ii < content.items().size; ii++){
                    Item item = content.item(ii);

                    if(other.team() == team() && items.has(item) && other.acceptItem(this, item) && canDump(other, item)){
                        other.handleItem(this, item);
                        items.remove(item, 1);
                        incrementDump(proximity.size);
                        return true;
                    }
                }
            }else{
                if(other.team() == team() && other.acceptItem(this, todump) && canDump(other, todump)){
                    other.handleItem(this, todump);
                    items.remove(todump, 1);
                    incrementDump(proximity.size);
                    return true;
                }
            }

            incrementDump(proximity.size);
        }

        return false;
    }

    public void incrementDump(int prox){
        rotation((byte)((rotation() + block.dumpIncrement) % (prox * block.dumpIncrement)));
    }

    /** Used for dumping items. */
    public boolean canDump(Tilec to, Item item){
        return true;
    }

    /** Try offloading an item to a nearby container in its facing direction. Returns true if success. */
    public boolean moveForward(Item item){
        Tilec other = front();
        if(other != null && other.team() == team() && other.acceptItem(this, item)){
            other.handleItem(this, item);
            return true;
        }
        return false;
    }

    public void onProximityRemoved(){
        if(power != null){
            powerGraphRemoved();
        }
    }

    public void onProximityAdded(){
        if(block.hasPower) updatePowerGraph();
    }

    public void updatePowerGraph(){

        for(Tilec other : getPowerConnections(tempTileEnts)){
            if(other.power() != null){
                other.power().graph.add(power.graph);
            }
        }
    }

    public void powerGraphRemoved(){
        if(power == null){
            return;
        }

        power.graph.remove(this);
        for(int i = 0; i < power.links.size; i++){
            Tile other = world.tile(power.links.get(i));
            if(other != null && other.entity != null && other.entity.power() != null){
                other.entity.power().links.removeValue(pos());
            }
        }
    }

    public Array<Tilec> getPowerConnections(Array<Tilec> out){
        out.clear();
        if(power == null) return out;

        for(Tilec other : proximity()){
            if(other != null && other.power() != null
            && !(block.consumesPower && other.block().consumesPower && !block.outputsPower && !other.block().outputsPower)
            && !power.links.contains(other.pos())){
                out.add(other);
            }
        }

        for(int i = 0; i < power.links.size; i++){
            Tile link = world.tile(power.links.get(i));
            if(link != null && link.entity != null && link.entity.power() != null) out.add(link.entity);
        }
        return out;
    }

    public float getProgressIncrease(float baseTime){
        return 1f / baseTime * delta() * efficiency();
    }

    /** @return whether this block should play its active sound.*/
    public boolean shouldActiveSound(){
        return false;
    }

    /** @return whether this block should play its idle sound.*/
    public boolean shouldIdleSound(){
        return shouldConsume();
    }

    public void drawStatus(){
        if(block.consumes.any()){
            float brcx = tile.drawx() + (block.size * tilesize / 2f) - (tilesize / 2f);
            float brcy = tile.drawy() - (block.size * tilesize / 2f) + (tilesize / 2f);

            Draw.color(Pal.gray);
            Fill.square(brcx, brcy, 2.5f, 45);
            Draw.color(cons.status().color);
            Fill.square(brcx, brcy, 1.5f, 45);
            Draw.color();
        }
    }

    public void drawCracks(){
        if(!damaged() || block.size > Block.maxCrackSize) return;
        int id = pos();
        TextureRegion region = Block.cracks[block.size - 1][Mathf.clamp((int)((1f - healthf()) * Block.crackRegions), 0, Block.crackRegions-1)];
        Draw.colorl(0.2f, 0.1f + (1f - healthf())* 0.6f);
        Draw.rect(region, x(), y(), (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(){
    }

    public void draw(){
        Draw.rect(block.region, x, y, block.rotate ? rotation() * 90 : 0);
    }

    public void drawLight(){
        if(block.hasLiquids && block.drawLiquidLight && liquids().current().lightColor.a > 0.001f){
            drawLiquidLight(liquids().current(), liquids().smoothAmount());
        }
    }

    public void drawLiquidLight(Liquid liquid, float amount){
        if(amount > 0.01f){
            Color color = liquid.lightColor;
            float fract = 1f;
            float opacity = color.a * fract;
            if(opacity > 0.001f){
                Drawf.light(x, y, block.size * 30f * fract, color, opacity);
            }
        }
    }

    public void drawTeam(){
        Draw.color(team().color);
        Draw.rect("block-border", x - block.size * tilesize / 2f + 4, y - block.size * tilesize / 2f + 4);
        Draw.color();
    }

    /** Called after the block is placed by this client. */
    @CallSuper
    public void playerPlaced(){

    }

    /** Called after the block is placed by anyone. */
    @CallSuper
    public void placed(){
        if(net.client()) return;

        if((block.consumesPower && !block.outputsPower) || (!block.consumesPower && block.outputsPower)){
            int range = 10;
            tempTiles.clear();
            Geometry.circle(tileX(), tileY(), range, (x, y) -> {
                Tilec other = world.ent(x, y);
                if(other != null && other.block() instanceof PowerNode && ((PowerNode)other.block()).linkValid(other, this) && !PowerNode.insulated(other, this) && !other.proximity().contains(this) &&
                !(block.outputsPower && proximity().contains(p -> p.power() != null && p.power().graph == other.power().graph))){
                    tempTiles.add(other.tile());
                }
            });
            tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
            if(!tempTiles.isEmpty()){
                Tile toLink = tempTiles.first();
                if(!toLink.entity.power().links.contains(pos())){
                    toLink.configureAny(pos());
                }
            }
        }
    }

    public void onRemoved(){
    }

    /** Called every frame a unit is on this  */
    public void unitOn(Unitc unit){
    }

    /** Called when a unit that spawned at this tile is removed. */
    public void unitRemoved(Unitc unit){
    }

    /** Call when some content is produced. This unlocks the content if it is applicable. */
    public void useContent(UnlockableContent content){
        //only unlocks content in zones
        if(!headless && team() == player.team() && state.isCampaign()){
            logic.handleContent(content);
        }
    }

    /** Called when arbitrary configuration is applied to a tile. */
    public void configured(@Nullable Playerc player, @Nullable Object value){
        //null is of type void.class; anonymous classes use their superclass.
        Class<?> type = value == null ? void.class : value.getClass().isAnonymousClass() ? value.getClass().getSuperclass() : value.getClass();

        if(block.configurations.containsKey(type)){
            block.configurations.get(type).get(this, value);
        }
    }

    /** Called when the block is tapped.*/
    public void tapped(Playerc player){

    }

    /** Called when the block is destroyed. */
    public void onDestroyed(){
        float explosiveness = block.baseExplosiveness;
        float flammability = 0f;
        float power = 0f;

        if(block.hasItems){
            for(Item item : content.items()){
                int amount = items.get(item);
                explosiveness += item.explosiveness * amount;
                flammability += item.flammability * amount;
            }
        }

        if(block.hasLiquids){
            flammability += liquids().sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += liquids().sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(block.consumes.hasPower() && block.consumes.getPower().buffered){
            power += this.power.status * block.consumes.getPower().capacity;
        }

        if(block.hasLiquids){

            liquids().each((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Time.run(i / 2f, () -> {
                        Tile other = world.tile(tileX() + Mathf.range(block.size / 2), tileY() + Mathf.range(block.size / 2));
                        if(other != null){
                            Puddles.deposit(other, liquid, splash);
                        }
                    });
                }
            });
        }

        Damage.dynamicExplosion(x, y, flammability, explosiveness * 3.5f, power, tilesize * block.size / 2f, Pal.darkFlame);
        if(!floor().solid && !floor().isLiquid){
            Effects.rubble(x, y, block.size);
        }
    }

    /**
     * Returns the flammability of the  Used for fire calculations.
     * Takes flammability of floor liquid into account.
     */
    public float getFlammability(){
        if(!block.hasItems){
            if(floor().isLiquid && !block.solid){
                return floor().liquidDrop.flammability;
            }
            return 0;
        }else{
            float result = items.sum((item, amount) -> item.flammability * amount);

            if(block.hasLiquids){
                result += liquids().sum((liquid, amount) -> liquid.flammability * amount / 3f);
            }

            return result;
        }
    }

    public String getDisplayName(){
        return block.localizedName;
    }

    public TextureRegion getDisplayIcon(){
        return block.icon(Cicon.medium);
    }

    public void display(Table table){
        table.table(bars -> {
            bars.defaults().growX().height(18f).pad(4);

            displayBars(bars);
        }).growX();
        table.row();
        table.table(this::displayConsumption).growX();

        boolean displayFlow = (block.category == Category.distribution || block.category == Category.liquid) && Core.settings.getBool("flow");

        if(displayFlow){
            String ps = " " + StatUnit.perSecond.localized();

            if(items != null){
                table.row();
                table.table(l -> {
                    Bits current = new Bits();
                    l.left();

                    Runnable rebuild = () -> {
                        l.clearChildren();
                        for(Item item : content.items()){
                            if(items.hasFlowItem(item)){
                                l.image(item.icon(Cicon.small)).padRight(3f);
                                l.label(() -> items.getFlowRate(item) < 0 ? "..." : Strings.fixed(items.getFlowRate(item), 1) + ps).color(Color.lightGray);
                                l.row();
                            }
                        }
                    };

                    rebuild.run();
                    l.update(() -> {
                        for(Item item : content.items()){
                            if(items.hasFlowItem(item) && !current.get(item.id)){
                                current.set(item.id);
                                rebuild.run();
                            }
                        }
                    });
                });
            }

            if(liquids != null){
                table.row();
                table.table(l -> {
                    l.left();
                    l.image(() -> liquids.current().icon(Cicon.small)).padRight(3f);
                    l.label(() -> liquids.getFlowRate() < 0 ? "..." : Strings.fixed(liquids.getFlowRate(), 2) + ps).color(Color.lightGray);
                });
            }
        }

        table.marginBottom(-5);
    }

    public void displayConsumption(Table table){
        table.left();
        for(Consume cons : block.consumes.all()){
            if(cons.isOptional() && cons.isBoost()) continue;
            cons.build(this, table);
        }
    }

    public void displayBars(Table table){
        for(Func<Tilec, Bar> bar : block.bars.list()){
            table.add(bar.get(this)).growX();
            table.row();
        }
    }

     /** Called when this block is tapped to build a UI on the table.
      * configurable must be true for this to be called.*/
    public void buildConfiguration(Table table){
    }

    /** Update table alignment after configuring.*/
    public void updateTableAlign(Table table){
        Vec2 pos = Core.input.mouseScreen(x, y - block().size * tilesize / 2f - 1);
        table.setPosition(pos.x, pos.y, Align.top);
    }

    /** Returns whether or not a hand cursor should be shown over this block. */
    public Cursor getCursor(){
        return block.configurable ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when another tile is tapped while this block is selected.
     * @return whether or not this block should be deselected.
     */
    public boolean onConfigureTileTapped(Tilec other){
        return this != other;
    }

    /** Returns whether this config menu should show when the specified player taps it. */
    public boolean shouldShowConfigure(Playerc player){
        return true;
    }

    /** Whether this configuration should be hidden now. Called every frame the config is open. */
    public boolean shouldHideConfigure(Playerc player){
        return false;
    }

    public void drawConfigure(){
        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.square(x, y, block().size * tilesize / 2f + 1f);
        Draw.reset();
    }

    public boolean checkSolid(){
        return false;
    }


    public float handleDamage(float amount){
        return amount;
    }

    public boolean collide(Bulletc other){
        return true;
    }

    public void collision(Bulletc other){
        damage(other.damage());
    }

    public void removeFromProximity(){
        onProximityRemoved();
        tmpTiles.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tilec other = world.ent(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                tmpTiles.add(other);
            }
        }

        for(Tilec other : tmpTiles){
            other.proximity().remove(this, true);
            other.onProximityUpdate();
        }
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();
        
        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tilec other = world.ent(tile.x + point.x, tile.y + point.y);

            if(other == null || !(other.tile().interactable(team()))) continue;

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

        for(Tilec other : tmpTiles){
            other.onProximityUpdate();
        }

        onProximityAdded();
        onProximityUpdate();

        for(Tilec other : tmpTiles){
            other.onProximityUpdate();
        }
    }

    public void updateTile(){

    }

    //endregion
    //region overrides

    /** Tile configuration. Defaults to null. Used for block rebuilding. */
    @Nullable
    public Object config(){
        return null;
    }

    @Replace
    @Override
    public boolean isValid(){
        return tile.entity == this && !dead();
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
        onDestroyed();
        tile.remove();
        remove();
    }

    @Final
    @Override
    public void update(){
        timeScaleDuration -= Time.delta();
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(sound != null){
            sound.update(x, y, shouldActiveSound());
        }

        if(block.idleSound != Sounds.none && shouldIdleSound()){
            loops.play(block.idleSound, this, block.idleSoundVolume);
        }

        updateTile();

        if(items != null){
            items.update(updateFlow);
        }

        if(liquids != null){
            liquids.update(updateFlow);
        }

        if(cons != null){
            cons.update();
        }

        if(power != null){
            power.graph.update();
        }

        updateFlow = false;
    }

    @Override
    public void hitbox(Rect out){
        out.setCentered(x, y, block.size * tilesize, block.size * tilesize);
    }

    //endregion
}
