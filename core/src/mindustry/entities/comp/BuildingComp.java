package mindustry.entities.comp;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.audio.*;
import mindustry.content.*;
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

@EntityDef(value = {Buildingc.class}, isFinal = false, genio = false, serialize = false)
@Component(base = true)
abstract class BuildingComp implements Posc, Teamc, Healthc, Buildingc, Timerc, QuadTreeObject, Displayable{
    //region vars and initialization
    static final float timeToSleep = 60f * 1;
    static final ObjectSet<Building> tmpTiles = new ObjectSet<>();
    static final Seq<Building> tempTileEnts = new Seq<>();
    static final Seq<Tile> tempTiles = new Seq<>();
    static int sleepingEntities = 0;
    
    @Import float x, y, health;
    @Import Team team;

    transient Tile tile;
    transient Block block;
    transient Seq<Building> proximity = new Seq<>(8);
    transient boolean updateFlow;
    transient byte dump;
    transient int rotation;

    PowerModule power;
    ItemModule items;
    LiquidModule liquids;
    ConsumeModule cons;

    private transient float timeScale = 1f, timeScaleDuration;

    private transient @Nullable SoundLoop sound;

    private transient boolean sleeping;
    private transient float sleepTime;
    private transient boolean initialized;

    /** Sets this tile entity data to this and adds it if necessary. */
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
        if(!initialized){
            create(tile.block(), team);
        }
        this.rotation = rotation;
        this.tile = tile;

        set(tile.drawx(), tile.drawy());

        if(shouldAdd){
            add();
        }

        created();

        return base();
    }

    /** Sets up all the necessary variables, but does not add this entity anywhere. */
    public Building create(Block block, Team team){
        this.tile = emptyTile;
        this.block = block;
        this.team = team;

        if(block.activeSound != Sounds.none){
            sound = new SoundLoop(block.activeSound, block.activeSoundVolume);
        }

        health(block.health);
        maxHealth(block.health);
        timer(new Interval(block.timers));

        cons = new ConsumeModule(base());
        if(block.hasItems) items = new ItemModule();
        if(block.hasLiquids) liquids = new LiquidModule();
        if(block.hasPower){
            power = new PowerModule();
            power.graph.add(base());
        }

        initialized = true;

        return base();
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
        write.b(rotation);
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

    /** Configure with the current, local player. */
    public void configure(Object value){
        //save last used config
        block.lastConfig = value;
        Call.tileConfig(player, base(), value);
    }

    /** Configure from a server. */
    public void configureAny(Object value){
        Call.tileConfig(null, base(), value);
    }

    /** Deselect this tile from configuration. */
    public void deselect(){
        if(!headless && control.input.frag.config.getSelectedTile() == base()){
            control.input.frag.config.hideConfig();
        }
    }

    /** Called clientside when the client taps a block to config.
     * @return whether the configuration UI should be shown. */
    public boolean configTapped(){
        return true;
    }

    public void applyBoost(float intensity, float  duration){
        timeScale = Math.max(timeScale, intensity);
        timeScaleDuration = Math.max(timeScaleDuration, duration);
    }

    public Building nearby(int dx, int dy){
        return world.build(tile.x + dx, tile.y + dy);
    }

    public Building nearby(int rotation){
        if(rotation == 0) return world.build(tile.x + 1, tile.y);
        if(rotation == 1) return world.build(tile.x, tile.y + 1);
        if(rotation == 2) return world.build(tile.x - 1, tile.y);
        if(rotation == 3) return world.build(tile.x, tile.y - 1);
        return null;
    }

    public byte relativeTo(Tile tile){
        return relativeTo(tile.x, tile.y);
    }

    public byte relativeTo(Building tile){
        return relativeTo(tile.tile());
    }

    public byte relativeToEdge(Tile other){
        return relativeTo(Edges.getFacingEdge(other, tile));
    }

    public byte relativeTo(int cx, int cy){
        return tile.absoluteRelativeTo(cx, cy);
    }

    /** Multiblock front. */
    public @Nullable Building front(){
        int trns = block.size/2 + 1;
        return nearby(Geometry.d4(rotation).x * trns, Geometry.d4(rotation).y * trns);
    }

    /** Multiblock back. */
    public @Nullable Building back(){
        int trns = block.size/2 + 1;
        return nearby(Geometry.d4(rotation + 2).x * trns, Geometry.d4(rotation + 2).y * trns);
    }

    /** Multiblock left. */
    public @Nullable Building left(){
        int trns = block.size/2 + 1;
        return nearby(Geometry.d4(rotation + 1).x * trns, Geometry.d4(rotation + 1).y * trns);
    }

    /** Multiblock right. */
    public @Nullable Building right(){
        int trns = block.size/2 + 1;
        return nearby(Geometry.d4(rotation + 3).x * trns, Geometry.d4(rotation + 3).y * trns);
    }

    public int pos(){
        return tile.pos();
    }

    public float rotdeg(){
        return rotation * 90;
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
        return Time.delta * timeScale;
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
        sleepTime += Time.delta;
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

    /** Returns the version of this Building IO code.*/
    public byte version(){
        return 0;
    }

    //endregion
    //region handler methods

    public void created(){}
    
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
        if(acceptItem(base(), item) && block.hasItems && (source == null || source.team() == team)){
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

    public boolean acceptPayload(Building source, Payload payload){
        return false;
    }

    public void handlePayload(Building source, Payload payload){

    }


    /**
     * Tries moving a payload forwards.
     * @param todump payload to dump.
     * @return whether the payload was moved successfully
     */
    public boolean movePayload(@NonNull Payload todump){
        int trns = block.size/2 + 1;
        Tile next = tile.getNearby(Geometry.d4(rotation).x * trns, Geometry.d4(rotation).y * trns);

        if(next != null && next.build != null && next.build.team() == team && next.build.acceptPayload(base(), todump)){
            next.build.handlePayload(base(), todump);
            return true;
        }

        return false;
    }

    /**
     * Tries dumping a payload to any adjacent block.
     * @param todump payload to dump.
     * @return whether the payload was moved successfully
     */
    public boolean dumpPayload(@NonNull Payload todump){
        if(proximity.size == 0) return false;

        int dump = this.dump;

        for(int i = 0; i < proximity.size; i++){
            Building other = proximity.get((i + dump) % proximity.size);

            if(other.team() == team && other.acceptPayload(base(), todump)){
                other.handlePayload(base(), todump);
                incrementDump(proximity.size);
                return true;
            }

            incrementDump(proximity.size);
        }

        return false;
    }

    public void handleItem(Building source, Item item){
        items.add(item, 1);
    }

    public boolean acceptItem(Building source, Item item){
        return block.consumes.itemFilters.get(item.id) && items.get(item) < getMaximumAccepted(item);
    }

    public boolean acceptLiquid(Building source, Liquid liquid, float amount){
        return block.hasLiquids && liquids.get(liquid) + amount < block.liquidCapacity && block.consumes.liquidfilters.get(liquid.id);
    }

    public void handleLiquid(Building source, Liquid liquid, float amount){
        liquids.add(liquid, amount);
    }

    public void dumpLiquid(Liquid liquid){
        int dump = this.dump;

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            other = other.getLiquidDestination(base(), liquid);

            if(other != null && other.team == team && other.block.hasLiquids && canDumpLiquid(other, liquid) && other.liquids != null){
                float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
                float fract = liquids.get(liquid) / block.liquidCapacity;

                if(ofract < fract) transferLiquid(other, (fract - ofract) * block.liquidCapacity / 2f, liquid);
            }
        }

    }

    public boolean canDumpLiquid(Building to, Liquid liquid){
        return true;
    }

    public void transferLiquid(Building next, float amount, Liquid liquid){
        float flow = Math.min(next.block.liquidCapacity - next.liquids.get(liquid) - 0.001f, amount);

        if(next.acceptLiquid(base(), liquid, flow)){
            next.handleLiquid(base(), liquid, flow);
            liquids.remove(liquid, flow);
        }
    }

    public float moveLiquidForward(float leakResistance, Liquid liquid){
        Tile next = tile.getNearby(rotation);

        if(next == null) return 0;

        if(next.build != null){
            return moveLiquid(next.build, liquid);
        }else if(leakResistance != 100f && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = liquids.get(liquid) / leakResistance;
            Puddles.deposit(next, tile, liquid, leakAmount);
            liquids.remove(liquid, leakAmount);
        }
        return 0;
    }

    public float moveLiquid(Building next, Liquid liquid){
        if(next == null) return 0;

        next = next.getLiquidDestination(base(), liquid);

        if(next.team() == team && next.block.hasLiquids && liquids.get(liquid) > 0f){

            if(next.acceptLiquid(base(), liquid, 0f)){
                float ofract = next.liquids().get(liquid) / next.block.liquidCapacity;
                float fract = liquids.get(liquid) / block.liquidCapacity * block.liquidPressure;
                float flow = Math.min(Mathf.clamp((fract - ofract) * (1f)) * (block.liquidCapacity), liquids.get(liquid));
                flow = Math.min(flow, next.block.liquidCapacity - next.liquids().get(liquid) - 0.001f);

                if(flow > 0f && ofract <= fract && next.acceptLiquid(base(), liquid, flow)){
                    next.handleLiquid(base(), liquid, flow);
                    liquids.remove(liquid, flow);
                    return flow;
                }else if(ofract > 0.1f && fract > 0.1f){
                    //TODO these are incorrect effect positions
                    float fx = (x + next.x) / 2f, fy = (y + next.y) / 2f;

                    Liquid other = next.liquids().current();
                    if((other.flammability > 0.3f && liquid.temperature > 0.7f) || (liquid.flammability > 0.3f && other.temperature > 0.7f)){
                        damage(1 * Time.delta);
                        next.damage(1 * Time.delta);
                        if(Mathf.chance(0.1 * Time.delta)){
                            Fx.fire.at(fx, fy);
                        }
                    }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) || (other.temperature > 0.7f && liquid.temperature < 0.55f)){
                        liquids.remove(liquid, Math.min(liquids.get(liquid), 0.7f * Time.delta));
                        if(Mathf.chance(0.2f * Time.delta)){
                            Fx.steam.at(fx, fy);
                        }
                    }
                }
            }
        }
        return 0;
    }

    public Building getLiquidDestination(Building from, Liquid liquid){
        return base();
    }

    public @Nullable Payload getPayload(){
        return null;
    }

    /** Tries to take the payload. Returns null if no payload is present. */
    public @Nullable Payload takePayload(){
        return null;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.
     */
    public void offload(Item item){
        int dump = this.dump;

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            if(other.team() == team && other.acceptItem(base(), item) && canDump(other, item)){
                other.handleItem(base(), item);
                return;
            }
        }

        handleItem(base(), item);
    }

    /**
     * Tries to put this item into a nearby container. Returns success. Unlike #offload(), this method does not change the block inventory.
     */
    public boolean put(Item item){
        int dump = this.dump;

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            if(other.team() == team && other.acceptItem(base(), item) && canDump(other, item)){
                other.handleItem(base(), item);
                return true;
            }
        }

        return false;
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

        int dump = this.dump;

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Building other = proximity.get((i + dump) % proximity.size);

            if(todump == null){

                for(int ii = 0; ii < content.items().size; ii++){
                    Item item = content.item(ii);

                    if(other.team() == team && items.has(item) && other.acceptItem(base(), item) && canDump(other, item)){
                        other.handleItem(base(), item);
                        items.remove(item, 1);
                        incrementDump(proximity.size);
                        return true;
                    }
                }
            }else{
                if(other.team() == team && other.acceptItem(base(), todump) && canDump(other, todump)){
                    other.handleItem(base(), todump);
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
        dump = (byte)((dump + 1) % prox);
    }

    /** Used for dumping items. */
    public boolean canDump(Building to, Item item){
        return true;
    }

    /** Try offloading an item to a nearby container in its facing direction. Returns true if success. */
    public boolean moveForward(Item item){
        Building other = front();
        if(other != null && other.team() == team && other.acceptItem(base(), item)){
            other.handleItem(base(), item);
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

        for(Building other : getPowerConnections(tempTileEnts)){
            if(other.power != null){
                other.power.graph.addGraph(power.graph);
            }
        }
    }

    public void powerGraphRemoved(){
        if(power == null){
            return;
        }

        power.graph.remove(base());
        for(int i = 0; i < power.links.size; i++){
            Tile other = world.tile(power.links.get(i));
            if(other != null && other.build != null && other.build.power != null){
                other.build.power.links.removeValue(pos());
            }
        }
    }

    public Seq<Building> getPowerConnections(Seq<Building> out){
        out.clear();
        if(power == null) return out;

        for(Building other : proximity){
            if(other != null && other.power != null
            && !(block.consumesPower && other.block.consumesPower && !block.outputsPower && !other.block.outputsPower)
            && !power.links.contains(other.pos())){
                out.add(other);
            }
        }

        for(int i = 0; i < power.links.size; i++){
            Tile link = world.tile(power.links.get(i));
            if(link != null && link.build != null && link.build.power != null) out.add(link.build);
        }
        return out;
    }

    public float getProgressIncrease(float baseTime){
        return 1f / baseTime * edelta();
    }

    public float getDisplayEfficiency(){
        return getProgressIncrease(1f) / edelta();
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

            Draw.z(Layer.blockOver);
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
        Draw.rect(region, x, y, (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(){
    }

    public void draw(){
        Draw.rect(block.region, x, y, block.rotate ? rotdeg() : 0);

        drawTeamTop();
    }

    public void drawTeamTop(){
        if(block.teamRegion.found()){
            if(block.teamRegions[team.id] == block.teamRegion) Draw.color(team.color);
            Draw.rect(block.teamRegions[team.id], x, y);
            Draw.color();
        }
    }

    public void drawLight(){
        if(block.hasLiquids && block.drawLiquidLight && liquids.current().lightColor.a > 0.001f){
            drawLiquidLight(liquids.current(), liquids.smoothAmount());
        }
    }

    public void drawLiquidLight(Liquid liquid, float amount){
        if(amount > 0.01f){
            Color color = liquid.lightColor;
            float fract = 1f;
            float opacity = color.a * fract;
            if(opacity > 0.001f){
                Drawf.light(team, x, y, block.size * 30f * fract, color, opacity);
            }
        }
    }

    public void drawTeam(){
        Draw.color(team.color);
        Draw.rect("block-border", x - block.size * tilesize / 2f + 4, y - block.size * tilesize / 2f + 4);
        Draw.color();
    }

    /** Called after the block is placed by this client. */
    @CallSuper
    public void playerPlaced(){
        if(block.saveConfig && block.lastConfig != null){
            configure(block.lastConfig);
        }
    }

    /** Called after the block is placed by anyone. */
    @CallSuper
    public void placed(){
        if(net.client()) return;

        if((block.consumesPower && !block.outputsPower) || (!block.consumesPower && block.outputsPower)){
            int range = 10;
            tempTiles.clear();
            Geometry.circle(tileX(), tileY(), range, (x, y) -> {
                Building other = world.build(x, y);
                if(other != null && other.block instanceof PowerNode && ((PowerNode)other.block).linkValid(other, base()) && !PowerNode.insulated(other, base())
                    && !other.proximity().contains(this.<Building>base()) &&
                !(block.outputsPower && proximity.contains(p -> p.power != null && p.power.graph == other.power.graph))){
                    tempTiles.add(other.tile());
                }
            });
            tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
            if(!tempTiles.isEmpty()){
                Tile toLink = tempTiles.first();
                if(!toLink.build.power.links.contains(pos())){
                    toLink.build.configureAny(pos());
                }
            }
        }
    }

    public void onRemoved(){
    }

    /** Called every frame a unit is on this  */
    public void unitOn(Unit unit){
    }

    /** Called when a unit that spawned at this tile is removed. */
    public void unitRemoved(Unit unit){
    }

    /** Called when arbitrary configuration is applied to a tile. */
    public void configured(@Nullable Player player, @Nullable Object value){
        //null is of type void.class; anonymous classes use their superclass.
        Class<?> type = value == null ? void.class : value.getClass().isAnonymousClass() ? value.getClass().getSuperclass() : value.getClass();

        if(block.configurations.containsKey(type)){
            block.configurations.get(type).get(this, value);
        }
    }

    /** Called when the block is tapped.*/
    public void tapped(Player player){

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
            flammability += liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
            explosiveness += liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
        }

        if(block.consumes.hasPower() && block.consumes.getPower().buffered){
            power += this.power.status * block.consumes.getPower().capacity;
        }

        if(block.hasLiquids){

            liquids.each((liquid, amount) -> {
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
                result += liquids.sum((liquid, amount) -> liquid.flammability * amount / 3f);
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

    @Override
    public void display(Table table){
        //display the block stuff
        //TODO duplicated code?
        table.table(t -> {
            t.left();
            t.add(new Image(block.getDisplayIcon(tile))).size(8 * 4);
            t.labelWrap(block.getDisplayName(tile)).left().width(190f).padLeft(5);
        }).growX().left();

        table.row();

        //only display everything else if the team is the same
        if(team == player.team()){
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
                    table.left();
                    table.table(l -> {
                        Bits current = new Bits();

                        Runnable rebuild = () -> {
                            l.clearChildren();
                            l.left();
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
                    }).left();
                }

                if(liquids != null){
                    table.row();
                    table.table(l -> {
                        l.left();
                        l.image(() -> liquids.current().icon(Cicon.small)).padRight(3f);
                        l.label(() -> liquids.getFlowRate() < 0 ? "..." : Strings.fixed(liquids.getFlowRate(), 2) + ps).color(Color.lightGray);
                    }).left();
                }
            }

            table.marginBottom(-5);
        }
    }

    public void displayConsumption(Table table){
        table.left();
        for(Consume cons : block.consumes.all()){
            if(cons.isOptional() && cons.isBoost()) continue;
            cons.build(base(), table);
        }
    }

    public void displayBars(Table table){
        for(Func<Building, Bar> bar : block.bars.list()){
            table.add(bar.get(base())).growX();
            table.row();
        }
    }

     /** Called when this block is tapped to build a UI on the table.
      * configurable must be true for this to be called.*/
    public void buildConfiguration(Table table){
    }

    /** Update table alignment after configuring.*/
    public void updateTableAlign(Table table){
        Vec2 pos = Core.input.mouseScreen(x, y - block.size * tilesize / 2f - 1);
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
    public boolean onConfigureTileTapped(Building other){
        return base() != other;
    }

    /** Returns whether this config menu should show when the specified player taps it. */
    public boolean shouldShowConfigure(Player player){
        return true;
    }

    /** Whether this configuration should be hidden now. Called every frame the config is open. */
    public boolean shouldHideConfigure(Player player){
        return false;
    }

    public void drawConfigure(){
        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.square(x, y, block.size * tilesize / 2f + 1f);
        Draw.reset();
    }

    public boolean checkSolid(){
        return false;
    }


    public float handleDamage(float amount){
        return amount;
    }

    public boolean collide(Bullet other){
        return true;
    }

    /** Handle a bullet collision.
     * @return whether the bullet should be removed. */
    public boolean collision(Bullet other){
        damage(other.damage() * other.type().tileDamageMultiplier);

        return true;
    }

    public void removeFromProximity(){
        onProximityRemoved();
        tmpTiles.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Building other = world.build(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                tmpTiles.add(other);
            }
        }

        for(Building other : tmpTiles){
            other.proximity.remove(base(), true);
            other.onProximityUpdate();
        }
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();
        
        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Building other = world.build(tile.x + point.x, tile.y + point.y);

            if(other == null || !(other.tile.interactable(team))) continue;

            //add this tile to proximity of nearby tiles
            if(!other.proximity.contains(base(), true)){
                other.proximity.add(base());
            }

            tmpTiles.add(other);
        }

        //using a set to prevent duplicates
        for(Building tile : tmpTiles){
            proximity.add(tile);
        }

        for(Building other : tmpTiles){
            other.onProximityUpdate();
        }

        onProximityAdded();
        onProximityUpdate();

        for(Building other : tmpTiles){
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
        return tile.build == base() && !dead();
    }

    @Replace
    @Override
    public void kill(){
        Call.tileDestroyed(base());
    }

    @Replace
    @Override
    public void damage(float damage){
        if(dead()) return;

        if(Mathf.zero(state.rules.blockHealthMultiplier)){
            damage = health + 1;
        }else{
            damage /= state.rules.blockHealthMultiplier;
        }

        Call.tileDamage(base(), health - handleDamage(damage));

        if(health <= 0){
            Call.tileDestroyed(base());
        }
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
        timeScaleDuration -= Time.delta;
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(sound != null){
            sound.update(x, y, shouldActiveSound());
        }

        if(block.idleSound != Sounds.none && shouldIdleSound()){
            loops.play(block.idleSound, base(), block.idleSoundVolume);
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
