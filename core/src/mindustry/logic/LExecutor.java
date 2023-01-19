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
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
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

    //special variables
    public static final int
    varCounter = 0,
    varUnit = 1,
    varThis = 2;

    public static final int
    maxGraphicsBuffer = 256,
    maxDisplayBuffer = 1024,
    maxTextBuffer = 400;

    public LInstruction[] instructions = {};
    public Var[] vars = {};
    public Var counter;
    public int[] binds;

    public int iptIndex = -1;
    public LongSeq graphicsBuffer = new LongSeq();
    public StringBuilder textBuffer = new StringBuilder();
    public Building[] links = {};
    public @Nullable LogicBuild build;
    public IntSet linkIds = new IntSet();
    public Team team = Team.derelict;
    public boolean privileged = false;

    //yes, this is a minor memory leak, but it's probably not significant enough to matter
    protected IntFloatMap unitTimeouts = new IntFloatMap();

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
        vars = new Var[builder.vars.size];
        instructions = builder.instructions;
        iptIndex = -1;

        builder.vars.each((name, var) -> {
            Var dest = new Var(name);
            vars[var.id] = dest;
            if(dest.name.equals("@ipt")){
                iptIndex = var.id;
            }

            dest.constant = var.constant;

            if(var.value instanceof Number number){
                dest.isobj = false;
                dest.numval = number.doubleValue();
            }else{
                dest.isobj = true;
                dest.objval = var.value;
            }
        });

        counter = vars[varCounter];
    }

    //region utility

    private static boolean invalid(double d){
        return Double.isNaN(d) || Double.isInfinite(d);
    }

    public Var var(int index){
        //global constants have variable IDs < 0, and they are fetched from the global constants object after being negated
        return index < 0 ? logicVars.get(-index) : vars[index];
    }

    public @Nullable Building building(int index){
        Object o = var(index).objval;
        return var(index).isobj && o instanceof Building building ? building : null;
    }

    public @Nullable Object obj(int index){
        Object o = var(index).objval;
        return var(index).isobj ? o : null;
    }

    public @Nullable Team team(int index){
        Var v = var(index);
        if(v.isobj){
            return v.objval instanceof Team t ? t : null;
        }else{
            int t = (int)v.numval;
            if(t < 0 || t >= Team.all.length) return null;
            return Team.all[t];
        }
    }

    public boolean bool(int index){
        Var v = var(index);
        return v.isobj ? v.objval != null : Math.abs(v.numval) >= 0.00001;
    }

    public double num(int index){
        Var v = var(index);
        return v.isobj ? v.objval != null ? 1 : 0 : invalid(v.numval) ? 0 : v.numval;
    }

    public float numf(int index){
        Var v = var(index);
        return v.isobj ? v.objval != null ? 1 : 0 : invalid(v.numval) ? 0 : (float)v.numval;
    }

    public int numi(int index){
        return (int)num(index);
    }

    public void setbool(int index, boolean value){
        setnum(index, value ? 1 : 0);
    }

    public void setnum(int index, double value){
        Var v = var(index);
        if(v.constant) return;
        if(invalid(value)){
            v.objval = null;
            v.isobj = true;
        }else{
            v.numval = value;
            v.objval = null;
            v.isobj = false;
        }
    }

    public void setobj(int index, Object value){
        Var v = var(index);
        if(v.constant) return;
        v.objval = value;
        v.isobj = true;
    }

    public void setconst(int index, Object value){
        Var v = var(index);
        v.objval = value;
        v.isobj = true;
    }

    //endregion

    /** A logic variable. */
    public static class Var{
        public final String name;

        public boolean isobj, constant;

        public Object objval;
        public double numval;

        public Var(String name){
            this.name = name;
        }
    }

    //region instruction types

    public interface LInstruction{
        void run(LExecutor exec);
    }

    /** Binds the processor to a unit based on some filters. */
    public static class UnitBindI implements LInstruction{
        public int type;

        public UnitBindI(int type){
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
            if(exec.obj(type) instanceof UnitType type && type.logicControllable){
                Seq<Unit> seq = exec.team.data().unitCache(type);

                if(seq != null && seq.any()){
                    exec.binds[type.id] %= seq.size;
                    if(exec.binds[type.id] < seq.size){
                        //bind to the next unit
                        exec.setconst(varUnit, seq.get(exec.binds[type.id]));
                    }
                    exec.binds[type.id] ++;
                }else{
                    //no units of this type found
                    exec.setconst(varUnit, null);
                }
            }else if(exec.obj(type) instanceof Unit u && (u.team == exec.team || exec.privileged) && u.type.logicControllable){
                //bind to specific unit object
                exec.setconst(varUnit, u);
            }else{
                exec.setconst(varUnit, null);
            }
        }
    }

    /** Uses a unit to find something that may not be in its range. */
    public static class UnitLocateI implements LInstruction{
        public LLocate locate = LLocate.building;
        public BlockFlag flag = BlockFlag.core;
        public int enemy, ore;
        public int outX, outY, outFound, outBuild;

        public UnitLocateI(LLocate locate, BlockFlag flag, int enemy, int ore, int outX, int outY, int outFound, int outBuild){
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
            Object unitObj = exec.obj(varUnit);
            LogicAI ai = UnitControlI.checkLogicAI(exec, unitObj);

            if(unitObj instanceof Unit unit && ai != null){
                ai.controlTimer = LogicAI.logicControlTimeout;

                Cache cache = (Cache)ai.execCache.get(this, Cache::new);

                if(ai.checkTargetTimer(this)){
                    Tile res = null;
                    boolean build = false;

                    switch(locate){
                        case ore -> {
                            if(exec.obj(ore) instanceof Item item){
                                res = indexer.findClosestOre(unit, item);
                            }
                        }
                        case building -> {
                            Building b = Geometry.findClosest(unit.x, unit.y, exec.bool(enemy) ? indexer.getEnemy(unit.team, flag) : indexer.getFlagged(unit.team, flag));
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
                        exec.setnum(outX, cache.x = World.conv(build ? res.build.x : res.worldx()));
                        exec.setnum(outY, cache.y = World.conv(build ? res.build.y : res.worldy()));
                        exec.setnum(outFound, 1);
                    }else{
                        cache.found = false;
                        exec.setnum(outFound, 0);
                    }
                    exec.setobj(outBuild, res != null && res.build != null && res.build.team == exec.team ? cache.build = res.build : null);
                }else{
                    exec.setobj(outBuild, cache.build);
                    exec.setbool(outFound, cache.found);
                    exec.setnum(outX, cache.x);
                    exec.setnum(outY, cache.y);
                }
            }else{
                exec.setbool(outFound, false);
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
        public int p1, p2, p3, p4, p5;

        public UnitControlI(LUnitControl type, int p1, int p2, int p3, int p4, int p5){
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
            if(unitObj instanceof Unit unit && unit.isValid() && exec.obj(varUnit) == unit && (unit.team == exec.team || exec.privileged) && !unit.isPlayer() && !(unit.isCommandable() && unit.command().hasCommand())){
                if(unit.controller() instanceof LogicAI la){
                    la.controller = exec.building(varThis);
                    return la;
                }else{
                    var la = new LogicAI();
                    la.controller = exec.building(varThis);

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
            Object unitObj = exec.obj(varUnit);
            LogicAI ai = checkLogicAI(exec, unitObj);

            //only control standard AI units
            if(unitObj instanceof Unit unit && ai != null){
                ai.controlTimer = LogicAI.logicControlTimeout;
                float x1 = World.unconv(exec.numf(p1)), y1 = World.unconv(exec.numf(p2)), d1 = World.unconv(exec.numf(p3));

                switch(type){
                    case idle -> {
                        ai.control = type;
                    }
                    case move, stop, approach -> {
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
                        exec.setnum(p4, unit.within(x1, y1, d1) ? 1 : 0);
                    }
                    case target -> {
                        ai.posTarget.set(x1, y1);
                        ai.aimControl = type;
                        ai.mainTarget = null;
                        ai.shoot = exec.bool(p3);
                    }
                    case targetp -> {
                        ai.aimControl = type;
                        ai.mainTarget = exec.obj(p1) instanceof Teamc t ? t : null;
                        ai.shoot = exec.bool(p2);
                    }
                    case boost -> {
                        ai.boost = exec.bool(p1);
                    }
                    case flag -> {
                        unit.flag = exec.num(p1);
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
                            if(exec.bool(p1)){
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
                        if((state.rules.logicUnitBuild || exec.privileged) && unit.canBuild() && exec.obj(p3) instanceof Block block && block.canBeBuilt() && (block.unlockedNow() || unit.team.isAI())){
                            int x = World.toTile(x1 - block.offset/tilesize), y = World.toTile(y1 - block.offset/tilesize);
                            int rot = Mathf.mod(exec.numi(p4), 4);

                            //reset state of last request when necessary
                            if(ai.plan.x != x || ai.plan.y != y || ai.plan.block != block || unit.plans.isEmpty()){
                                ai.plan.progress = 0;
                                ai.plan.initialized = false;
                                ai.plan.stuck = false;
                            }

                            var conf = exec.obj(p5);
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
                            exec.setobj(p3, null);
                            exec.setobj(p4, null);
                            exec.setobj(p5, null);
                        }else{
                            Tile tile = world.tileWorld(x1, y1);
                            if(tile == null){
                                exec.setobj(p3, null);
                                exec.setobj(p4, null);
                                exec.setobj(p5, null);
                            }else{
                                //any environmental solid block is returned as StoneWall, aka "@solid"
                                Block block = !tile.synthetic() ? (tile.solid() ? Blocks.stoneWall : Blocks.air) : tile.block();
                                exec.setobj(p3, block);
                                exec.setobj(p4, tile.build != null ? tile.build : null);
                                //Allows reading of ore tiles if they are present (overlay is not air) otherwise returns the floor
                                exec.setobj(p5, tile.overlay() == Blocks.air ? tile.floor() : tile.overlay());
                            }
                        }
                    }
                    case itemDrop -> {
                        if(!exec.timeoutDone(unit, LogicAI.transferDelay)) return;

                        //clear item when dropping to @air
                        if(exec.obj(p1) == Blocks.air){
                            //only server-side; no need to call anything, as items are synced in snapshots
                            if(!net.client()){
                                unit.clearItem();
                            }
                            exec.updateTimeout(unit);
                        }else{
                            Building build = exec.building(p1);
                            int dropped = Math.min(unit.stack.amount, exec.numi(p2));
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

                        Building build = exec.building(p1);
                        int amount = exec.numi(p3);

                        if(build != null && build.team == unit.team && build.isValid() && build.items != null &&
                            exec.obj(p2) instanceof Item item && unit.within(build, logicItemTransferRange + build.block.size * tilesize/2f)){
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
        public int target;
        public LAccess type = LAccess.enabled;
        public int p1, p2, p3, p4;

        public ControlI(LAccess type, int target, int p1, int p2, int p3, int p4){
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
            Object obj = exec.obj(target);
            if(obj instanceof Building b && (exec.privileged || (b.team == exec.team && exec.linkIds.contains(b.id)))){

                if(type == LAccess.enabled && !exec.bool(p1)){
                    b.lastDisabler = exec.build;
                }

                if(type == LAccess.enabled && exec.bool(p1)){
                    b.noSleep();
                }

                if(type.isObj && exec.var(p1).isobj){
                    b.control(type, exec.obj(p1), exec.num(p2), exec.num(p3), exec.num(p4));
                }else{
                    b.control(type, exec.num(p1), exec.num(p2), exec.num(p3), exec.num(p4));
                }
            }
        }
    }

    public static class GetLinkI implements LInstruction{
        public int output, index;

        public GetLinkI(int output, int index){
            this.index = index;
            this.output = output;
        }

        public GetLinkI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(index);

            exec.setobj(output, address >= 0 && address < exec.links.length ? exec.links[address] : null);
        }
    }

    public static class ReadI implements LInstruction{
        public int target, position, output;

        public ReadI(int target, int position, int output){
            this.target = target;
            this.position = position;
            this.output = output;
        }

        public ReadI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(position);
            Building from = exec.building(target);

            if(from instanceof MemoryBuild mem && (exec.privileged || from.team == exec.team)){

                exec.setnum(output, address < 0 || address >= mem.memory.length ? 0 : mem.memory[address]);
            }
        }
    }

    public static class WriteI implements LInstruction{
        public int target, position, value;

        public WriteI(int target, int position, int value){
            this.target = target;
            this.position = position;
            this.value = value;
        }

        public WriteI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(position);
            Building from = exec.building(target);

            if(from instanceof MemoryBuild mem && (exec.privileged || from.team == exec.team) && address >= 0 && address < mem.memory.length){
                mem.memory[address] = exec.num(value);
            }
        }
    }

    public static class SenseI implements LInstruction{
        public int from, to, type;

        public SenseI(int from, int to, int type){
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public SenseI(){
        }

        @Override
        public void run(LExecutor exec){
            Object target = exec.obj(from);
            Object sense = exec.obj(type);

            if(target == null && sense == LAccess.dead){
                exec.setnum(to, 1);
                return;
            }

            //note that remote units/buildings can be sensed as well
            if(target instanceof Senseable se){
                if(sense instanceof Content co){
                    exec.setnum(to, se.sense(co));
                }else if(sense instanceof LAccess la){
                    Object objOut = se.senseObject(la);

                    if(objOut == Senseable.noSensed){
                        //numeric output
                        exec.setnum(to, se.sense(la));
                    }else{
                        //object output
                        exec.setobj(to, objOut);
                    }
                }
            }else{
                exec.setobj(to, null);
            }
        }
    }

    public static class RadarI implements LInstruction{
        public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
        public RadarSort sort = RadarSort.distance;
        public int radar, sortOrder, output;

        //radar instructions are special in that they cache their output and only change it at fixed intervals.
        //this prevents lag from spam of radar instructions
        public Healthc lastTarget;
        public Object lastSourceBuild;
        public Interval timer = new Interval();

        static float bestValue = 0f;
        static Unit best = null;

        public RadarI(RadarTarget target1, RadarTarget target2, RadarTarget target3, RadarSort sort, int radar, int sortOrder, int output){
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
            Object base = exec.obj(radar);

            int sortDir = exec.bool(sortOrder) ? 1 : -1;
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

                exec.setobj(output, targeted);
            }else{
                exec.setobj(output, null);
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
        public int from, to;

        public SetI(int from, int to){
            this.from = from;
            this.to = to;
        }

        SetI(){}

        @Override
        public void run(LExecutor exec){
            Var v = exec.var(to);
            Var f = exec.var(from);

            if(!v.constant){
                if(f.isobj){
                    v.objval = f.objval;
                    v.isobj = true;
                }else{
                    v.numval = invalid(f.numval) ? 0 : f.numval;
                    v.isobj = false;
                }
            }
        }
    }

    public static class OpI implements LInstruction{
        public LogicOp op = LogicOp.add;
        public int a, b, dest;

        public OpI(LogicOp op, int a, int b, int dest){
            this.op = op;
            this.a = a;
            this.b = b;
            this.dest = dest;
        }

        OpI(){}

        @Override
        public void run(LExecutor exec){
            if(op == LogicOp.strictEqual){
                Var v = exec.var(a), v2 = exec.var(b);
                exec.setnum(dest, v.isobj == v2.isobj && ((v.isobj && Structs.eq(v.objval, v2.objval)) || (!v.isobj && v.numval == v2.numval)) ? 1 : 0);
            }else if(op.unary){
                exec.setnum(dest, op.function1.get(exec.num(a)));
            }else{
                Var va = exec.var(a);
                Var vb = exec.var(b);

                if(op.objFunction2 != null && va.isobj && vb.isobj){
                    //use object function if both are objects
                    exec.setnum(dest, op.objFunction2.get(exec.obj(a), exec.obj(b)));
                }else{
                    //otherwise use the numeric function
                    exec.setnum(dest, op.function2.get(exec.num(a), exec.num(b)));
                }

            }
        }
    }

    public static class EndI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            exec.var(varCounter).numval = exec.instructions.length;
        }
    }

    public static class NoopI implements LInstruction{
        @Override
        public void run(LExecutor exec){}
    }

    public static class DrawI implements LInstruction{
        public byte type;
        public int target;
        public int x, y, p1, p2, p3, p4;

        public DrawI(byte type, int target, int x, int y, int p1, int p2, int p3, int p4){
            this.type = type;
            this.target = target;
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

            int num1 = exec.numi(p1);

            if(type == LogicDisplay.commandImage){
                num1 = exec.obj(p1) instanceof UnlockableContent u ? u.iconId : 0;
            }

            //explicitly unpack colorPack, it's pre-processed here
            if(type == LogicDisplay.commandColorPack){
                double packed = exec.num(x);

                int value = (int)(Double.doubleToRawLongBits(packed)),
                r = ((value & 0xff000000) >>> 24),
                g = ((value & 0x00ff0000) >>> 16),
                b = ((value & 0x0000ff00) >>> 8),
                a = ((value & 0x000000ff));

                exec.graphicsBuffer.add(DisplayCmd.get(LogicDisplay.commandColor, pack(r), pack(g), pack(b), pack(a), 0, 0));
            }else{
                //add graphics calls, cap graphics buffer size
                exec.graphicsBuffer.add(DisplayCmd.get(type, packSign(exec.numi(x)), packSign(exec.numi(y)), packSign(num1), packSign(exec.numi(p2)), packSign(exec.numi(p3)), packSign(exec.numi(p4))));
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
        public int target;

        public DrawFlushI(int target){
            this.target = target;
        }

        public DrawFlushI(){
        }

        @Override
        public void run(LExecutor exec){
            //graphics on headless servers are useless.
            if(Vars.headless) return;

            if(exec.building(target) instanceof LogicDisplayBuild d && (d.team == exec.team || exec.privileged)){
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
        public int value;

        public PrintI(int value){
            this.value = value;
        }

        PrintI(){}

        @Override
        public void run(LExecutor exec){

            if(exec.textBuffer.length() >= maxTextBuffer) return;

            //this should avoid any garbage allocation
            Var v = exec.var(value);
            if(v.isobj && value != 0){
                String strValue = toString(v.objval);

                exec.textBuffer.append(strValue);
            }else{
                //display integer version when possible
                if(Math.abs(v.numval - (long)v.numval) < 0.00001){
                    exec.textBuffer.append((long)v.numval);
                }else{
                    exec.textBuffer.append(v.numval);
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

    public static class PrintFlushI implements LInstruction{
        public int target;

        public PrintFlushI(int target){
            this.target = target;
        }

        public PrintFlushI(){
        }

        @Override
        public void run(LExecutor exec){
            
            if(exec.building(target) instanceof MessageBuild d && (d.team == exec.team || exec.privileged)){

                d.message.setLength(0);
                d.message.append(exec.textBuffer, 0, Math.min(exec.textBuffer.length(), maxTextBuffer));

            }
            exec.textBuffer.setLength(0);

        }
    }

    public static class JumpI implements LInstruction{
        public ConditionOp op = ConditionOp.notEqual;
        public int value, compare, address;

        public JumpI(ConditionOp op, int value, int compare, int address){
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
                Var va = exec.var(value);
                Var vb = exec.var(compare);
                boolean cmp;

                if(op == ConditionOp.strictEqual){
                    cmp = va.isobj == vb.isobj && ((va.isobj && va.objval == vb.objval) || (!va.isobj && va.numval == vb.numval));
                }else if(op.objFunction != null && va.isobj && vb.isobj){
                    //use object function if both are objects
                    cmp = op.objFunction.get(exec.obj(value), exec.obj(compare));
                }else{
                    cmp = op.function.get(exec.num(value), exec.num(compare));
                }

                if(cmp){
                    exec.var(varCounter).numval = address;
                }
            }
        }
    }

    public static class WaitI implements LInstruction{
        public int value;

        public float curTime;
        public long frameId;

        public WaitI(int value){
            this.value = value;
        }

        public WaitI(){
        }

        @Override
        public void run(LExecutor exec){
            if(curTime >= exec.num(value)){
                curTime = 0f;
            }else{
                //skip back to self.
                exec.var(varCounter).numval --;
            }

            if(state.updateId != frameId){
                curTime += Time.delta / 60f;
                frameId = state.updateId;
            }
        }
    }

    public static class StopI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            //skip back to self.
            exec.var(varCounter).numval --;
        }
    }

    //TODO inverse lookup
    public static class LookupI implements LInstruction{
        public int dest;
        public int from;
        public ContentType type;

        public LookupI(int dest, int from, ContentType type){
            this.dest = dest;
            this.from = from;
            this.type = type;
        }

        public LookupI(){
        }

        @Override
        public void run(LExecutor exec){
            exec.setobj(dest, logicVars.lookupContent(type, exec.numi(from)));
        }
    }

    public static class PackColorI implements LInstruction{
        public int result, r, g, b, a;

        public PackColorI(int result, int r, int g, int b, int a){
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
            exec.setnum(result, Color.toDoubleBits(Mathf.clamp(exec.numf(r)), Mathf.clamp(exec.numf(g)), Mathf.clamp(exec.numf(b)), Mathf.clamp(exec.numf(a))));
        }
    }

    public static class CutsceneI implements LInstruction{
        public CutsceneAction action = CutsceneAction.stop;
        public int p1, p2, p3, p4;

        public CutsceneI(CutsceneAction action, int p1, int p2, int p3, int p4){
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
                    control.input.logicCamPan.set(World.unconv(exec.numf(p1)), World.unconv(exec.numf(p2)));
                    control.input.logicCamSpeed = exec.numf(p3);
                }
                case zoom -> {
                    control.input.logicCutscene = true;
                    control.input.logicCutsceneZoom = Mathf.clamp(exec.numf(p1));
                }
                case stop -> {
                    control.input.logicCutscene = false;
                }
            }
        }
    }

    public static class FetchI implements LInstruction{
        public FetchType type = FetchType.unit;
        public int result, team, extra, index;

        public FetchI(FetchType type, int result, int team, int extra, int index){
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
            int i = exec.numi(index);
            Team t = exec.team(team);
            if(t == null) return;
            TeamData data = t.data();

            switch(type){
                case unit -> exec.setobj(result, i < 0 || i >= data.units.size ? null : data.units.get(i));
                case player -> exec.setobj(result, i < 0 || i >= data.players.size || data.players.get(i).unit().isNull() ? null : data.players.get(i).unit());
                case core -> exec.setobj(result, i < 0 || i >= data.cores.size ? null : data.cores.get(i));
                case build -> {
                    Block block = exec.obj(extra) instanceof Block b ? b : null;
                    if(block == null){
                        exec.setobj(result, null);
                    }else{
                        var builds = data.getBuildings(block);
                        exec.setobj(result, i < 0 || i >= builds.size ? null : builds.get(i));
                    }
                }
                case unitCount -> exec.setnum(result, data.units.size);
                case coreCount -> exec.setnum(result, data.cores.size);
                case playerCount -> exec.setnum(result, data.players.size);
                case buildCount -> {
                    Block block = exec.obj(extra) instanceof Block b ? b : null;
                    if(block == null){
                        exec.setobj(result, null);
                    }else{
                        exec.setnum(result, data.getBuildings(block).size);
                    }
                }
            }
        }
    }

    //endregion
    //region privileged / world instructions

    public static class GetBlockI implements LInstruction{
        public int x, y;
        public int dest;
        public TileLayer layer = TileLayer.block;

        public GetBlockI(int x, int y, int dest, TileLayer layer){
            this.x = x;
            this.y = y;
            this.dest = dest;
            this.layer = layer;
        }

        public GetBlockI(){
        }

        @Override
        public void run(LExecutor exec){
            Tile tile = world.tile(exec.numi(x), exec.numi(y));
            if(tile == null){
                exec.setobj(dest, null);
            }else{
                exec.setobj(dest, switch(layer){
                    case floor -> tile.floor();
                    case ore -> tile.overlay();
                    case block -> tile.block();
                    case building -> tile.build;
                });
            }
        }
    }

    public static class SetBlockI implements LInstruction{
        public int x, y;
        public int block;
        public int team, rotation;
        public TileLayer layer = TileLayer.block;

        public SetBlockI(int x, int y, int block, int team, int rotation, TileLayer layer){
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

            Tile tile = world.tile(exec.numi(x), exec.numi(y));
            if(tile != null && exec.obj(block) instanceof Block b){
                //TODO this can be quite laggy...
                switch(layer){
                    case ore -> {
                        if((b instanceof OverlayFloor || b == Blocks.air) && tile.overlay() != b) tile.setOverlayNet(b);
                    }
                    case floor -> {
                        if(b instanceof Floor f && tile.floor() != f && !f.isOverlay()) tile.setFloorNet(f);
                    }
                    case block -> {
                        if(!b.isFloor() || b == Blocks.air){
                            Team t = exec.team(team);
                            if(t == null) t = Team.derelict;

                            if(tile.block() != b || tile.team() != t){
                                tile.setNet(b, t, Mathf.clamp(exec.numi(rotation), 0, 3));
                            }
                        }
                    }
                    //building case not allowed
                }
            }
        }
    }

    public static class SpawnUnitI implements LInstruction{
        public int type, x, y, rotation, team, result;

        public SpawnUnitI(int type, int x, int y, int rotation, int team, int result){
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

            Team t = exec.team(team);

            if(exec.obj(type) instanceof UnitType type && !type.hidden && t != null && Units.canCreate(t, type)){
                //random offset to prevent stacking
                var unit = type.spawn(t, World.unconv(exec.numf(x)) + Mathf.range(0.01f), World.unconv(exec.numf(y)) + Mathf.range(0.01f));
                spawner.spawnEffect(unit, exec.numf(rotation));
                exec.setobj(result, unit);
            }
        }
    }

    public static class ApplyEffectI implements LInstruction{
        public boolean clear;
        public String effect;
        public int unit, duration;

        public ApplyEffectI(boolean clear, String effect, int unit, int duration){
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

            if(exec.obj(unit) instanceof Unit unit && content.statusEffect(effect) != null){
                if(clear){
                    unit.unapply(content.statusEffect(effect));
                }else{
                    unit.apply(content.statusEffect(effect), exec.numf(duration) * 60f);
                }
            }
        }
    }

    public static class SetRuleI implements LInstruction{
        public LogicRule rule = LogicRule.waveSpacing;
        public int value, p1, p2, p3, p4;

        public SetRuleI(LogicRule rule, int value, int p1, int p2, int p3, int p4){
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
                case waveTimer -> state.rules.waveTimer = exec.bool(value);
                case wave -> state.wave = exec.numi(value);
                case currentWaveTime -> state.wavetime = exec.numf(value) * 60f;
                case waves -> state.rules.waves = exec.bool(value);
                case waveSending -> state.rules.waveSending = exec.bool(value);
                case attackMode -> state.rules.attackMode = exec.bool(value);
                case waveSpacing -> state.rules.waveSpacing = exec.numf(value) * 60f;
                case enemyCoreBuildRadius -> state.rules.enemyCoreBuildRadius = exec.numf(value) * 8f;
                case dropZoneRadius -> state.rules.dropZoneRadius = exec.numf(value) * 8f;
                case unitCap -> state.rules.unitCap = exec.numi(value);
                case lighting -> state.rules.lighting = exec.bool(value);
                case mapArea -> {
                    int x = exec.numi(p1), y = exec.numi(p2), w = exec.numi(p3), h = exec.numi(p4);
                    if(!checkMapArea(x, y, w, h, false)){
                        Call.setMapArea(x, y, w, h);
                    }
                }
                case ambientLight -> state.rules.ambientLight.fromDouble(exec.num(value));
                case solarMultiplier -> state.rules.solarMultiplier = exec.numf(value);
                case unitBuildSpeed, unitCost, unitDamage, blockHealth, blockDamage, buildSpeed, rtsMinSquad, rtsMinWeight -> {
                    Team team = exec.team(p1);
                    if(team != null){
                        float num = exec.numf(value);
                        switch(rule){
                            case buildSpeed -> team.rules().buildSpeedMultiplier = Mathf.clamp(num, 0.001f, 50f);
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
        public int duration;

        public FlushMessageI(MessageType type, int duration){
            this.type = type;
            this.duration = duration;
        }

        public FlushMessageI(){
        }

        @Override
        public void run(LExecutor exec){
            if(headless && type != MessageType.mission) return;

            //skip back to self until possible
            //TODO this is guaranteed desync on servers - I don't see a good solution
            if(
                type == MessageType.announce && ui.hasAnnouncement() ||
                type == MessageType.notify && ui.hudfrag.hasToast() ||
                type == MessageType.toast && ui.hasAnnouncement()
            ){
                exec.var(varCounter).numval --;
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
                case announce -> ui.announce(text, exec.numf(duration));
                case toast -> ui.showInfoToast(text, exec.numf(duration));
                //TODO desync?
                case mission -> state.rules.mission = text;
            }

            exec.textBuffer.setLength(0);
        }
    }

    public static class ExplosionI implements LInstruction{
        public int team, x, y, radius, damage, air, ground, pierce;

        public ExplosionI(int team, int x, int y, int radius, int damage, int air, int ground, int pierce){
            this.team = team;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damage = damage;
            this.air = air;
            this.ground = ground;
            this.pierce = pierce;
        }

        public ExplosionI(){
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            Team t = exec.team(team);
            //note that there is a radius cap
            Call.logicExplosion(t, World.unconv(exec.numf(x)), World.unconv(exec.numf(y)), World.unconv(Math.min(exec.numf(radius), 100)), exec.numf(damage), exec.bool(air), exec.bool(ground), exec.bool(pierce));
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void logicExplosion(Team team, float x, float y, float radius, float damage, boolean air, boolean ground, boolean pierce){
        if(damage < 0f) return;

        Damage.damage(team, x, y, radius, damage, pierce, air, ground);
        if(pierce){
            Fx.spawnShockwave.at(x, y, World.conv(radius));
        }else{
            Fx.dynamicExplosion.at(x, y, World.conv(radius) / 8f);
        }
    }

    public static class SetRateI implements LInstruction{
        public int amount;

        public SetRateI(int amount){
            this.amount = amount;
        }

        public SetRateI(){
        }

        @Override
        public void run(LExecutor exec){
            if(exec.build != null && exec.build.block.privileged){
                exec.build.ipt = Mathf.clamp(exec.numi(amount), 1, ((LogicBlock)exec.build.block).maxInstructionsPerTick);
                if(exec.iptIndex >= 0 && exec.vars.length > exec.iptIndex){
                    exec.vars[exec.iptIndex].numval = exec.build.ipt;
                }
            }
        }
    }

    public static class GetFlagI implements LInstruction{
        public int result, flag;

        public GetFlagI(int result, int flag){
            this.result = result;
            this.flag = flag;
        }

        public GetFlagI(){
        }

        @Override
        public void run(LExecutor exec){
            if(exec.obj(flag) instanceof String str){
                exec.setbool(result, state.rules.objectiveFlags.contains(str));
            }else{
                exec.setobj(result, null);
            }
        }
    }

    public static class SetFlagI implements LInstruction{
        public int flag, value;

        public SetFlagI(int flag, int value){
            this.flag = flag;
            this.value = value;
        }

        public SetFlagI(){
        }

        @Override
        public void run(LExecutor exec){
            if(exec.obj(flag) instanceof String str){
                if(!exec.bool(value)){
                    state.rules.objectiveFlags.remove(str);
                }else{
                    state.rules.objectiveFlags.add(str);
                }
            }
        }
    }

    public static class SpawnWaveI implements LInstruction{
        public int natural;
        public int x, y;

        public SpawnWaveI(){
        }

        public SpawnWaveI(int natural, int x, int y){
            this.natural = natural;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run(LExecutor exec){
            if(net.client()) return;

            if(exec.bool(natural)){
                logic.skipWave();
                return;
            }

            float
                spawnX = World.unconv(exec.numf(x)),
                spawnY = World.unconv(exec.numf(y));
            int packed = Point2.pack(exec.numi(x), exec.numi(y));

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

    //endregion
}
