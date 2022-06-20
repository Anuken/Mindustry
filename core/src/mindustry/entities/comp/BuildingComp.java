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
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.heat.HeatConductor.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import java.util.*;

import static mindustry.Vars.*;

@EntityDef(value = {Buildingc.class}, isFinal = false, genio = false, serialize = false)
@Component(base = true)
abstract class BuildingComp implements Posc, Teamc, Healthc, Buildingc, Timerc, QuadTreeObject, Displayable, Senseable, Controllable, Sized{
    //region vars and initialization
    static final float timeToSleep = 60f * 1, recentDamageTime = 60f * 5f;
    static final ObjectSet<Building> tmpTiles = new ObjectSet<>();
    static final Seq<Building> tempBuilds = new Seq<>();
    static final BuildTeamChangeEvent teamChangeEvent = new BuildTeamChangeEvent();
    static final BuildDamageEvent bulletDamageEvent = new BuildDamageEvent();
    static int sleepingEntities = 0;
    
    @Import float x, y, health, maxHealth;
    @Import Team team;

    transient Tile tile;
    transient Block block;
    transient Seq<Building> proximity = new Seq<>(6);
    transient int cdump;
    transient int rotation;
    transient float payloadRotation;
    transient String lastAccessed;
    transient boolean wasDamaged; //used only by the indexer
    transient float visualLiquid;

    /** TODO Each bit corresponds to a team ID. Only 64 are supported. Does not work on servers. */
    transient long visibleFlags;
    transient boolean wasVisible; //used only by the block renderer when fog is on (TODO replace with discovered check?)

    transient boolean enabled = true;
    transient @Nullable Building lastDisabler;

    @Nullable PowerModule power;
    @Nullable ItemModule items;
    @Nullable LiquidModule liquids;

    /** Base efficiency. Takes the minimum value of all consumers. */
    transient float efficiency;
    /** Same as efficiency, but for optional consumers only. */
    transient float optionalEfficiency;
    /** The efficiency this block *would* have if shouldConsume() returned true. */
    transient float potentialEfficiency;

    transient float healSuppressionTime = -1f;
    transient float lastHealTime = -120f * 10f;

    private transient float lastDamageTime = -recentDamageTime;
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
        boolean writeVisibility = state.rules.fog && visibleFlags != 0;

        write.f(health);
        write.b(rotation | 0b10000000);
        write.b(team.id);
        write.b(writeVisibility ? 4 : 3); //version
        write.b(enabled ? 1 : 0);
        //write presence of items/power/liquids/cons, so removing/adding them does not corrupt future saves.
        write.b(moduleBitmask());
        if(items != null) items.write(write);
        if(power != null) power.write(write);
        if(liquids != null) liquids.write(write);

        //efficiency is written as two bytes to save space
        write.b((byte)(Mathf.clamp(efficiency) * 255f));
        write.b((byte)(Mathf.clamp(optionalEfficiency) * 255f));

        //only write visibility when necessary, saving 8 bytes - implies new version
        if(writeVisibility){
            write.l(visibleFlags);
        }
    }

    public final void readBase(Reads read){
        //cap health by block health in case of nerfs
        health = Math.min(read.f(), block.health);
        byte rot = read.b();
        team = Team.get(read.b());

        rotation = rot & 0b01111111;

        int moduleBits = moduleBitmask();
        boolean legacy = true;
        byte version = 0;

        //new version
        if((rot & 0b10000000) != 0){
            version = read.b(); //version of entity save
            if(version >= 1){
                byte on = read.b();
                this.enabled = on == 1;
            }

            //get which modules should actually be read; this was added in version 2
            if(version >= 2){
                moduleBits = read.b();
            }
            legacy = false;
        }

        if((moduleBits & 1) != 0) (items == null ? new ItemModule() : items).read(read, legacy);
        if((moduleBits & 2) != 0) (power == null ? new PowerModule() : power).read(read, legacy);
        if((moduleBits & 4) != 0) (liquids == null ? new LiquidModule() : liquids).read(read, legacy);

        //unnecessary consume module read in version 2 and below
        if(version <= 2) read.bool();

        //version 3 has efficiency numbers instead of bools
        if(version >= 3){
            efficiency = potentialEfficiency = read.ub() / 255f;
            optionalEfficiency = read.ub() / 255f;
        }

        //version 4 (and only 4 at the moment) has visibility flags
        if(version == 4){
            visibleFlags = read.l();
        }
    }

    public int moduleBitmask(){
        return (items != null ? 1 : 0) | (power != null ? 2 : 0) | (liquids != null ? 4 : 0) | 8;
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
        addPlan(checkPrevious, false);
    }

    public void addPlan(boolean checkPrevious, boolean ignoreConditions){
        if(!ignoreConditions && (!block.rebuildable || (team == state.rules.defaultTeam && state.isCampaign() && !block.isVisible()))) return;

        Object overrideConfig = null;
        Block toAdd = this.block;

        if(self() instanceof ConstructBuild entity){
            //update block to reflect the fact that something was being constructed
            if(entity.current != null && entity.current.synthetic() && entity.wasConstructing){
                toAdd = entity.current;
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
            for(int i = 0; i < data.plans.size; i++){
                BlockPlan b = data.plans.get(i);
                if(b.x == tile.x && b.y == tile.y){
                    data.plans.removeIndex(i);
                    break;
                }
            }
        }

        data.plans.addFirst(new BlockPlan(tile.x, tile.y, (short)rotation, toAdd.id, overrideConfig == null ? config() : overrideConfig));
    }

    public @Nullable Tile findClosestEdge(Position to, Boolf<Tile> solid){
        Tile best = null;
        float mindst = 0f;
        for(var point : Edges.getEdges(block.size)){
            Tile other = Vars.world.tile(tile.x + point.x, tile.y + point.y);
            if(other != null && !solid.get(other) && (best == null || to.dst2(other) < mindst)){
                best = other;
                mindst = other.dst2(other);
            }
        }
        return best;
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
        if(!headless && control.input.config.getSelected() == self()){
            control.input.config.hideConfig();
        }
    }

    /** Called clientside when the client taps a block to config.
     * @return whether the configuration UI should be shown. */
    public boolean configTapped(){
        return true;
    }

    public float calculateHeat(float[] sideHeat){
        return calculateHeat(sideHeat, null);
    }

    //TODO can cameFrom be an IntSet?
    public float calculateHeat(float[] sideHeat, @Nullable IntSet cameFrom){
        Arrays.fill(sideHeat, 0f);
        if(cameFrom != null) cameFrom.clear();

        float heat = 0f;

        for(var edge : block.getEdges()){
            Building build = nearby(edge.x, edge.y);
            if(build != null && build.team == team && build instanceof HeatBlock heater && (!build.block.rotate || (relativeTo(build) + 2) % 4 == build.rotation)){ //TODO hacky

                //if there's a cycle, ignore its heat
                if(!(build instanceof HeatConductorBuild hc && hc.cameFrom.contains(id()))){
                    //heat is distributed across building size
                    float add = heater.heat() / build.block.size;

                    sideHeat[Mathf.mod(relativeTo(build), 4)] += add;
                    heat += add;
                }

                //register traversed cycles
                if(cameFrom != null){
                    cameFrom.add(build.id);
                    if(build instanceof HeatConductorBuild hc){
                        cameFrom.addAll(hc.cameFrom);
                    }
                }
            }
        }
        return heat;
    }

    public void applyBoost(float intensity, float duration){
        //do not refresh time scale when getting a weaker intensity
        if(intensity >= this.timeScale - 0.001f){
            timeScaleDuration = Math.max(timeScaleDuration, duration);
        }
        timeScale = Math.max(timeScale, intensity);
    }

    public void applySlowdown(float intensity, float duration){
        //do not refresh time scale when getting a weaker intensity
        if(intensity <= this.timeScale - 0.001f){
            timeScaleDuration = Math.max(timeScaleDuration, duration);
        }
        timeScale = Math.min(timeScale, intensity);
    }

    public void applyHealSuppression(float amount){
        healSuppressionTime = Math.max(healSuppressionTime, Time.time + amount);
    }

    public boolean isHealSuppressed(){
        return block.suppressable && Time.time <= healSuppressionTime;
    }

    public void recentlyHealed(){
        lastHealTime = Time.time;
    }

    public boolean wasRecentlyHealed(float duration){
        return lastHealTime + duration >= Time.time;
    }

    public boolean wasRecentlyDamaged(){
        return lastDamageTime + recentDamageTime >= Time.time;
    }

    public Building nearby(int dx, int dy){
        return world.build(tile.x + dx, tile.y + dy);
    }

    public Building nearby(int rotation){
        return switch(rotation){
            case 0 -> world.build(tile.x + 1, tile.y);
            case 1 -> world.build(tile.x, tile.y + 1);
            case 2 -> world.build(tile.x - 1, tile.y);
            case 3 -> world.build(tile.x, tile.y - 1);
            default -> null;
        };
    }

    public byte relativeTo(Tile tile){
        return relativeTo(tile.x, tile.y);
    }

    public byte relativeTo(Building build){
        if(Math.abs(x - build.x) > Math.abs(y - build.y)){
            if(x <= build.x - 1) return 0;
            if(x >= build.x + 1) return 2;
        }else{
            if(y <= build.y - 1) return 1;
            if(y >= build.y + 1) return 3;
        }
        return -1;
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

    /** Any class that overrides this method and changes the value must call Vars.fogControl.forceUpdate(team). */
    public float fogRadius(){
        return block.fogRadius;
    }

    public int pos(){
        return tile.pos();
    }

    public float rotdeg(){
        return rotation * 90;
    }

    /** @return preferred rotation of main texture region to be drawn */
    public float drawrot(){
        return block.rotate && block.rotateDraw ? rotation * 90 : 0f;
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

    /**
     * @return the building's 'warmup', a smooth value from 0 to 1.
     * usually used for crafters and things that need to spin up before reaching full efficiency.
     * many blocks will just return 0.
     * */
    public float warmup(){
        return 0f;
    }

    /** @return total time this block has been producing something; non-crafter blocks usually return Time.time. */
    public float totalProgress(){
        return Time.time;
    }

    public float progress(){
        return 0f;
    }

    /** @return whether this block is allowed to update based on team/environment */
    public boolean allowUpdate(){
        return team != Team.derelict && block.supportsEnv(state.rules.env) &&
            //check if outside map limit
            (!state.rules.limitMapArea || !state.rules.disableOutsideArea || Rect.contains(state.rules.limitX, state.rules.limitY, state.rules.limitWidth, state.rules.limitHeight, tile.x, tile.y));
    }

    public BlockStatus status(){
        if(!enabled){
            return BlockStatus.logicDisable;
        }

        if(!shouldConsume()){
            return BlockStatus.noOutput;
        }

        if(efficiency <= 0 || !productionValid()){
            return BlockStatus.noInput;
        }

        return BlockStatus.active;
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
    public boolean canControlSelect(Unit player){
        return false;
    }

    /** Called when a player control-selects this building - not called for ControlBlock subclasses. */
    public void onControlSelect(Unit player){

    }

    /** Called when this building receives a position command. Requires a commandable block. */
    public void onCommand(Vec2 target){

    }

    /** @return the position that this block points to for commands, or null. */
    public @Nullable Vec2 getCommandPosition(){
        return null;
    }

    public void handleUnitPayload(Unit unit, Cons<Payload> grabber){
        Fx.spawn.at(unit);

        if(unit.isPlayer()){
            unit.getPlayer().clearUnit();
        }

        unit.remove();

        //needs new ID as it is now a payload
        if(net.client()){
            unit.id = EntityGroup.nextId();
        }else{
            //server-side, this needs to be delayed until next frame because otherwise the packets sent out right after this event would have the wrong unit ID, leading to ghosts
            Core.app.post(() -> unit.id = EntityGroup.nextId());
        }

        grabber.get(new UnitPayload(unit));
        Fx.unitDrop.at(unit);
    }

    public boolean canWithdraw(){
        return true;
    }

    public boolean canUnload(){
        return block.unloadable;
    }

    public boolean canResupply(){
        return block.allowResupply;
    }

    public boolean payloadCheck(int conveyorRotation){
        return block.rotate && (rotation + 2) % 4 == conveyorRotation;
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

    /** @return whether this block is currently "active" and should be consuming requirements. */
    public boolean shouldConsume(){
        return enabled;
    }

    public boolean productionValid(){
        return true;
    }

    /** @return whether this building is currently "burning" a trigger consumer (an item) - if true, valid() on those will return true. */
    public boolean consumeTriggerValid(){
        return false;
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
        return block.consumesItem(item) && items.get(item) < getMaximumAccepted(item);
    }

    public boolean acceptLiquid(Building source, Liquid liquid){
        return block.hasLiquids && block.consumesLiquid(liquid);
    }

    public void handleLiquid(Building source, Liquid liquid, float amount){
        liquids.add(liquid, amount);
    }

    //TODO entire liquid system is awful
    public void dumpLiquid(Liquid liquid){
        dumpLiquid(liquid, 2f);
    }

    public void dumpLiquid(Liquid liquid, float scaling){
        dumpLiquid(liquid, scaling, -1);
    }

    /** @param outputDir output liquid direction relative to rotation, or -1 to use any direction. */
    public void dumpLiquid(Liquid liquid, float scaling, int outputDir){
        int dump = this.cdump;

        if(liquids.get(liquid) <= 0.0001f) return;

        if(!net.client() && state.isCampaign() && team == state.rules.defaultTeam) liquid.unlock();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(proximity.size);

            Building other = proximity.get((i + dump) % proximity.size);
            if(outputDir != -1 && (outputDir + rotation) % 4 != relativeTo(other)) continue;

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
            Puddles.deposit(next, tile, liquid, leakAmount, true, true);
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
                //handle reactions between different liquid types ▼
            }else if(!next.block.consumesLiquid(liquid) && next.liquids.currentAmount() / next.block.liquidCapacity > 0.1f && fract > 0.1f){
                //TODO !IMPORTANT! uses current(), which is 1) wrong for multi-liquid blocks and 2) causes unwanted reactions, e.g. hydrogen + slag in pump
                //TODO these are incorrect effect positions
                float fx = (x + next.x) / 2f, fy = (y + next.y) / 2f;

                Liquid other = next.liquids.current();
                //TODO liquid reaction handler for extensibility
                if((other.flammability > 0.3f && liquid.temperature > 0.7f) || (liquid.flammability > 0.3f && other.temperature > 0.7f)){
                    damageContinuous(1);
                    next.damageContinuous(1);
                    if(Mathf.chanceDelta(0.1)){
                        Fx.fire.at(fx, fy);
                    }
                }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) || (other.temperature > 0.7f && liquid.temperature < 0.55f)){
                    liquids.remove(liquid, Math.min(liquids.get(liquid), 0.7f * Time.delta));
                    if(Mathf.chanceDelta(0.2f)){
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

    public @Nullable PayloadSeq getPayloads(){
        return null;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.
     */
    public void offload(Item item){
        produced(item, 1);
        int dump = this.cdump;

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
        if(Vars.state.rules.sector != null && team == state.rules.defaultTeam){
            Vars.state.rules.sector.info.handleProduction(item, amount);

            if(!net.client()) item.unlock();
        }
    }

    /** Dumps any item with an accumulator. May dump multiple times per frame. Use with care. */
    public boolean dumpAccumulate(){
        return dumpAccumulate(null);
    }

    /** Dumps any item with an accumulator. May dump multiple times per frame. Use with care. */
    public boolean dumpAccumulate(Item item){
        boolean res = false;
        dumpAccum += delta();
        while(dumpAccum >= 1f){
            res |= dump(item);
            dumpAccum -=1f;
        }
        return res;
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
        cdump = ((cdump + 1) % prox);
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
            && !(block.consumesPower && other.block.consumesPower && !block.outputsPower && !other.block.outputsPower && !block.conductivePower && !other.block.conductivePower)
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
        if(block.enableDrawStatus && block.consumers.length > 0){
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
        if(!block.drawCracks || !damaged() || block.size > BlockRenderer.maxCrackSize) return;
        int id = pos();
        TextureRegion region = renderer.blocks.cracks[block.size - 1][Mathf.clamp((int)((1f - healthf()) * BlockRenderer.crackRegions), 0, BlockRenderer.crackRegions-1)];
        Draw.colorl(0.2f, 0.1f + (1f - healthf())* 0.6f);
        //TODO could be random, flipped, pseudorandom, etc
        Draw.rect(region, x, y, (id%4)*90);
        Draw.color();
    }

    /** Draw the block overlay that is shown when a cursor is over the block. */
    public void drawSelect(){
        block.drawOverlay(x, y, rotation);
    }

    public void drawDisabled(){
        Draw.color(Color.scarlet);
        Draw.alpha(0.8f);

        float size = 6f;
        Draw.rect(Icon.cancel.getRegion(), x, y, size, size);

        Draw.reset();
    }

    public void draw(){
        if(block.variants == 0 || block.variantRegions == null){
            Draw.rect(block.region, x, y, drawrot());
        }else{
            Draw.rect(block.variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, block.variantRegions.length - 1))], x, y, drawrot());
        }

        drawTeamTop();
    }

    public void payloadDraw(){
        draw();
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
            //yes, I am updating in draw()... but this is purely visual anyway, better have it here than in update() where it wastes time
            visualLiquid = Mathf.lerpDelta(visualLiquid, liquids.currentAmount(), 0.07f);
            drawLiquidLight(liquids.current(), visualLiquid);
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
        Draw.color(team.color);
        Draw.rect("block-border", x - block.size * tilesize / 2f + 4, y - block.size * tilesize / 2f + 4);
        Draw.color();
    }

    /** @return whether a building has regen/healing suppressed; if so, spawns particles on it. */
    public boolean checkSuppression(){
        if(isHealSuppressed()){
            if(Mathf.chanceDelta(0.03)){
                Fx.regenSuppressParticle.at(x + Mathf.range(block.size * tilesize/2f - 1f), y + Mathf.range(block.size * tilesize/2f - 1f));
            }

            return true;
        }

        return false;
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

    /** @return whether this building is in a payload */
    public boolean isPayload(){
        return tile == emptyTile;
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
        if(value instanceof UnitType) type = UnitType.class;
        
        if(builder != null && builder.isPlayer()){
            lastAccessed = builder.getPlayer().coloredName();
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
        if(block.destroyBullet != null){
            //I really do not like that the bullet will not destroy derelict
            //but I can't do anything about it without using a random team
            //which may or may not cause issues with servers and js
            block.destroyBullet.create(this, Team.derelict, x, y, 0);
        }
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
                power += item.charge * Mathf.pow(amount, 1.1f) * 150f;
            }
        }

        if(block.hasLiquids){
            flammability += liquids.sum((liquid, amount) -> liquid.flammability * amount / 2f);
            explosiveness += liquids.sum((liquid, amount) -> liquid.explosiveness * amount / 2f);
        }

        if(block.consPower != null && block.consPower.buffered){
            power += this.power.status * block.consPower.capacity;
        }

        if(block.hasLiquids && state.rules.damageExplosions){

            liquids.each((liquid, amount) -> {
                float splash = Mathf.clamp(amount / 4f, 0f, 10f);

                for(int i = 0; i < Mathf.clamp(amount / 5, 0, 30); i++){
                    Time.run(i / 2f, () -> {
                        Tile other = world.tileWorld(x + Mathf.range(block.size * tilesize / 2), y + Mathf.range(block.size * tilesize / 2));
                        if(other != null){
                            Puddles.deposit(other, liquid, splash);
                        }
                    });
                }
            });
        }

        Damage.dynamicExplosion(x, y, flammability, explosiveness * 3.5f, power, tilesize * block.size / 2f, state.rules.damageExplosions, block.destroyEffect);

        if(block.createRubble && !floor().solid && !floor().isLiquid){
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

    /** @return the item module to use for flow rate calculations */
    public ItemModule flowItems(){
        return items;
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

                var flowItems = flowItems();

                if(flowItems != null){
                    table.row();
                    table.left();
                    table.table(l -> {
                        Bits current = new Bits();

                        Runnable rebuild = () -> {
                            l.clearChildren();
                            l.left();
                            for(Item item : content.items()){
                                if(flowItems.hasFlowItem(item)){
                                    l.image(item.uiIcon).padRight(3f);
                                    l.label(() -> flowItems.getFlowRate(item) < 0 ? "..." : Strings.fixed(flowItems.getFlowRate(item), 1) + ps).color(Color.lightGray);
                                    l.row();
                                }
                            }
                        };

                        rebuild.run();
                        l.update(() -> {
                            for(Item item : content.items()){
                                if(flowItems.hasFlowItem(item) && !current.get(item.id)){
                                    current.set(item.id);
                                    rebuild.run();
                                }
                            }
                        });
                    }).left();
                }

                if(liquids != null){
                    table.row();
                    table.left();
                    table.table(l -> {
                        Bits current = new Bits();

                        Runnable rebuild = () -> {
                            l.clearChildren();
                            l.left();
                            for(var liquid : content.liquids()){
                                if(liquids.hasFlowLiquid(liquid)){
                                    l.image(liquid.uiIcon).padRight(3f);
                                    l.label(() -> liquids.getFlowRate(liquid) < 0 ? "..." : Strings.fixed(liquids.getFlowRate(liquid), 1) + ps).color(Color.lightGray);
                                    l.row();
                                }
                            }
                        };

                        rebuild.run();
                        l.update(() -> {
                            for(var liquid : content.liquids()){
                                if(liquids.hasFlowLiquid(liquid) && !current.get(liquid.id)){
                                    current.set(liquid.id);
                                    rebuild.run();
                                }
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
        for(Consume cons : block.consumers){
            if(cons.optional && cons.booster) continue;
            cons.build(self(), table);
        }
    }

    public void displayBars(Table table){
        for(Func<Building, Bar> bar : block.listBars()){
            var result = bar.get(self());
            if(result == null) continue;
            table.add(result).growX();
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

    /** Returns whether a hand cursor should be shown over this block. */
    public Cursor getCursor(){
        return block.configurable && interactable(player.team()) ? SystemCursor.hand : SystemCursor.arrow;
    }

    /**
     * Called when another tile is tapped while this building is selected.
     * @return whether this block should be deselected.
     */
    public boolean onConfigureBuildTapped(Building other){
        if(block.clearOnDoubleTap){
            if(self() == other){
                deselect();
                configure(null);
                return false;
            }
            return true;
        }
        return self() != other;
    }

    /**
     * Called when a position is tapped while this building is selected.
     *
     * @return whether the tap event is consumed - if true, the player will not start shooting or interact with things under the cursor.
     * */
    public boolean onConfigureTapped(float x, float y){
        return false;
    }

    /**
     * Called when this block's config menu is closed.
     */
    public void onConfigureClosed(){}

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

    public boolean absorbLasers(){
        return block.absorbLasers;
    }

    public boolean isInsulated(){
        return block.insulated;
    }

    public boolean collide(Bullet other){
        return true;
    }

    /** Handle a bullet collision.
     * @return whether the bullet should be removed. */
    public boolean collision(Bullet other){
        damage(other.team, other.damage() * other.type().buildingDamageMultiplier);
        Events.fire(bulletDamageEvent.set(self(), other));

        return true;
    }

    /** Used to handle damage from splash damage for certain types of blocks. */
    public void damage(@Nullable Team source, float damage){
        damage(damage);
    }

    /** Handles splash damage with a bullet source. */
    public void damage(Bullet bullet, Team source, float damage){
        damage(source, damage);
        Events.fire(bulletDamageEvent.set(self(), bullet));
    }

    /** Changes this building's team in a safe manner. */
    public void changeTeam(Team next){
        if(this.team == next) return;

        Team last = this.team;
        boolean was = isValid();

        if(was) indexer.removeIndex(tile);

        this.team = next;

        if(was){
            indexer.addIndex(tile);
            Events.fire(teamChangeEvent.set(last, self()));
        }
    }

    public boolean canPickup(){
        return true;
    }

    /** Called right before this building is picked up. */
    public void pickedUp(){

    }

    /** Called right after this building is picked up. */
    public void afterPickedUp(){
        if(power != null){
            //TODO can lead to ghost graphs?
            power.graph = new PowerGraph();
            power.links.clear();
            if(block.consPower != null && !block.consPower.buffered){
                power.status = 0f;
            }
        }
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

            other.proximity.addUnique(self());

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

    //TODO probably should not have a shouldConsume() check? should you even *use* consValid?

    public void consume(){
        for(Consume cons : block.consumers){
            cons.trigger(self());
        }
    }

    public boolean canConsume(){
        return potentialEfficiency > 0;
    }

    /** Scaled delta. */
    public float delta(){
        return Time.delta * timeScale;
    }

    /** Efficiency * delta. */
    public float edelta(){
        return efficiency * delta();
    }

    /** Called after efficiency is updated but before consumers are updated. Use to apply your own multiplier. */
    public void updateEfficiencyMultiplier(){

    }

    public void updateConsumption(){
        //everything is valid when cheating
        if(!block.hasConsumers || cheating()){
            potentialEfficiency = efficiency = optionalEfficiency = enabled ? 1f : 0f;
            return;
        }

        //disabled -> nothing works
        if(!enabled){
            potentialEfficiency = efficiency = optionalEfficiency = 0f;
            return;
        }

        //TODO why check for old state?
        boolean prevValid = efficiency > 0, update = shouldConsume() && productionValid();

        float minEfficiency = 1f;

        //assume efficiency is 1 for the calculations below
        efficiency = optionalEfficiency = 1f;

        //first pass: get the minimum efficiency of any consumer
        for(var cons : block.nonOptionalConsumers){
            minEfficiency = Math.min(minEfficiency, cons.efficiency(self()));
        }

        //same for optionals
        for(var cons : block.optionalConsumers){
            optionalEfficiency = Math.min(optionalEfficiency, cons.efficiency(self()));
        }

        //efficiency is now this minimum value
        efficiency = minEfficiency;
        optionalEfficiency = Math.min(optionalEfficiency, minEfficiency);

        //assign "potential"
        potentialEfficiency = efficiency;

        //no updating means zero efficiency
        if(!update){
            efficiency = optionalEfficiency = 0f;
        }

        updateEfficiencyMultiplier();

        //second pass: update every consumer based on efficiency
        if(update && prevValid && efficiency > 0){
            for(var cons : block.updateConsumers){
                cons.update(self());
            }
        }
    }

    public void updatePayload(@Nullable Unit unitHolder, @Nullable Building buildingHolder){
        update();
    }

    public void updateTile(){

    }

    /** @return ambient sound volume scale. */
    public float ambientVolume(){
        return efficiency;
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
        lastDamageTime = Time.time;

        if(Mathf.zero(dm)){
            damage = health + 1;
        }else{
            damage = Damage.applyArmor(damage, block.armor) / dm;
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
            case color -> Color.toDoubleBits(team.color.r, team.color.g, team.color.b, 1f);
            case dead -> !isValid() ? 1 : 0;
            case team -> team.id;
            case health -> health;
            case maxHealth -> maxHealth;
            case efficiency -> efficiency;
            case timescale -> timeScale;
            case range -> this instanceof Ranged r ? r.range() / tilesize : 0;
            case rotation -> rotation;
            case totalItems -> items == null ? 0 : items.total();
            //totalLiquids is inherently bad design, but unfortunately it is useful for conduits/tanks
            case totalLiquids -> liquids == null ? 0 : liquids.currentAmount();
            case totalPower -> power == null || block.consPower == null ? 0 : power.status * (block.consPower.buffered ? block.consPower.capacity : 1f);
            case itemCapacity -> block.hasItems ? block.itemCapacity : 0;
            case liquidCapacity -> block.hasLiquids ? block.liquidCapacity : 0;
            case powerCapacity -> block.consPower != null ? block.consPower.capacity : 0f;
            case powerNetIn -> power == null ? 0 : power.graph.getLastScaledPowerIn() * 60;
            case powerNetOut -> power == null ? 0 : power.graph.getLastScaledPowerOut() * 60;
            case powerNetStored -> power == null ? 0 : power.graph.getLastPowerStored();
            case powerNetCapacity -> power == null ? 0 : power.graph.getLastCapacity();
            case enabled -> enabled ? 1 : 0;
            case controlled -> this instanceof ControlBlock c && c.isControlled() ? GlobalVars.ctrlPlayer : 0;
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

    @Replace
    @Override
    public boolean inFogTo(Team viewer){
        if(team == viewer || !state.rules.fog) return false;

        int size = block.size, of = block.sizeOffset, tx = tile.x, ty = tile.y;

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(fogControl.isVisibleTile(viewer, tx + x + of, ty + y + of)){
                    return false;
                }
            }
        }

        return true;
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
        if(tile != emptyTile){
            tile.remove();
        }
        remove();
        afterDestroyed();
    }

    //TODO atrocious method and should be squished
    @Final
    @Replace
    @Override
    public void update(){
        //TODO should just avoid updating buildings instead
        if(state.isEditor()) return;

        //TODO refactor to timestamp-based system?
        if((timeScaleDuration -= Time.delta) <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(!allowUpdate()){
            enabled = false;
        }

        //TODO separate system for sound? AudioSource, etc
        if(!headless){
            if(sound != null){
                sound.update(x, y, shouldActiveSound());
            }

            if(block.ambientSound != Sounds.none && shouldAmbientSound()){
                control.sound.loop(block.ambientSound, self(), block.ambientSoundVolume * ambientVolume());
            }
        }

        updateConsumption();

        //TODO just handle per-block instead
        if(enabled || !block.noUpdateDisabled){
            updateTile();
        }
    }

    @Override
    public void hitbox(Rect out){
        out.setCentered(x, y, block.size * tilesize, block.size * tilesize);
    }

    //endregion
}
