package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.logic.LogicFx.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.logic.LogicDisplay.*;
import mindustry.world.blocks.logic.MemoryBlock.*;
import mindustry.world.blocks.logic.MessageBlock.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LExecutor{
    public static final int maxInstructions = 1000;

    public static final int
    maxGraphicsBuffer = 256,
    maxDisplayBuffer = 1024,
    maxTextBuffer = 400;


    public LInstruction[] instructions = {};
    /** Non-constant variables used for network sync */
    public LVar[] vars = {};
    
    public LVar counter, unit, thisv, ipt;
    
    public int[] binds;
    public boolean yield;

    public LongSeq graphicsBuffer = new LongSeq();
    public StringBuilder textBuffer = new StringBuilder();
    public Building[] links = {};
    public @Nullable LogicBuild build;
    public IntSet linkIds = new IntSet();
    public Team team = Team.derelict;
    public boolean privileged = false;

    //yes, this is a minor memory leak, but it's probably not significant enough to matter
    protected static IntFloatMap unitTimeouts = new IntFloatMap();

    static{
        Events.on(ResetEvent.class, e -> unitTimeouts.clear());
    }

    boolean timeoutDone(Unit unit, float delay){
        return Time.time >= unitTimeouts.get(unit.id) + delay;
    }

    void updateTimeout(Unit unit){
        unitTimeouts.put(unit.id, Time.time);
    }

    public boolean initialized(){
        return instructions.length > 0;
    }

    /** Runs a single instruction. */
    public void runOnce(){
        //reset to start
        if(counter.numval >= instructions.length || counter.numval < 0){
            counter.numval = 0;
        }

        if(counter.numval < instructions.length){
            instructions[(int)(counter.numval++)].run(this);
        }
    }

    /** Loads with a specified assembler. Resets all variables. */
    public void load(LAssembler builder){
        vars = builder.vars.values().toSeq().retainAll(var -> !var.constant).toArray(LVar.class);
        for(int i = 0; i < vars.length; i++){
            vars[i].id = i;
        }

        instructions = builder.instructions;
        counter = builder.getVar("@counter");
        unit = builder.getVar("@unit");
        thisv = builder.getVar("@this");
        ipt = builder.putConst("@ipt", build != null ? build.ipt : 0);
    }

    //region utility

    /** @return a Var from this processor. May be null if out of bounds. */
    public @Nullable LVar optionalVar(int index){
        return index < 0 || index >= vars.length ? null : vars[index];
    }

    //endregion

    //region instruction types

    public interface LInstruction{
        void run(LExecutor exec);
    }

    /** Binds the processor to a unit based on some filters. */
    public static class UnitBindI implements LInstruction{
        public LVar type;

        public UnitBindI(LVar type){
            this.type = type;
        }

        public UnitBindI(){
        }

        @Override
        public void run(LExecutor exec){
            if(exec.binds == null || exec.binds.length != content.units().size){
                exec.binds = new int[content.units().size];
            }

            //binding to `null` was previously possible, but was too powerful and exploitable
            if(type.obj() instanceof UnitType type && type.logicControllable){
                Seq<Unit> seq = exec.team.data().unitCache(type);

                if(seq != null && seq.any()){
                    exec.binds[type.id] %= seq.size;
                    if(exec.binds[type.id] < seq.size){
                        //bind to the next unit
                        exec.unit.setconst(seq.get(exec.binds[type.id]));
                    }
                    exec.binds[type.id] ++;
                }else{
                    //no units of this type found
                    exec.unit.setconst(null);
                }
            }else if(type.obj() instanceof Unit u && (u.team == exec.team || exec.privileged) && u.type.logicControllable){
                //bind to specific unit object
                exec.unit.setconst(u);
            }else{
                exec.unit.setconst(null);
            }
        }
    }

    /** Uses a unit to find something that may not be in its range. */
    public static class UnitLocateI implements LInstruction{
        public LLocate locate = LLocate.building;
        public BlockFlag flag = BlockFlag.core;
        public LVar enemy, ore;
        public LVar outX, outY, outFound, outBuild;

        public UnitLocateI(LLocate locate, BlockFlag flag, LVar enemy, LVar ore, LVar outX, LVar outY, LVar outFound, LVar outBuild){
            this.locate = locate;
            this.flag = flag;
            this.enemy = enemy;
            this.ore = ore;
            this.outX = outX;
            this.outY = outY;
            this.outFound = outFound;
            this.outBuild = outBuild;
        }

        public UnitLocateI(){
        }

        @Override
        public void run(LExecutor exec){
            Object unitObj = exec.unit.obj();
            LogicAI ai = UnitControlI.checkLogicAI(exec, unitObj);

            if(unitObj instanceof Unit unit && ai != null){
                ai.controlTimer = LogicAI.logicControlTimeout;

                Cache cache = (Cache)ai.execCache.get(this, Cache::new);

                if(ai.checkTargetTimer(this)){
                    Tile res = null;
                    boolean build = false;

                    switch(locate){
                        case ore -> {
                            if(ore.obj() instanceof Item item){
                                res = indexer.findClosestOre(unit, item);
                            }
                        }
                        case building -> {
                            Building b = Geometry.findClosest(unit.x, unit.y, enemy.bool() ? indexer.getEnemy(unit.team, flag) : indexer.getFlagged(unit.team, flag));
                            res = b == null ? null : b.tile;
                            build = true;
                        }
                        case spawn -> {
                            res = Geometry.findClosest(unit.x, unit.y, Vars.spawner.getSpawns());
                        }
                        case damaged -> {
                            Building b = Units.findDamagedTile(unit.team, unit.x, unit.y);
                            res = b == null ? null : b.tile;
                            build = true;
                        }
                    }

                    if(res != null && (!build || res.build != null)){
                        cache.found = true;
                        //set result if found
                        outX.setnum(cache.x = World.conv(build ? res.build.x : res.worldx()));
                        outY.setnum(cache.y = World.conv(build ? res.build.y : res.worldy()));
                        outFound.setnum(1);
                    }else{
                        cache.found = false;
                        outFound.setnum(0);
                    }
                    
                    if(res != null && res.build != null && 
                        (unit.within(res.build.x, res.build.y, Math.max(unit.range(), buildingRange)) || res.build.team == exec.team)){
                        cache.build = res.build;
                        outBuild.setobj(res.build);
                    }else{
                        outBuild.setobj(null);
                    }
                }else{
                    outBuild.setobj(cache.build);
                    outFound.setbool(cache.found);
                    outX.setnum(cache.x);
                    outY.setnum(cache.y);
                }
            }else{
                outFound.setbool(false);
            }
        }

        static class Cache{
            float x, y;
            boolean found;
            Building build;
        }
    }

    /** Controls the unit based on some parameters. */
    public static class UnitControlI implements LInstruction{
        public LUnitControl type = LUnitControl.move;
        public LVar p1, p2, p3, p4, p5;

        public UnitControlI(LUnitControl type, LVar p1, LVar p2, LVar p3, LVar p4, LVar p5){
            this.type = type;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
            this.p5 = p5;
        }

        public UnitControlI(){
        }

        /** Checks is a unit is valid for logic AI control, and returns the controller. */
        @Nullable
        public static LogicAI checkLogicAI(LExecutor exec, Object unitObj){
            if(unitObj instanceof Unit unit && unit.isValid() && exec.unit.obj() == unit && (unit.team == exec.team || exec.privileged) && unit.controller().isLogicControllable()){
                if(unit.controller() instanceof LogicAI la){
                    la.controller = exec.thisv.building();
                    return la;
                }else{
                    var la = new LogicAI();
                    la.controller = exec.thisv.building();

                    unit.controller(la);
                    //clear old state
                    unit.mineTile = null;
                    unit.clearBuilding();

                    return la;
                }
            }
            return null;
        }

        @Override
        public void run(LExecutor exec){
            Object unitObj = exec.unit.obj();
            LogicAI ai = checkLogicAI(exec, unitObj);

            //only control standard AI units
            if(unitObj instanceof Unit unit && ai != null){
                ai.controlTimer = LogicAI.logicControlTimeout;
                float x1 = World.unconv(p1.numf()), y1 = World.unconv(p2.numf()), d1 = World.unconv(p3.numf());

                switch(type){
                    case idle, autoPathfind -> {
                        ai.control = type;
                    }
                    case move, stop, approach, pathfind -> {
                        ai.control = type;
                        ai.moveX = x1;
                        ai.moveY = y1;
                        if(type == LUnitControl.approach){
                            ai.moveRad = d1;
                        }

                        //stop mining/building
                        if(type == LUnitControl.stop){
                            unit.mineTile = null;
                            unit.clearBuilding();
                        }
                    }
                    case unbind -> {
                        //TODO is this a good idea? will allocate
                        unit.resetController();
                    }
                    case within -> {
                        p4.setnum(unit.within(x1, y1, d1) ? 1 : 0);
                    }
                    case target -> {
                        ai.posTarget.set(x1, y1);
                        ai.aimControl = type;
                        ai.mainTarget = null;
                        ai.shoot = p3.bool();
                    }
                    case targetp -> {
                        ai.aimControl = type;
                        ai.mainTarget = p1.obj() instanceof Teamc t ? t : null;
                        ai.shoot = p2.bool();
                    }
                    case boost -> {
                        ai.boost = p1.bool();
                    }
                    case flag -> {
                        unit.flag = p1.num();
                    }
                    case mine -> {
                        Tile tile = world.tileWorld(x1, y1);
                        if(unit.canMine()){
                            unit.mineTile = unit.validMine(tile) ? tile : null;
                        }
                    }
                    case payDrop -> {
                        if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                        if(unit instanceof Payloadc pay && pay.hasPayload()){
                            Call.payloadDropped(unit, unit.x, unit.y);
                            exec.updateTimeout(unit);
                        }
                    }
                    case payTake -> {
                        if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                        if(unit instanceof Payloadc pay){
                            //units
                            if(p1.bool()){
                                Unit result = Units.closest(unit.team, unit.x, unit.y, unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize * 1.2f));

                                if(result != null){
                                    Call.pickedUnitPayload(unit, result);
                                }
                            }else{ //buildings
                                Building build = world.buildWorld(unit.x, unit.y);

                                //TODO copy pasted code
                                if(build != null && build.team == unit.team){
                                    Payload current = build.getPayload();
                                    if(current != null && pay.canPickupPayload(current)){
                                        Call.pickedBuildPayload(unit, build, false);
                                        //pick up whole building directly
                                    }else if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                                        Call.pickedBuildPayload(unit, build, true);
                                    }
                                }
                            }
                            exec.updateTimeout(unit);
                        }
                    }
                    case payEnter -> {
                        Building build = world.buildWorld(unit.x, unit.y);
                        if(build != null && unit.team() == build.team && build.canControlSelect(unit)){
                            Call.unitBuildingControlSelect(unit, build);
                        }
                    }
                    case build -> {
                        if((state.rules.logicUnitBuild || exec.privileged) && unit.canBuild() && p3.obj() instanceof Block block && block.canBeBuilt() && (block.unlockedNow() || unit.team.isAI())){
                            int x = World.toTile(x1 - block.offset/tilesize), y = World.toTile(y1 - block.offset/tilesize);
                            int rot = Mathf.mod(p4.numi(), 4);

                            //reset state of last request when necessary
                            if(ai.plan.x != x || ai.plan.y != y || ai.plan.block != block || unit.plans.isEmpty()){
                                ai.plan.progress = 0;
                                ai.plan.initialized = false;
                                ai.plan.stuck = false;
                            }

                            var conf = p5.obj();
                            ai.plan.set(x, y, rot, block);
                            ai.plan.config = conf instanceof Content c ? c : conf instanceof Building b ? b : null;

                            unit.clearBuilding();
                            Tile tile = ai.plan.tile();

                            if(tile != null && !(tile.block() == block && tile.build != null && tile.build.rotation == rot)){
                                unit.updateBuilding = true;
                                unit.addBuild(ai.plan);
                            }
                        }
                    }
                    case getBlock -> {
                        float range = Math.max(unit.range(), unit.type.buildRange);
                        if(!unit.within(x1, y1, range)){
                            p3.setobj(null);
                            p4.setobj(null);
                            p5.setobj(null);
                        }else{
                            Tile tile = world.tileWorld(x1, y1);
                            if(tile == null){
                                p3.setobj(null);
                                p4.setobj(null);
                                p5.setobj(null);
                            }else{
                                p3.setobj(tile.block());
                                p4.setobj(tile.build != null ? tile.build : null);
                                //Allows reading of ore tiles if they are present (overlay is not air) otherwise returns the floor
                                p5.setobj(tile.overlay() == Blocks.air ? tile.floor() : tile.overlay());
                            }
                        }
                    }
                    case itemDrop -> {
                        if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                        //clear item when dropping to @air
                        if(p1.obj() == Blocks.air){
                            //only server-side; no need to call anything, as items are synced in snapshots
                            if(!net.client()){
                                unit.clearItem();
                            }
                            exec.updateTimeout(unit);
                        }else{
                            Building build = p1.building();
                            int dropped = Math.min(unit.stack.amount, p2.numi());
                            if(build != null && build.team == unit.team && build.isValid() && dropped > 0 && unit.within(build, logicItemTransferRange + build.block.size * tilesize/2f)){
                                int accepted = build.acceptStack(unit.item(), dropped, unit);
                                if(accepted > 0){
                                    Call.transferItemTo(unit, unit.item(), accepted, unit.x, unit.y, build);
                                    exec.updateTimeout(unit);
                                }
                            }
                        }
                    }
                    case itemTake -> {
                        if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                        Building build = p1.building();
                        int amount = p3.numi();

                        if(build != null && build.team == unit.team && build.isValid() && build.items != null &&
                            p2.obj() instanceof Item item && unit.within(build, logicItemTransferRange + build.block.size * tilesize/2f)){
                            int taken = Math.min(build.items.get(item), Math.min(amount, unit.maxAccepted(item)));

                            if(taken > 0){
                                Call.takeItems(build, item, taken, unit);
                                exec.updateTimeout(unit);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    /** Controls a building's state. */
    public static class ControlI implements LInstruction{
        public LVar target;
        public LAccess type = LAccess.enabled;
        public LVar p1, p2, p3, p4;

        public ControlI(LAccess type, LVar target, LVar p1, LVar p2, LVar p3, LVar p4){
            this.type = type;
            this.target = target;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        ControlI(){}

        @Override
        public void run(LExecutor exec){
            Object obj = target.obj();
            if(obj instanceof Building b && (exec.privileged || (b.team == exec.team && exec.linkIds.contains(b.id)))){

                if(type == LAccess.enabled && !p1.bool()){
                    b.lastDisabler = exec.build;
                }

                if(type == LAccess.enabled && p1.bool()){
                    b.noSleep();
                }

                if(type.isObj && p1.isobj){
                    b.control(type, p1.obj(), p2.num(), p3.num(), p4.num());
                }else{
                    b.control(type, p1.num(), p2.num(), p3.num(), p4.num());
                }
            }
        }
    }

    public static class GetLinkI implements LInstruction{
        public LVar output, index;

        public GetLinkI(LVar output, LVar index){
            this.index = index;
            this.output = output;
        }

        public GetLinkI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = index.numi();

            output.setobj(address >= 0 && address < exec.links.length ? exec.links[address] : null);
        }
    }

    public static class ReadI implements LInstruction{
        public LVar target, position, output;

        public ReadI(LVar target, LVar position, LVar output){
            this.target = target;
            this.position = position;
            this.output = output;
        }

        public ReadI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = position.numi();
            Building from = target.building();

            if(from instanceof MemoryBuild mem && (exec.privileged || from.team == exec.team)){
                output.setnum(address < 0 || address >= mem.memory.length ? 0 : mem.memory[address]);
            }
        }
    }

    public static class WriteI implements LInstruction{
        public LVar target, position, value;

        public WriteI(LVar target, LVar position, LVar value){
            this.target = target;
            this.position = position;
            this.value = value;
        }

        public WriteI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = position.numi();
            Building from = target.building();

            if(from instanceof MemoryBuild mem && (exec.privileged || (from.team == exec.team && !mem.block.privileged)) && address >= 0 && address < mem.memory.length){
                mem.memory[address] = value.num();
            }
        }
    }

    public static class SenseI implements LInstruction{
        public LVar from, to, type;

        public SenseI(LVar from, LVar to, LVar type){
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public SenseI(){
        }

        @Override
        public void run(LExecutor exec){
            Object target = from.obj();
            Object sense = type.obj();

            if(target == null && sense == LAccess.dead){
                to.setnum(1);
                return;
            }

            //note that remote units/buildings can be sensed as well
            if(target instanceof Senseable se){
                if(sense instanceof Content co){
                    to.setnum(se.sense(co));
                }else if(sense instanceof LAccess la){
                    Object objOut = se.senseObject(la);

                    if(objOut == Senseable.noSensed){
                        //numeric output
                        to.setnum(se.sense(la));
                    }else{
                        //object output
                        to.setobj(objOut);
                    }
                }
            }else{
                to.setobj(null);
            }
        }
    }

    public static class RadarI implements LInstruction{
        public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
        public RadarSort sort = RadarSort.distance;
        public LVar radar, sortOrder, output;

        //radar instructions are special in that they cache their output and only change it at fixed intervals.
        //this prevents lag from spam of radar instructions
        public Healthc lastTarget;
        public Object lastSourceBuild;
        public Interval timer = new Interval();

        static float bestValue = 0f;
        static Unit best = null;

        public RadarI(RadarTarget target1, RadarTarget target2, RadarTarget target3, RadarSort sort, LVar radar, LVar sortOrder, LVar output){
            this.target1 = target1;
            this.target2 = target2;
            this.target3 = target3;
            this.sort = sort;
            this.radar = radar;
            this.sortOrder = sortOrder;
            this.output = output;
        }

        public RadarI(){
        }

        @Override
        public void run(LExecutor exec){
            Object base = radar.obj();

            int sortDir = sortOrder.bool() ? 1 : -1;
            LogicAI ai = null;

            if(base instanceof Ranged r && (exec.privileged || r.team() == exec.team) &&
                (base instanceof Building || (ai = UnitControlI.checkLogicAI(exec, base)) != null)){ //must be a building or a controllable unit
                float range = r.range();

                Healthc targeted;

                //timers update on a fixed 30 tick interval
                //units update on a special timer per controller instance
                if((base instanceof Building && (timer.get(30f) || lastSourceBuild != base)) || (ai != null && ai.checkTargetTimer(this))){
                    //if any of the targets involve enemies
                    boolean enemies = target1 == RadarTarget.enemy || target2 == RadarTarget.enemy || target3 == RadarTarget.enemy;
                    boolean allies = target1 == RadarTarget.ally || target2 == RadarTarget.ally || target3 == RadarTarget.ally;

                    best = null;
                    bestValue = 0;

                    if(enemies){
                        Seq<TeamData> data = state.teams.present;
                        for(int i = 0; i < data.size; i++){
                            if(data.items[i].team != r.team()){
                                find(r, range, sortDir, data.items[i].team);
                            }
                        }
                    }else if(!allies){
                        Seq<TeamData> data = state.teams.present;
                        for(int i = 0; i < data.size; i++){
                            find(r, range, sortDir, data.items[i].team);
                        }
                    }else{
                        find(r, range, sortDir, r.team());
                    }

                    if(ai != null){
                        ai.execCache.put(this, best);
                    }

                    lastSourceBuild = base;
                    lastTarget = targeted = best;
                }else{
                    if(ai != null){
                        targeted = (Healthc)ai.execCache.get(this);
                    }else{
                        targeted = lastTarget;
                    }
                }

                output.setobj(targeted);
            }else{
                output.setobj(null);
            }
        }

        void find(Ranged b, float range, int sortDir, Team team){
            Units.nearby(team, b.x(), b.y(), range, u -> {
                if(!u.within(b, range) || !u.targetable(team) || b == u) return;

                boolean valid =
                    target1.func.get(b.team(), u) &&
                    target2.func.get(b.team(), u) &&
                    target3.func.get(b.team(), u);

                if(!valid) return;

                float val = sort.func.get(b, u) * sortDir;
                if(val > bestValue || best == null){
                    bestValue = val;
                    best = u;
                }
            });
        }
    }

    public static class SetI implements LInstruction{
        public LVar from, to;

        public SetI(LVar from, LVar to){
            this.from = from;
            this.to = to;
        }

        SetI(){}

        @Override
        public void run(LExecutor exec){
            if(!to.constant){
                if(from.isobj){
                    if(to != exec.counter){
                        to.objval = from.objval;
                        to.isobj = true;
                    }
                }else{
                    to.numval = LVar.invalid(from.numval) ? 0 : from.numval;
                    to.isobj = false;
                }
            }
        }
    }

    public static class OpI implements LInstruction{
        public LogicOp op = LogicOp.add;
        public LVar a, b, dest;

        public OpI(LogicOp op, LVar a, LVar b, LVar dest){
            this.op = op;
            this.a = a;
            this.b = b;
            this.dest = dest;
        }

        OpI(){}

        @Override
        public void run(LExecutor exec){
            if(op == LogicOp.strictEqual){
                dest.setnum(a.isobj == b.isobj && ((a.isobj && Structs.eq(a.objval, b.objval)) || (!a.isobj && a.numval == b.numval)) ? 1 : 0);
            }else if(op.unary){
                dest.setnum(op.function1.get(a.num()));
            }else{
                if(op.objFunction2 != null && a.isobj && b.isobj){
                    //use object function if both are objects
                    dest.setnum(op.objFunction2.get(a.obj(), b.obj()));
                }else{
                    //otherwise use the numeric function
                    dest.setnum(op.function2.get(a.num(), b.num()));
                }

            }
        }
    }

    public static class EndI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            exec.counter.numval = exec.instructions.length;
        }
    }

    public static class NoopI implements LInstruction{
        @Override
        public void run(LExecutor exec){}
    }

    public static class DrawI implements LInstruction{
        public byte type;
        public LVar x, y, p1, p2, p3, p4;

        public DrawI(byte type, LVar x, LVar y, LVar p1, LVar p2, LVar p3, LVar p4){
            this.type = type;
            this.x = x;
            this.y = y;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        public DrawI(){
        }

        @Override
        public void run(LExecutor exec){
            //graphics on headless servers are useless.
            if(Vars.headless || exec.graphicsBuffer.size >= maxGraphicsBuffer) return;

            //explicitly unpack colorPack, it's pre-processed here
            if(type == LogicDisplay.commandColorPack){
                double packed = x.num();

                int value = (int)(Double.doubleToRawLongBits(packed)),
                r = ((value & 0xff000000) >>> 24),
                g = ((value & 0x00ff0000) >>> 16),
                b = ((value & 0x0000ff00) >>> 8),
                a = ((value & 0x000000ff));

                exec.graphicsBuffer.add(DisplayCmd.get(LogicDisplay.commandColor, pack(r), pack(g), pack(b), pack(a), 0, 0));
            }else if(type == LogicDisplay.commandPrint){
                CharSequence str = exec.textBuffer;

                if(str.length() > 0){
                    var data = Fonts.logic.getData();
                    int advance = (int)data.spaceXadvance, lineHeight = (int)data.lineHeight;

                    int xOffset, yOffset;
                    int align = p1.id; //p1 is not a variable, it's a raw align value. what a massive hack

                    int maxWidth = 0, lines = 1, lineWidth = 0;
                    for(int i = 0; i < str.length(); i++){
                        char next = str.charAt(i);
                        if(next == '\n'){
                            maxWidth = Math.max(maxWidth, lineWidth);
                            lineWidth = 0;
                            lines ++;
                        }else{
                            lineWidth ++;
                        }
                    }
                    maxWidth = Math.max(maxWidth, lineWidth);

                    float
                    width = maxWidth * advance,
                    height = lines * lineHeight,
                    ha = ((Align.isLeft(align) ? -1f : 0f) + 1f + (Align.isRight(align) ? 1f : 0f))/2f,
                    va = ((Align.isBottom(align) ? -1f : 0f) + 1f + (Align.isTop(align) ? 1f : 0f))/2f;

                    xOffset = -(int)(width * ha);
                    yOffset = -(int)(height * va) + (lines - 1) * lineHeight;


                    int curX = x.numi(), curY = y.numi();
                    for(int i = 0; i < str.length(); i++){
                        char next = str.charAt(i);
                        if(next == '\n'){
                            //move Y down when newline is encountered
                            curY -= lineHeight;
                            curX = x.numi(); //reset
                            continue;
                        }
                        if(Fonts.logic.getData().hasGlyph(next)){
                            exec.graphicsBuffer.add(DisplayCmd.get(LogicDisplay.commandPrint, packSign(curX + xOffset), packSign(curY + yOffset), next, 0, 0, 0));
                        }
                        curX += advance;
                    }

                    exec.textBuffer.setLength(0);
                }
            }else{
                int num1 = p1.numi(), num4 = p4.numi(), xval = packSign(x.numi()), yval = packSign(y.numi());

                if(type == LogicDisplay.commandImage){
                    if(p1.obj() instanceof UnlockableContent u){
                        //TODO: with mods, this will overflow (ID >= 512), but that's better than the previous system, at least
                        num1 = u.id;
                        num4 = u.getContentType().ordinal();
                    }else{
                        num1 = -1;
                        num4 = -1;
                    }
                    //num1 = p1.obj() instanceof UnlockableContent u ? u.iconId : 0;
                }else if(type == LogicDisplay.commandScale){
                    xval = packSign((int)(x.numf() / LogicDisplay.scaleStep));
                    yval = packSign((int)(y.numf() / LogicDisplay.scaleStep));
                }

                //add graphics calls, cap graphics buffer size
                exec.graphicsBuffer.add(DisplayCmd.get(type, xval, yval, packSign(num1), packSign(p2.numi()), packSign(p3.numi()), packSign(num4)));
            }
        }

        static int pack(int value){
            return value & 0b0111111111;
        }

        static int packSign(int value){
            return (Math.abs(value) & 0b0111111111) | (value < 0 ? 0b1000000000 : 0);
        }
    }

    public static class DrawFlushI implements LInstruction{
        public LVar target;

        public DrawFlushI(LVar target){
            this.target = target;
        }

        public DrawFlushI(){
        }

        @Override
        public void run(LExecutor exec){
            //graphics on headless servers are useless.
            if(Vars.headless) return;

            if(target.building() instanceof LogicDisplayBuild d && (d.team == exec.team || exec.privileged)){
                if(d.commands.size + exec.graphicsBuffer.size < maxDisplayBuffer){
                    for(int i = 0; i < exec.graphicsBuffer.size; i++){
                        d.commands.addLast(exec.graphicsBuffer.items[i]);
                    }
                }
                exec.graphicsBuffer.clear();
            }
        }
    }

    public static class PrintI implements LInstruction{
        public LVar value;

        public PrintI(LVar value){
            this.value = value;
        }

        PrintI(){}

        @Override
        public void run(LExecutor exec){

            if(exec.textBuffer.length() >= maxTextBuffer) return;

            //this should avoid any garbage allocation
            if(value.isobj){
                String strValue = toString(value.objval);

                exec.textBuffer.append(strValue);
            }else{
                //display integer version when possible
                if(Math.abs(value.numval - (long)value.numval) < 0.00001){
                    exec.textBuffer.append((long)value.numval);
                }else{
                    exec.textBuffer.append(value.numval);
                }
            }
        }

        public static String toString(Object obj){
            return
                obj == null ? "null" :
                obj instanceof String s ? s :
                obj == Blocks.stoneWall ? "solid" : //special alias
                obj instanceof MappableContent content ? content.name :
                obj instanceof Content ? "[content]" :
                obj instanceof Building build ? build.block.name :
                obj instanceof Unit unit ? unit.type.name :
                obj instanceof Enum<?> e ? e.name() :
                obj instanceof Team team ? team.name :
                "[object]";
        }
    }

    public static class FormatI implements LInstruction{
        public LVar value;

        public FormatI(LVar value){
            this.value = value;
        }

        FormatI(){}

        @Override
        public void run(LExecutor exec){

            if(exec.textBuffer.length() >= maxTextBuffer) return;

            int placeholderIndex = -1;
            int placeholderNumber = 10;

            for(int i = 0; i < exec.textBuffer.length(); i++){
                if(exec.textBuffer.charAt(i) == '{' && exec.textBuffer.length() - i > 2){
                    char numChar = exec.textBuffer.charAt(i + 1);

                    if(numChar >= '0' && numChar <= '9' && exec.textBuffer.charAt(i + 2) == '}'){
                        if(numChar - '0' < placeholderNumber){
                            placeholderNumber = numChar - '0';
                            placeholderIndex = i;
                        }
                    }
                }
            }

            if(placeholderIndex == -1) return;

            //this should avoid any garbage allocation
            if(value.isobj){
                String strValue = PrintI.toString(value.objval);

                exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, strValue);
            }else{
                //display integer version when possible
                if(Math.abs(value.numval - (long)value.numval) < 0.00001){
                    exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, (long)value.numval + "");
                }else{
                    exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, value.numval + "");
                }
            }
        }
    }

    public static class PrintFlushI implements LInstruction{
        public LVar target;

        public PrintFlushI(LVar target){
            this.target = target;
        }

        public PrintFlushI(){
        }

        @Override
        public void run(LExecutor exec){

            if(target.building() instanceof MessageBuild d && (d.team == exec.team || exec.privileged)){

                d.message.setLength(0);
                d.message.append(exec.textBuffer, 0, Math.min(exec.textBuffer.length(), maxTextBuffer));

            }
            exec.textBuffer.setLength(0);

        }
    }

    public static class JumpI implements LInstruction{
        public ConditionOp op = ConditionOp.notEqual;
        public LVar value, compare;
        public int address;

        public JumpI(ConditionOp op, LVar value, LVar compare, int address){
            this.op = op;
            this.value = value;
            this.compare = compare;
            this.address = address;
        }

        public JumpI(){
        }

        @Override
        public void run(LExecutor exec){
            if(address != -1){
                LVar va = value;
                LVar vb = compare;
                boolean cmp;

                if(op == ConditionOp.strictEqual){
                    cmp = va.isobj == vb.isobj && ((va.isobj && va.objval == vb.objval) || (!va.isobj && va.numval == vb.numval));
                }else if(op.objFunction != null && va.isobj && vb.isobj){
                    //use object function if both are objects
                    cmp = op.objFunction.get(value.obj(), compare.obj());
                }else{
                    cmp = op.function.get(value.num(), compare.num());
                }

                if(cmp){
                    exec.counter.numval = address;
                }
            }
        }
    }

    public static class WaitI implements LInstruction{
        public LVar value;

        public float curTime;

        public WaitI(LVar value){
            this.value = value;
        }

        public WaitI(){
        }

        @Override
        public void run(LExecutor exec){
            if(curTime >= value.num()){
                curTime = 0f;
            }else{
                //skip back to self.
                exec.counter.numval --;
                exec.yield = true;
                curTime += Time.delta / 60f;
            }
        }
    }

    public static class StopI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            //skip back to self.
            exec.counter.numval --;
            exec.yield = true;
        }
    }

    public static class LookupI implements LInstruction{
        public LVar dest;
        public LVar from;
        public ContentType type;

        public LookupI(LVar dest, LVar from, ContentType type){
            this.dest = dest;
            this.from = from;
            this.type = type;
        }

        public LookupI(){
        }

        @Override
        public void run(LExecutor exec){
            dest.setobj(logicVars.lookupContent(type, from.numi()));
        }
    }

    public static class PackColorI implements LInstruction{
        public LVar result, r, g, b, a;

        public PackColorI(LVar result, LVar r, LVar g, LVar b, LVar a){
            this.result = result;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public PackColorI(){
        }

        @Override
        public void run(LExecutor exec){
            result.setnum(Color.toDoubleBits(Mathf.clamp(r.numf()), Mathf.clamp(g.numf()), Mathf.clamp(b.numf()), Mathf.clamp(a.numf())));
        }
    }

    public static class CutsceneI implements LInstruction{
        public CutsceneAction action = CutsceneAction.stop;
        public LVar p1, p2, p3, p4;

        public CutsceneI(CutsceneAction action, LVar p1, LVar p2, LVar p3, LVar p4){
            this.action = action;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        public CutsceneI(){
        }

        @Override
        public void run(LExecutor exec){
            if(headless) return;

            switch(action){
                case pan -> {
                    control.input.logicCutscene = true;
                    control.input.logicCamPan.set(World.unconv(p1.numf()), World.unconv(p2.numf()));
                    control.input.logicCamSpeed = p3.numf();
                }
                case zoom -> {
                    control.input.logicCutscene = true;
                    control.input.logicCutsceneZoom = Mathf.clamp(p1.numf());
                }
                case stop -> {
                    control.input.logicCutscene = false;
                }
            }
        }
    }

    public static class FetchI implements LInstruction{
        public FetchType type = FetchType.unit;
        public LVar result, team, extra, index;

        public FetchI(FetchType type, LVar result, LVar team, LVar extra, LVar index){
            this.type = type;
            this.result = result;
            this.team = team;
            this.extra = extra;
            this.index = index;
        }

        public FetchI(){
        }

        @Override
        public void run(LExecutor exec){
            int i = index.numi();
            Team t = team.team();
            if(t == null) return;
            TeamData data = t.data();

            switch(type){
                case unit -> {
                    UnitType type = extra.obj() instanceof UnitType u ? u : null;
                    if(type == null){
                        result.setobj(i < 0 || i >= data.units.size ? null : data.units.get(i));
                    }else{
                        var units = data.unitCache(type);
                        result.setobj(units == null || i < 0 || i >= units.size ? null : units.get(i));
                    }
                }
                case player -> result.setobj(i < 0 || i >= data.players.size || data.players.get(i).unit().isNull() ? null : data.players.get(i).unit());
                case core -> result.setobj(i < 0 || i >= data.cores.size ? null : data.cores.get(i));
                case build -> {
                    Block block = extra.obj() instanceof Block b ? b : null;
                    if(block == null){
                        result.setobj(i < 0 || i >= data.buildings.size ? null : data.buildings.get(i));
                    }else{
                        var builds = data.getBuildings(block);
                        result.setobj(i < 0 || i >= builds.size ? null : builds.get(i));
                    }
                }
                case unitCount -> {
                    UnitType type = extra.obj() instanceof UnitType u ? u : null;
                    if(type == null){
                        result.setnum(data.units.size);
                    }else{
                        result.setnum(data.unitCache(type) == null ? 0 : data.unitCache(type).size);
                    }
                }
                case coreCount -> result.setnum(data.cores.size);
                case playerCount -> result.setnum(data.players.size);
                case buildCount -> {
                    Block block = extra.obj() instanceof Block b ? b : null;
                    if(block == null){
                        result.setnum(data.buildings.size);
                    }else{
                        result.setnum(data.getBuildings(block).size);
                    }
                }
            }
        }
    }

    //endregion
    //region privileged / world instructions

    public static class GetBlockI implements LInstruction{
        public LVar x, y;
        public LVar dest;
        public TileLayer layer = TileLayer.block;

        public GetBlockI(LVar x, LVar y, LVar dest, TileLayer layer){
            this.x = x;
            this.y = y;
            this.dest = dest;
            this.layer = layer;
        }

        public GetBlockI(){
        }

        @Override
        public void run(LExecutor exec){
            Tile tile = world.tile(x.numi(), y.numi());
            if(tile == null){
                dest.setobj(null);
            }else{
                dest.setobj(switch(layer){
                    case floor -> tile.floor();
                    case ore -> tile.overlay();
                    case block -> tile.block();
                    case building -> tile.build;
                });
            }
        }
    }

    public static class SetBlockI implements LInstruction{
        public LVar x, y;
        public LVar block;
        public LVar team, rotation;
        public TileLayer layer = TileLayer.block;

        public SetBlockI(LVar x, LVar y, LVar block, LVar team, LVar rotation, TileLayer layer){
            this.x = x;
            this.y = y;
            this.block = block;
            this.team = team;
            this.rotation = rotation;
            this.layer = layer;
        }

        public SetBlockI(){
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            Tile tile = world.tile(x.numi(), y.numi());
            if(tile != null && block.obj() instanceof Block b){
                //TODO this can be quite laggy...
                switch(layer){
                    case ore -> {
                        if((b instanceof OverlayFloor || b == Blocks.air) && tile.overlay() != b) tile.setOverlayNet(b);
                    }
                    case floor -> {
                        if(b instanceof Floor f && tile.floor() != f && !f.isOverlay() && !f.isAir()){
                            tile.setFloorNet(f);
                        }
                    }
                    case block -> {
                        if(!b.isFloor() || b == Blocks.air){
                            Team t = team.team();
                            if(t == null) t = Team.derelict;

                            if(tile.block() != b || tile.team() != t){
                                tile.setBlock(b, t, Mathf.clamp(rotation.numi(), 0, 3));
                            }
                        }
                    }
                    //building case not allowed
                }
            }
        }
    }

    public static class SpawnUnitI implements LInstruction{
        public LVar type, x, y, rotation, team, result;

        public SpawnUnitI(LVar type, LVar x, LVar y, LVar rotation, LVar team, LVar result){
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.team = team;
            this.result = result;
        }

        public SpawnUnitI(){
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            Team t = team.team();

            if(type.obj() instanceof UnitType type && !type.hidden && t != null && Units.canCreate(t, type)){
                //random offset to prevent stacking
                var unit = type.spawn(t, World.unconv(x.numf()) + Mathf.range(0.01f), World.unconv(y.numf()) + Mathf.range(0.01f));
                spawner.spawnEffect(unit, rotation.numf());
                result.setobj(unit);
            }
        }
    }

    public static class SenseWeatherI implements LInstruction{
        public LVar type, to;

        public SenseWeatherI(LVar type, LVar to){
            this.type = type;
            this.to = to;
        }

        @Override
        public void run(LExecutor exec){
            to.setbool(type.obj() instanceof Weather weather && weather.isActive());
        }
    }

    public static class SetWeatherI implements LInstruction{
        public LVar type, state;

        public SetWeatherI(LVar type, LVar state){
            this.type = type;
            this.state = state;
        }

        @Override
        public void run(LExecutor exec){
            if(type.obj() instanceof Weather weather){
                if(state.bool()){
                    if(!weather.isActive()){ //Create is not already active
                        Tmp.v1.setToRandomDirection();
                        Call.createWeather(weather, 1f, WeatherState.fadeTime, Tmp.v1.x, Tmp.v1.y);
                    }else{
                        weather.instance().life(WeatherState.fadeTime);
                    }
                }else{
                    if(weather.isActive() && weather.instance().life > WeatherState.fadeTime){
                        weather.instance().life(WeatherState.fadeTime);
                    }
                }
            }
        }
    }

    public static class ApplyEffectI implements LInstruction{
        public boolean clear;
        public String effect;
        public LVar unit, duration;

        public ApplyEffectI(boolean clear, String effect, LVar unit, LVar duration){
            this.clear = clear;
            this.effect = effect;
            this.unit = unit;
            this.duration = duration;
        }

        public ApplyEffectI(){
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            if(unit.obj() instanceof Unit unit && content.statusEffect(effect) != null){
                if(clear){
                    unit.unapply(content.statusEffect(effect));
                }else{
                    unit.apply(content.statusEffect(effect), duration.numf() * 60f);
                }
            }
        }
    }

    public static class SetRuleI implements LInstruction{
        public LogicRule rule = LogicRule.waveSpacing;
        public LVar value, p1, p2, p3, p4;

        public SetRuleI(LogicRule rule, LVar value, LVar p1, LVar p2, LVar p3, LVar p4){
            this.rule = rule;
            this.value = value;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        public SetRuleI(){
        }

        @Override
        public void run(LExecutor exec){
            switch(rule){
                case waveTimer -> state.rules.waveTimer = value.bool();
                case wave -> state.wave = Math.max(value.numi(), 1);
                case currentWaveTime -> state.wavetime = Math.max(value.numf() * 60f, 0f);
                case waves -> state.rules.waves = value.bool();
                case waveSending -> state.rules.waveSending = value.bool();
                case attackMode -> state.rules.attackMode = value.bool();
                case waveSpacing -> state.rules.waveSpacing = value.numf() * 60f;
                case enemyCoreBuildRadius -> state.rules.enemyCoreBuildRadius = value.numf() * 8f;
                case dropZoneRadius -> state.rules.dropZoneRadius = value.numf() * 8f;
                case unitCap -> state.rules.unitCap = Math.max(value.numi(), 0);
                case lighting -> state.rules.lighting = value.bool();
                case mapArea -> {
                    int x = p1.numi(), y = p2.numi(), w = p3.numi(), h = p4.numi();
                    if(!checkMapArea(x, y, w, h, false)){
                        Call.setMapArea(x, y, w, h);
                    }
                }
                case ambientLight -> state.rules.ambientLight.fromDouble(value.num());
                case solarMultiplier -> state.rules.solarMultiplier = Math.max(value.numf(), 0f);
                case ban -> {
                    Object cont = value.obj();
                    if(cont instanceof Block b){
                        // Rebuild PlacementFragment if anything has changed
                        if(state.rules.bannedBlocks.add(b) && !headless) ui.hudfrag.blockfrag.rebuild();
                    }else if(cont instanceof UnitType u){
                        state.rules.bannedUnits.add(u);
                    }
                }
                case unban -> {
                    Object cont = value.obj();
                    if(cont instanceof Block b){
                        if(state.rules.bannedBlocks.remove(b) && !headless) ui.hudfrag.blockfrag.rebuild();
                    }else if(cont instanceof UnitType u){
                        state.rules.bannedUnits.remove(u);
                    }
                }
                case unitHealth, unitBuildSpeed, unitCost, unitDamage, blockHealth, blockDamage, buildSpeed, rtsMinSquad, rtsMinWeight -> {
                    Team team = p1.team();
                    if(team != null){
                        float num = value.numf();
                        switch(rule){
                            case buildSpeed -> team.rules().buildSpeedMultiplier = Mathf.clamp(num, 0.001f, 50f);
                            case unitHealth -> team.rules().unitHealthMultiplier = Math.max(num, 0.001f);
                            case unitBuildSpeed -> team.rules().unitBuildSpeedMultiplier = Mathf.clamp(num, 0f, 50f);
                            case unitCost -> team.rules().unitCostMultiplier = Math.max(num, 0f);
                            case unitDamage -> team.rules().unitDamageMultiplier = Math.max(num, 0f);
                            case blockHealth -> team.rules().blockHealthMultiplier = Math.max(num, 0.001f);
                            case blockDamage -> team.rules().blockDamageMultiplier = Math.max(num, 0f);
                            case rtsMinWeight -> team.rules().rtsMinWeight = num;
                            case rtsMinSquad -> team.rules().rtsMinSquad = (int)num;
                        }
                    }
                }
            }
        }
    }

    /** @return whether the map area is already set to this value. */
    static boolean checkMapArea(int x, int y, int w, int h, boolean set){
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        w = Math.min(world.width(), w);
        h = Math.min(world.height(), h);
        boolean full = x == 0 && y == 0 && w == world.width() && h == world.height();

        if(state.rules.limitMapArea){
            if(state.rules.limitX == x && state.rules.limitY == y && state.rules.limitWidth == w && state.rules.limitHeight == h){
                return true;
            }else if(full){
                //disable the rule, covers the whole map
                if(set){
                    state.rules.limitMapArea = false;
                    if(!headless){
                        renderer.updateAllDarkness();
                    }
                    world.checkMapArea();
                    return false;
                }
            }
        }else if(full){ //was already disabled, no need to change anything
            return true;
        }

        if(set){
            state.rules.limitMapArea = true;
            state.rules.limitX = x;
            state.rules.limitY = y;
            state.rules.limitWidth = w;
            state.rules.limitHeight = h;
            world.checkMapArea();

            if(!headless){
                renderer.updateAllDarkness();
            }
        }

        return false;
    }

    @Remote(called = Loc.server)
    public static void setMapArea(int x, int y, int w, int h){
        checkMapArea(x, y, w, h, true);
    }

    public static class FlushMessageI implements LInstruction{
        public MessageType type = MessageType.announce;
        public LVar duration, outSuccess;

        public FlushMessageI(MessageType type, LVar duration, LVar outSuccess){
            this.type = type;
            this.duration = duration;
            this.outSuccess = outSuccess;
        }

        public FlushMessageI(){
        }

        @Override
        public void run(LExecutor exec){
            //set default to success
            outSuccess.setnum(1);
            if(headless && type != MessageType.mission) {
                exec.textBuffer.setLength(0);
                return;
            }

            if(
                type == MessageType.announce && ui.hasAnnouncement() ||
                type == MessageType.notify && ui.hudfrag.hasToast() ||
                type == MessageType.toast && ui.hasAnnouncement()
            ){
                //set outSuccess=false to let user retry.
                outSuccess.setnum(0);
                return;
            }

            String text = exec.textBuffer.toString();
            if(text.startsWith("@")){
                String substr = text.substring(1);
                if(Core.bundle.has(substr)){
                    text = Core.bundle.get(substr);
                }
            }

            switch(type){
                case notify -> ui.hudfrag.showToast(Icon.info, text);
                case announce -> ui.announce(text, duration.numf());
                case toast -> ui.showInfoToast(text, duration.numf());
                //TODO desync?
                case mission -> state.rules.mission = text;
            }

            exec.textBuffer.setLength(0);
        }
    }

    public static class EffectI implements LInstruction{
        public EffectEntry type;
        public LVar x, y, rotation, color, data;

        public EffectI(EffectEntry type, LVar x, LVar y, LVar rotation, LVar color, LVar data){
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.color = color;
            this.data = data;
        }

        public EffectI(){
        }

        @Override
        public void run(LExecutor exec){
            if(type != null){
                double col = color.num();
                //limit size so people don't create lag with ridiculous numbers (some explosions scale with size)
                float rot = type.rotate ? rotation.numf() :
                    Math.min(rotation.numf(), 1000f);

                type.effect.at(World.unconv(x.numf()), World.unconv(y.numf()), rot, Tmp.c1.fromDouble(col), data.obj());
            }
        }
    }

    public static class ExplosionI implements LInstruction{
        public LVar team, x, y, radius, damage, air, ground, pierce, effect;

        public ExplosionI(LVar team, LVar x, LVar y, LVar radius, LVar damage, LVar air, LVar ground, LVar pierce, LVar effect){
            this.team = team;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damage = damage;
            this.air = air;
            this.ground = ground;
            this.pierce = pierce;
            this.effect = effect;
        }

        public ExplosionI(){
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            Team t = team.team();
            //note that there is a radius cap
            Call.logicExplosion(t, World.unconv(x.numf()), World.unconv(y.numf()), World.unconv(Math.min(radius.numf(), 100)), damage.numf(), air.bool(), ground.bool(), pierce.bool(), effect.bool());
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void logicExplosion(Team team, float x, float y, float radius, float damage, boolean air, boolean ground, boolean pierce, boolean effect){
        if(damage < 0f) return;

        Damage.damage(team, x, y, radius, damage, pierce, air, ground);
        if(effect){
            if(pierce){
                Fx.spawnShockwave.at(x, y, World.conv(radius));
            }else{
                Fx.dynamicExplosion.at(x, y, World.conv(radius) / 8f);
            }
        }
    }

    public static class SetRateI implements LInstruction{
        public LVar amount;

        public SetRateI(LVar amount){
            this.amount = amount;
        }

        public SetRateI(){
        }

        @Override
        public void run(LExecutor exec){
            exec.build.ipt = Mathf.clamp(amount.numi(), 1, ((LogicBlock)exec.build.block).maxInstructionsPerTick);
            if(exec.ipt != null){
                exec.ipt.numval = exec.build.ipt;
            }
        }
    }

    @Remote(unreliable = true)
    public static void syncVariable(Building building, int variable, Object value){
        if(building instanceof LogicBuild build){
            LVar v = build.executor.optionalVar(variable);
            if(v != null && !v.constant){
                if(value instanceof Number n){
                    v.isobj = false;
                    v.numval = n.doubleValue();
                }else{
                    v.isobj = true;
                    v.objval = value;
                }
            }
        }
    }

    public static class SyncI implements LInstruction{
        //20 syncs per second
        public static long syncInterval = 1000 / 20;

        public LVar variable;

        public SyncI(LVar variable){
            this.variable = variable;
        }

        public SyncI(){
        }

        @Override
        public void run(LExecutor exec){
            if(!variable.constant && Time.timeSinceMillis(variable.syncTime) > syncInterval){
                variable.syncTime = Time.millis();
                Call.syncVariable(exec.build, variable.id, variable.isobj ? variable.objval : variable.numval);
            }
        }
    }

    public static class ClientDataI implements LInstruction{
        public LVar channel, value, reliable;

        public ClientDataI(LVar channel, LVar value, LVar reliable){
            this.channel = channel;
            this.value = value;
            this.reliable = reliable;
        }

        public ClientDataI() {
        }

        @Override
        public void run(LExecutor exec) {
            if(channel.obj() instanceof String c){
                Object v = value.isobj ? value.objval : value.numval;
                if(reliable.bool()){
                    Call.clientLogicDataReliable(c, v);
                }else{
                    Call.clientLogicDataUnreliable(c, v);
                }
            }
        }
    }

    public static class GetFlagI implements LInstruction{
        public LVar result, flag;

        public GetFlagI(LVar result, LVar flag){
            this.result = result;
            this.flag = flag;
        }

        public GetFlagI(){
        }

        @Override
        public void run(LExecutor exec){
            if(flag.obj() instanceof String str){
                result.setbool(state.rules.objectiveFlags.contains(str));
            }else{
                result.setobj(null);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void setFlag(String flag, boolean add){
        if(add){
            state.rules.objectiveFlags.add(flag);
        }else{
            state.rules.objectiveFlags.remove(flag);
        }
    }

    public static class SetFlagI implements LInstruction{
        public LVar flag, value;

        public SetFlagI(LVar flag, LVar value){
            this.flag = flag;
            this.value = value;
        }

        public SetFlagI(){
        }

        @Override
        public void run(LExecutor exec){
            //don't invoke unless the flag state actually changes
            if(flag.obj() instanceof String str && state.rules.objectiveFlags.contains(str) != value.bool()){
                Call.setFlag(str, value.bool());
            }
        }
    }

    public static class SpawnWaveI implements LInstruction{
        public LVar natural;
        public LVar x, y;

        public SpawnWaveI(){
        }

        public SpawnWaveI(LVar natural, LVar x, LVar y){
            this.natural = natural;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            if(natural.bool()){
                logic.skipWave();
                return;
            }

            float
                spawnX = World.unconv(x.numf()),
                spawnY = World.unconv(y.numf());
            int packed = Point2.pack(x.numi(), y.numi());

            for(SpawnGroup group : state.rules.spawns){
                if(group.type == null || (group.spawn != -1 && group.spawn != packed)) continue;

                int spawned = group.getSpawned(state.wave - 1);
                float spread = tilesize * 2;

                for(int i = 0; i < spawned; i++){
                    Tmp.v1.rnd(spread);

                    Unit unit = group.createUnit(state.rules.waveTeam, state.wave - 1);
                    unit.set(spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);
                    Vars.spawner.spawnEffect(unit);
                }
            }
        }
    }

    public static class SetPropI implements LInstruction{
        public LVar type, of, value;

        public SetPropI(LVar type, LVar of, LVar value){
            this.type = type;
            this.of = of;
            this.value = value;
        }

        public SetPropI(){
        }

        @Override
        public void run(LExecutor exec){
            if(of.obj() instanceof Settable sp){
                Object key = type.obj();
                if(key instanceof LAccess property){
                    if(value.isobj){
                        sp.setProp(property, value.objval);
                    }else{
                        sp.setProp(property, value.numval);
                    }
                }else if(key instanceof UnlockableContent content){
                    sp.setProp(content, value.num());
                }
            }
        }
    }

    public static class SetMarkerI implements LInstruction{
        public LMarkerControl type = LMarkerControl.pos;
        public LVar id, p1, p2, p3;

        public SetMarkerI(LMarkerControl type, LVar id, LVar p1, LVar p2, LVar p3){
            this.type = type;
            this.id = id;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        public SetMarkerI(){
        }

        @Override
        public void run(LExecutor exec){
            if(type == LMarkerControl.remove){
                state.markers.remove(id.numi());
            }else{
                var marker = state.markers.get(id.numi());
                if(marker == null) return;

                if(type == LMarkerControl.flushText){
                    marker.setText(exec.textBuffer.toString(), p1.bool());
                    exec.textBuffer.setLength(0);
                }else if(type == LMarkerControl.texture){
                    if(p1.bool()){
                        marker.setTexture(exec.textBuffer.toString());
                        exec.textBuffer.setLength(0);
                    }else{
                        marker.setTexture(PrintI.toString(p2.obj()));
                    }
                }else{
                    marker.control(type, p1.numOrNan(), p2.numOrNan(), p3.numOrNan());
                }
            }
        }
    }

    public static class MakeMarkerI implements LInstruction{
        //TODO arbitrary number
        public static final int maxMarkers = 20000;

        public String type = "shape";
        public LVar id, x, y, replace;

        public MakeMarkerI(String type, LVar id, LVar x, LVar y, LVar replace){
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
            this.replace = replace;
        }

        public MakeMarkerI(){
        }

        @Override
        public void run(LExecutor exec){
            var cons = MapObjectives.markerNameToType.get(type);

            if(cons != null && state.markers.size() < maxMarkers){
                int mid = id.numi();
                if(replace.bool() || !state.markers.has(mid)){
                    var marker = cons.get();
                    marker.control(LMarkerControl.pos, x.num(), y.num(), 0);
                    state.markers.add(mid, marker);
                }
            }
        }
    }

    @Remote(called = Loc.server, variants = Variant.both, unreliable = true)
    public static void createMarker(int id, ObjectiveMarker marker){
        state.markers.add(id, marker);
    }

    @Remote(called = Loc.server, variants = Variant.both, unreliable = true)
    public static void removeMarker(int id){
        state.markers.remove(id);
    }

    @Remote(called = Loc.server, variants = Variant.both, unreliable = true)
    public static void updateMarker(int id, LMarkerControl control, double p1, double p2, double p3){
        var marker = state.markers.get(id);
        if(marker != null){
            marker.control(control, p1, p2, p3);
        }
    }

    @Remote(called = Loc.server, variants = Variant.both, unreliable = true)
    public static void updateMarkerText(int id, LMarkerControl type, boolean fetch, String text){
        var marker = state.markers.get(id);
        if(marker != null){
            if(type == LMarkerControl.flushText){
                marker.setText(text, fetch);
            }
        }
    }

    @Remote(called = Loc.server, variants = Variant.both, unreliable = true)
    public static void updateMarkerTexture(int id, String textureName){
        var marker = state.markers.get(id);
        if(marker != null){
            marker.setTexture(textureName);
        }
    }

    public static class LocalePrintI implements LInstruction{
        public LVar name;

        public LocalePrintI(LVar name){
            this.name = name;
        }

        public LocalePrintI(){
        }

        @Override
        public void run(LExecutor exec){
            if(exec.textBuffer.length() >= maxTextBuffer) return;

            //this should avoid any garbage allocation
            if(name.isobj){
                String name = PrintI.toString(this.name.objval);

                String strValue;

                if(mobile){
                    strValue = state.mapLocales.containsProperty(name + ".mobile") ?
                    state.mapLocales.getProperty(name + ".mobile") :
                    state.mapLocales.getProperty(name);
                }else{
                    strValue = state.mapLocales.getProperty(name);
                }

                exec.textBuffer.append(strValue);
            }
        }
    }

    //endregion
}
