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
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

@EntityDef(value = {Buildingc.class}, isFinal = false, genio = false, serialize = false)
@Component(base = true)
abstract class BuildingComp implements Posc, Teamc, Healthc, Buildingc, Timerc, QuadTreeObject, Displayable, Senseable, Controllable, Sized{
    //region vars and initialization
    static final float timeToSleep = 60f * 1, timeToUncontrol = 60f * 6;
    static final ObjectSet<Building> tmpTiles = new ObjectSet<>();
    static final Seq<Building> tempBuilds = new Seq<>();
    static final BuildTeamChangeEvent teamChangeEvent = new BuildTeamChangeEvent();
    static int sleepingEntities = 0;
    
    @Import float x, y, health, maxHealth;
    @Import Team team;

    transient Tile tile;
    transient Block block;
    transient Seq<Building> proximity = new Seq<>(6);
    transient boolean updateFlow;
    transient byte cdump;
    transient int rotation;
    transient boolean enabled = true;
    transient float enabledControlTime;
    transient String lastAccessed;
    transient boolean wasDamaged; //used only by the indexer

    PowerModule power;
    ItemModule items;
    LiquidModule liquids;
    ConsumeModule cons;

    private transient float timeScale = 1f, timeScaleDuration;
    private transient float dumpAccum;

    private transient @Nullable SoundLoop sound;

    private transient boolean sleeping;
    private transient float sleepTime;
    private transient boolean initialized;

    /** Sets this tile entity data to this and adds it if necessary. */
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
        if(!initialized){
            create(tile.block(), team);
        }else{
            if(block.hasPower){
                power.init = false;
                //reinit power graph
                new PowerGraph().add(self());
            }
        }
        proximity.clear();
        this.rotation = rotation;
        this.tile = tile;

        set(tile.drawx(), tile.drawy());

        if(shouldAdd){
            add();
        }

        created();

        return self();
    }

    /** Sets up all the necessary variables, but does not add this entity anywhere. */
    public Building create(Block block, Team team){
        this.block = block;
        this.team = team;

        if(block.loopSound != Sounds.none){
            sound = new SoundLoop(block.loopSound, block.loopSoundVolume);
        }

        health = block.health;
        maxHealth(block.health);
        timer(new Interval(block.timers));

        cons = new ConsumeModule(self());
        if(block.hasItems) items = new ItemModule();
        if(block.hasLiquids) liquids = new LiquidModule();
        if(block.hasPower){
            power = new PowerModule();
            power.graph.add(self());
        }

        initialized = true;

        return self();
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
        write.b(rotation | 0b10000000);
        write.b(team.id);
        write.b(1); //version
        write.b(enabled ? 1 : 0);
        if(items != null) items.write(write);
        if(power != null) power.write(write);
        if(liquids != null) liquids.write(write);
        if(cons != null) cons.write(write);
    }

    public final void readBase(Reads read){
        //cap health by block health in case of nerfs
        health = Math.min(read.f(), block.health);
        byte rot = read.b();
        team = Team.get(read.b());

        rotation = rot & 0b01111111;
        boolean legacy = true;
        if((rot & 0b10000000) != 0){
            byte ver = read.b(); //version of entity save
            if(ver == 1){
                byte on = read.b();
                this.enabled = on == 1;
                if(!this.enabled){
                    enabledControlTime = timeToUncontrol;
                }
            }
            legacy = false;
        }

        if(items != null) items.read(read, legacy);
        if(power != null) power.read(read, legacy);
        if(liquids != null) liquids.read(read, legacy);
        if(cons != null) cons.read(read, legacy);
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

    public void addPlan(boolean checkPrevious){
        if(!block.rebuildable || (team == state.rules.defaultTeam && state.isCampaign() && !block.isVisible())) return;

        Object overrideConfig = null;

        if(self() instanceof ConstructBuild entity){
            //update block to reflect the fact that something was being constructed
            if(entity.current != null && entity.current.synthetic() && entity.wasConstructing){
                block = entity.current;
                overrideConfig = entity.lastConfig;
            }else{
                //otherwise this was a deconstruction that was interrupted, don't want to rebuild that
                return;
            }
        }

        TeamData data = team.data();

        if(checkPrevious){
            //remove existing blocks that have been placed here.
            //painful O(n) iteration + copy
            for(int i = 0; i < data.blocks.size; i++){
                BlockPlan b = data.blocks.get(i);
                if(b.x == tile.x && b.y == tile.y){
                    data.blocks.removeIndex(i);
                    break;
                }
            }
        }

        data.blocks.addFirst(new BlockPlan(tile.x, tile.y, (short)rotation, block.id, overrideConfig == null ? config() : overrideConfig));
    }

    /** Configure with the current, local player. */
    public void configure(Object value){
        //save last used config
        block.lastConfig = value;
        Call.tileConfig(player, self(), value);
    }

    /** Configure from a server. */
    public void configureAny(Object value){
        Call.tileConfig(null, self(), value);
    }

    /** Deselect this tile from configuration. */
    public void deselect(){
        if(!headless && control.input.frag.config.getSelectedTile() == self()){
            control.input.frag.config.hideConfig();
        }
    }

    /** Called clientside when the client taps a block to config.
     * @return whether the configuration UI should be shown. */
    public boolean configTapped(){
        return true;
    }

    public void applyBoost(float intensity, float duration){
        //do not refresh time scale when getting a weaker intensity
        if(intensity >= this.timeScale){
            timeScaleDuration = Math.max(timeScaleDuration, duration);
        }
        timeScale = Math.max(timeScale, intensity);
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
        //disabled -> 0 efficiency
        if(!enabled) return 0;
        return power != null && (block.consumes.has(ConsumeType.power) && !block.consumes.getPower().buffered) ? power.status : 1f;
    }

    public BlockStatus status(){
        return cons.status();
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

    /** @return whether the player can select (but not actually control) this building. */
    public boolean canControlSelect(Player player){
        return false;
    }

    /** Called when a player control-selects this building - not called for ControlBlock subclasses. */
    public void onControlSelect(Player player){

    }

    public void acceptPlayerPayload(Player player, Cons<Payload> grabber){
        Fx.spawn.at(player);
        var unit = player.unit();
        player.clearUnit();
        //player.deathTimer = Player.deathDelay + 1f; //for instant respawn
        unit.remove();
        grabber.get(new UnitPayload(unit));
        Fx.unitDrop.at(unit);
        if(Vars.net.client()){
            Vars.netClient.clearRemovedEntity(unit.id);
        }
    }

    public boolean canUnload(){
        return block.unloadable;
    }

    /** Called when an unloader takes an item. */
    public void itemTaken(Item item){

    }

    /** Called when this block is dropped as a payload. */
    public void dropped(){

    }

    /** This is for logic blocks. */
    public void handleString(Object value){

    }

    public void created(){}

    public boolean shouldConsume(){
        return enabled;
    }

    public boolean productionValid(){
        return true;
    }

    public float getPowerProduction(){
        return 0f;
    }

    /** Returns the amount of items this block can accept. */
    public int acceptStack(Item item, int amount, Teamc source){
        if(acceptItem(self(), item) && block.hasItems && (source == null || source.team() == team)){
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
    public void handleStack(Item item, int amount, @Nullable Teamc source){
        noSleep();
        items.add(item, amount);
    }

    /** Returns offset for stack placement. */
    public void getStackOffset(Item item, Vec2 trns){

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
    public boolean movePayload(Payload todump){
        int trns = block.size/2 + 1;
        Tile next = tile.nearby(Geometry.d4(rotation).x * trns, Geometry.d4(rotation).y * trns);

        if(next != null && next.build != null && next.build.team == team && next.build.acceptPayload(self(), todump)){
            next.build.handlePayload(self(), todump);
            return true;
        }

        return false;
    }

    /**
     * Tries dumping a payload to any adjacent block.
     * @param todump payload to dump.
     * @return whether the payload was moved successfully
     */
    public boolean dumpPayload(Payload todump){
        if(proximity.size == 0) return false;

        int dump = this.cdump;

        for(int i = 0; i < proximity.size; i++){
            Building other = proximity.get((i + dump) % proximity.size);

            if(other.team == team && other.acceptPayload(self(), todump)){
                other.handlePayload(self(), todump);
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

    public boolean acceptLiquid(Building source, Liquid liquid){
        return block.hasLiquids && block.consumes.liquidfilters.get(liquid.id);
    }

    public void handleLiquid(Building source, Liquid liquid, float amount){
        liquids.add(liquid, amount);
    }

    public void dumpLiquid(Liquid liquid){
        dumpLiquid(liquid, 2f);
    }

    public void dumpLiquid(Liquid liquid, float scaling){
        int dump = this.cdump;

        if(liquids.get(liquid) <= 0.0001f) return;

        if(!net.client() && state.isCampaign() && team == state.rules.defaultTeam) liquid.unlock();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            other = other.getLiquidDestination(self(), liquid);

            if(other != null && other.team == team && other.block.hasLiquids && canDumpLiquid(other, liquid) && other.liquids != null){
                float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
                float fract = liquids.get(liquid) / block.liquidCapacity;

                if(ofract < fract) transferLiquid(other, (fract - ofract) * block.liquidCapacity / scaling, liquid);
            }
        }
    }

    public boolean canDumpLiquid(Building to, Liquid liquid){
        return true;
    }

    public void transferLiquid(Building next, float amount, Liquid liquid){
        float flow = Math.min(next.block.liquidCapacity - next.liquids.get(liquid), amount);

        if(next.acceptLiquid(self(), liquid)){
            next.handleLiquid(self(), liquid, flow);
            liquids.remove(liquid, flow);
        }
    }

    public float moveLiquidForward(boolean leaks, Liquid liquid){
        Tile next = tile.nearby(rotation);

        if(next == null) return 0;

        if(next.build != null){
            return moveLiquid(next.build, liquid);
        }else if(leaks && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = liquids.get(liquid) / 1.5f;
            Puddles.deposit(next, tile, liquid, leakAmount);
            liquids.remove(liquid, leakAmount);
        }
        return 0;
    }

    public float moveLiquid(Building next, Liquid liquid){
        if(next == null) return 0;

        next = next.getLiquidDestination(self(), liquid);

        if(next.team == team && next.block.hasLiquids && liquids.get(liquid) > 0f){
            float ofract = next.liquids.get(liquid) / next.block.liquidCapacity;
            float fract = liquids.get(liquid) / block.liquidCapacity * block.liquidPressure;
            float flow = Math.min(Mathf.clamp((fract - ofract)) * (block.liquidCapacity), liquids.get(liquid));
            flow = Math.min(flow, next.block.liquidCapacity - next.liquids.get(liquid));

            if(flow > 0f && ofract <= fract && next.acceptLiquid(self(), liquid)){
                next.handleLiquid(self(), liquid, flow);
                liquids.remove(liquid, flow);
                return flow;
            }else if(next.liquids.currentAmount() / next.block.liquidCapacity > 0.1f && fract > 0.1f){
                //TODO these are incorrect effect positions
                float fx = (x + next.x) / 2f, fy = (y + next.y) / 2f;

                Liquid other = next.liquids.current();
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
        return 0;
    }

    public Building getLiquidDestination(Building from, Liquid liquid){
        return self();
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
        produced(item, 1);
        int dump = this.cdump;
        if(!net.client() && state.isCampaign() && team == state.rules.defaultTeam) item.unlock();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            if(other.team == team && other.acceptItem(self(), item) && canDump(other, item)){
                other.handleItem(self(), item);
                return;
            }
        }

        handleItem(self(), item);
    }

    /**
     * Tries to put this item into a nearby container. Returns success. Unlike #offload(), this method does not change the block inventory.
     */
    public boolean put(Item item){
        int dump = this.cdump;

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);
            Building other = proximity.get((i + dump) % proximity.size);
            if(other.team == team && other.acceptItem(self(), item) && canDump(other, item)){
                other.handleItem(self(), item);
                return true;
            }
        }

        return false;
    }

    public void produced(Item item){
        produced(item, 1);
    }

    public void produced(Item item, int amount){
        if(Vars.state.rules.sector != null && team == state.rules.defaultTeam) Vars.state.rules.sector.info.handleProduction(item, amount);
    }

    /** Dumps any item with an accumulator. May dump multiple times per frame. Use with care. */
    public void dumpAccumulate(){
        dumpAccumulate(null);
    }

    /** Dumps any item with an accumulator. May dump multiple times per frame. Use with care. */
    public void dumpAccumulate(Item item){
        dumpAccum += delta();
        while(dumpAccum >= 1f){
            dump(item);
            dumpAccum -=1f;
        }
    }

    /** Try dumping any item near the building. */
    public boolean dump(){
        return dump(null);
    }

    /**
     * Try dumping a specific item near the building.
     * @param todump Item to dump. Can be null to dump anything.
     */
    public boolean dump(Item todump){
        if(!block.hasItems || items.total() == 0 || (todump != null && !items.has(todump))) return false;

        int dump = this.cdump;

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Building other = proximity.get((i + dump) % proximity.size);

            if(todump == null){

                for(int ii = 0; ii < content.items().size; ii++){
                    Item item = content.item(ii);

                    if(other.team == team && items.has(item) && other.acceptItem(self(), item) && canDump(other, item)){
                        other.handleItem(self(), item);
                        items.remove(item, 1);
                        incrementDump(proximity.size);
                        return true;
                    }
                }
            }else{
                if(other.team == team && other.acceptItem(self(), todump) && canDump(other, todump)){
                    other.handleItem(self(), todump);
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
        cdump = (byte)((cdump + 1) % prox);
    }

    /** Used for dumping items. */
    public boolean canDump(Building to, Item item){
        return true;
    }

    /** Try offloading an item to a nearby container in its facing direction. Returns true if success. */
    public boolean moveForward(Item item){
        Building other = front();
        if(other != null && other.team == team && other.acceptItem(self(), item)){
            other.handleItem(self(), item);
            return true;
        }
        return false;
    }

    /** Called shortly before this building is removed. */
    public void onProximityRemoved(){
        if(power != null){
            powerGraphRemoved();
        }
    }

    /** Called after this building is created in the world. May be called multiple times, or when adjacent buildings change. */
    public void onProximityAdded(){
        if(power != null){
            updatePowerGraph();
        }
    }

    /** Called when anything adjacent to this building is placed/removed, including itself. */
    public void onProximityUpdate(){
        noSleep();
    }

    public void updatePowerGraph(){
        for(Building other : getPowerConnections(tempBuilds)){
            if(other.power != null){
                other.power.graph.addGraph(power.graph);
            }
        }
    }

    public void powerGraphRemoved(){
        if(power == null) return;

        power.graph.remove(self());
        for(int i = 0; i < power.links.size; i++){
            Tile other = world.tile(power.links.get(i));
            if(other != null && other.build != null && other.build.power != null){
                other.build.power.links.removeValue(pos());
            }
        }
        power.links.clear();
    }
    
    public boolean conductsTo(Building other){
        return !block.insulated;
    }

    public Seq<Building> getPowerConnections(Seq<Building> out){
        out.clear();
        if(power == null) return out;

        for(Building other : proximity){
            if(other != null && other.power != null
            && other.team == team
            && !(block.consumesPower && other.block.consumesPower && !block.outputsPower && !other.block.outputsPower)
            && conductsTo(other) && other.conductsTo(self()) && !power.links.contains(other.pos())){
                out.add(other);
            }
        }

        for(int i = 0; i < power.links.size; i++){
            Tile link = world.tile(power.links.get(i));
            if(link != null && link.build != null && link.build.power != null && link.build.team == team) out.add(link.build);
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
    public boolean shouldAmbientSound(){
        return shouldConsume();
    }

    public void drawStatus(){
        if(block.enableDrawStatus && block.consumes.any()){
            float multiplier = block.size > 1 ? 1 : 0.64f;
            float brcx = x + (block.size * tilesize / 2f) - (tilesize * multiplier / 2f);
            float brcy = y - (block.size * tilesize / 2f) + (tilesize * multiplier / 2f);

            Draw.z(Layer.power + 1);
            Draw.color(Pal.gray);
            Fill.square(brcx, brcy, 2.5f * multiplier, 45);
            Draw.color(status().color);
            Fill.square(brcx, brcy, 1.5f * multiplier, 45);
            Draw.color();
        }
    }

    public void drawCracks(){
        if(!damaged() || block.size > BlockRenderer.maxCrackSize) return;
        int id = pos();
        TextureRegion region = renderer.blocks.cracks[block.size - 1][Mathf.clamp((int)((1f - healthf()) * BlockRenderer.crackRegions), 0, BlockRenderer.crackRegions-1)];
        Draw.colorl(0.2f, 0.1f + (1f - healthf())* 0.6f);
        Draw.rect(region, x, y, (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(){
    }

    public void drawDisabled(){
        Draw.color(Color.scarlet);
        Draw.alpha(0.8f);

        float size = 6f;
        Draw.rect(Icon.cancel.getRegion(), x, y, size, size);

        Draw.reset();
    }

    public void draw(){
        if(block.variants == 0){
            Draw.rect(block.region, x, y, block.rotate ? rotdeg() : 0);
        }else{
            Draw.rect(block.variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, block.variantRegions.length - 1))], x, y, block.rotate ? rotdeg() : 0);
        }

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
    public void playerPlaced(Object config){

    }

    /** Called after the block is placed by anyone. */
    @CallSuper
    public void placed(){
        if(net.client()) return;

        if((block.consumesPower || block.outputsPower) && block.hasPower){
            PowerNode.getNodeLinks(tile, block, team, other -> {
                if(!other.power.links.contains(pos())){
                    other.configureAny(pos());
                }
            });
        }
    }

    /**
     * Called when a block is placed over some other blocks. This seq will always have at least one item.
     * Should load some previous state, if necessary. */
    public void overwrote(Seq<Building> previous){

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
    public void configured(@Nullable Unit builder, @Nullable Object value){
        //null is of type void.class; anonymous classes use their superclass.
        Class<?> type = value == null ? void.class : value.getClass().isAnonymousClass() ? value.getClass().getSuperclass() : value.getClass();

        if(value instanceof Item) type = Item.class;
        if(value instanceof Block) type = Block.class;
        if(value instanceof Liquid) type = Liquid.class;
        
        if(builder != null && builder.isPlayer()){
            lastAccessed = builder.getPlayer().name;
        }

        if(block.configurations.containsKey(type)){
            block.configurations.get(type).get(this, value);
        }else if(value instanceof Building build){
            //copy config of another building
            var conf = build.config();
            if(conf != null && !(conf instanceof Building)){
                configured(builder, conf);
            }
        }
    }

    /** Called when the block is tapped by the local player. */
    public void tapped(){

    }

    /** Called *after* the tile has been removed. */
    public void afterDestroyed(){

    }

    /** @return the cap for item amount calculations, used when this block explodes. */
    public int explosionItemCap(){
        return block.itemCapacity;
    }

    /** Called when the block is destroyed. The tile is still intact at this stage. */
    public void onDestroyed(){
        float explosiveness = block.baseExplosiveness;
        float flammability = 0f;
        float power = 0f;

        if(block.hasItems){
            for(Item item : content.items()){
                int amount = Math.min(items.get(item), explosionItemCap());
                explosiveness += item.explosiveness * amount;
                flammability += item.flammability * amount;
                power += item.charge * amount * 100f;
            }
        }

        if(block.hasLiquids){
            flammability += liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
            explosiveness += liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
        }

        if(block.consumes.hasPower() && block.consumes.getPower().buffered){
            power += this.power.status * block.consumes.getPower().capacity;
        }

        if(block.hasLiquids && state.rules.damageExplosions){

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

        Damage.dynamicExplosion(x, y, flammability, explosiveness * 3.5f, power, tilesize * block.size / 2f, state.rules.damageExplosions, block.destroyEffect);

        if(!floor().solid && !floor().isLiquid){
            Effect.rubble(x, y, block.size);
        }
    }

    public String getDisplayName(){
        //derelict team icon currently doesn't display
        return team == Team.derelict ?
            block.localizedName + "\n" + Core.bundle.get("block.derelict") :
            block.localizedName + (team == player.team() || team.emoji.isEmpty() ? "" : " " + team.emoji);
    }

    public TextureRegion getDisplayIcon(){
        return block.uiIcon;
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

            boolean displayFlow = (block.category == Category.distribution || block.category == Category.liquid) && block.displayFlow;

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
                                    l.image(item.uiIcon).padRight(3f);
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
                        boolean[] had = {false};

                        Runnable rebuild = () -> {
                            l.clearChildren();
                            l.left();
                            l.image(() -> liquids.current().uiIcon).padRight(3f);
                            l.label(() -> liquids.getFlowRate() < 0 ? "..." : Strings.fixed(liquids.getFlowRate(), 2) + ps).color(Color.lightGray);
                        };

                        l.update(() -> {
                           if(!had[0] && liquids.hadFlow()){
                               had[0] = true;
                               rebuild.run();
                           }
                        });
                    }).left();
                }
            }

            if(net.active() && lastAccessed != null){
                table.row();
                table.add(Core.bundle.format("lastaccessed", lastAccessed)).growX().wrap().left();
            }

            table.marginBottom(-5);
        }
    }

    public void displayConsumption(Table table){
        table.left();
        for(Consume cons : block.consumes.all()){
            if(cons.isOptional() && cons.isBoost()) continue;
            cons.build(self(), table);
        }
    }

    public void displayBars(Table table){
        for(Func<Building, Bar> bar : block.bars.list()){
            //TODO fix conclusively
            try{
                table.add(bar.get(self())).growX();
                table.row();
            }catch(ClassCastException e){
                break;
            }
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
        return block.configurable && interactable(player.team()) ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when another tile is tapped while this block is selected.
     * @return whether or not this block should be deselected.
     */
    public boolean onConfigureTileTapped(Building other){
        return self() != other;
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
        damage(other.team, other.damage() * other.type().buildingDamageMultiplier);

        return true;
    }

    /** Used to handle damage from splash damage for certain types of blocks. */
    public void damage(@Nullable Team source, float damage){
        damage(damage);
    }

    /** Changes this building's team in a safe manner. */
    public void changeTeam(Team next){
        Team last = this.team;
        indexer.removeIndex(tile);
        this.team = next;
        indexer.addIndex(tile);
        Events.fire(teamChangeEvent.set(last, self()));
    }

    public boolean canPickup(){
        return true;
    }

    public void pickedUp(){

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
            other.proximity.remove(self(), true);
            other.onProximityUpdate();
        }
        proximity.clear();
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();
        
        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Building other = world.build(tile.x + point.x, tile.y + point.y);

            if(other == null || !(other.tile.interactable(team))) continue;

            //add this tile to proximity of nearby tiles
            if(!other.proximity.contains(self(), true)){
                other.proximity.add(self());
            }

            tmpTiles.add(other);
        }

        //using a set to prevent duplicates
        for(Building tile : tmpTiles){
            proximity.add(tile);
        }

        onProximityAdded();
        onProximityUpdate();

        for(Building other : tmpTiles){
            other.onProximityUpdate();
        }
    }

    public void updateTile(){

    }

    /** @return ambient sound volume scale. */
    public float ambientVolume(){
        return efficiency();
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
        return tile.build == self() && !dead();
    }

    @MethodPriority(100)
    @Override
    public void heal(){
        indexer.notifyBuildHealed(self());
    }

    @MethodPriority(100)
    @Override
    public void heal(float amount){
        indexer.notifyBuildHealed(self());
    }

    @Override
    public float hitSize(){
        return tile.block().size * tilesize;
    }

    @Replace
    @Override
    public void kill(){
        Call.tileDestroyed(self());
    }

    @Replace
    @Override
    public void damage(float damage){
        if(dead()) return;

        float dm = state.rules.blockHealth(team);

        if(Mathf.zero(dm)){
            damage = health + 1;
        }else{
            damage /= dm;
        }

        Call.tileDamage(self(), health - handleDamage(damage));

        if(health <= 0){
            Call.tileDestroyed(self());
        }
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case x -> World.conv(x);
            case y -> World.conv(y);
            case dead -> !isValid() ? 1 : 0;
            case team -> team.id;
            case health -> health;
            case maxHealth -> maxHealth;
            case efficiency -> efficiency();
            case timescale -> timeScale;
            case range -> this instanceof Ranged r ? r.range() / tilesize : 0;
            case rotation -> rotation;
            case totalItems -> items == null ? 0 : items.total();
            case totalLiquids -> liquids == null ? 0 : liquids.total();
            case totalPower -> power == null || !block.consumes.hasPower() ? 0 : power.status * (block.consumes.getPower().buffered ? block.consumes.getPower().capacity : 1f);
            case itemCapacity -> block.hasItems ? block.itemCapacity : 0;
            case liquidCapacity -> block.hasLiquids ? block.liquidCapacity : 0;
            case powerCapacity -> block.consumes.hasPower() ? block.consumes.getPower().capacity : 0f;
            case powerNetIn -> power == null ? 0 : power.graph.getLastScaledPowerIn() * 60;
            case powerNetOut -> power == null ? 0 : power.graph.getLastScaledPowerOut() * 60;
            case powerNetStored -> power == null ? 0 : power.graph.getLastPowerStored();
            case powerNetCapacity -> power == null ? 0 : power.graph.getLastCapacity();
            case enabled -> enabled ? 1 : 0;
            case controlled -> this instanceof ControlBlock c && c.isControlled() ? GlobalConstants.ctrlPlayer : 0;
            case payloadCount -> getPayload() != null ? 1 : 0;
            case size -> block.size;
            default -> Float.NaN; //gets converted to null in logic
        };
    }

    @Override
    public Object senseObject(LAccess sensor){
        return switch(sensor){
            case type -> block;
            case firstItem -> items == null ? null : items.first();
            case config -> block.configSenseable() ? config() : null;
            case payloadType -> getPayload() instanceof UnitPayload p1 ? p1.unit.type : getPayload() instanceof BuildPayload p2 ? p2.block() : null;
            default -> noSensed;
        };
    }

    @Override
    public double sense(Content content){
        if(content instanceof Item i && items != null) return items.get(i);
        if(content instanceof Liquid l && liquids != null) return liquids.get(l);
        return Float.NaN; //invalid sense
    }

    @Override
    public void control(LAccess type, double p1, double p2, double p3, double p4){
        if(type == LAccess.enabled){
            enabled = !Mathf.zero((float)p1);
            enabledControlTime = timeToUncontrol;
        }
    }

    @Override
    public void control(LAccess type, Object p1, double p2, double p3, double p4){
        //don't execute configure instructions that copy logic building configures; this can cause extreme lag
        if(type == LAccess.config && block.logicConfigurable && !(p1 instanceof LogicBuild)){
            //change config only if it's new
            configured(null, p1);
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
        block.destroySound.at(tile);
        onDestroyed();
        tile.remove();
        remove();
        afterDestroyed();
    }

    @Final
    @Override
    public void update(){
        if(state.isEditor()) return;

        timeScaleDuration -= Time.delta;
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(!enabled && block.autoResetEnabled){
            noSleep();
            enabledControlTime -= Time.delta;

            if(enabledControlTime <= 0){
                enabled = true;
            }
        }

        if(team == Team.derelict || !block.supportsEnv(state.rules.environment)){
            enabled = false;
        }

        if(!headless){
            if(sound != null){
                sound.update(x, y, shouldActiveSound());
            }

            if(block.ambientSound != Sounds.none && shouldAmbientSound()){
                control.sound.loop(block.ambientSound, self(), block.ambientSoundVolume * ambientVolume());
            }
        }

        if(enabled || !block.noUpdateDisabled){
            updateTile();
        }

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
