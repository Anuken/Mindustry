package mindustry.logic;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.*;
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
    varTime = 1,
    varUnit = 2,
    varThis = 3,
    varTick = 4;

    public static final int
    maxGraphicsBuffer = 256,
    maxDisplayBuffer = 1024,
    maxTextBuffer = 256;

    public LInstruction[] instructions = {};
    public Var[] vars = {};
    public int[] binds;

    public LongSeq graphicsBuffer = new LongSeq();
    public StringBuilder textBuffer = new StringBuilder();
    public Building[] links = {};
    public IntSet linkIds = new IntSet();
    public Team team = Team.derelict;

    public boolean initialized(){
        return instructions != null && vars != null && instructions.length > 0;
    }

    /** Runs a single instruction. */
    public void runOnce(){
        //set time
        vars[varTime].numval = Time.millis();
        vars[varTick].numval = Time.time;

        //reset to start
        if(vars[varCounter].numval >= instructions.length || vars[varCounter].numval < 0){
            vars[varCounter].numval = 0;
        }

        if(vars[varCounter].numval < instructions.length){
            instructions[(int)(vars[varCounter].numval++)].run(this);
        }
    }

    public void load(String data){
        load(LAssembler.assemble(data));
    }

    /** Loads with a specified assembler. Resets all variables. */
    public void load(LAssembler builder){
        vars = new Var[builder.vars.size];
        instructions = builder.instructions;

        builder.vars.each((name, var) -> {
            Var dest = new Var(name);
            vars[var.id] = dest;

            dest.constant = var.constant;

            if(var.value instanceof Number number){
                dest.isobj = false;
                dest.numval = number.doubleValue();
            }else{
                dest.isobj = true;
                dest.objval = var.value;
            }
        });
    }

    //region utility

    private static boolean invalid(double d){
        return Double.isNaN(d) || Double.isInfinite(d);
    }

    public Var var(int index){
        //global constants have variable IDs < 0, and they are fetched from the global constants object after being negated
        return index < 0 ? constants.get(-index) : vars[index];
    }

    public @Nullable Building building(int index){
        Object o = var(index).objval;
        return var(index).isobj && o instanceof Building building ? building : null;
    }

    public @Nullable Object obj(int index){
        Object o = var(index).objval;
        return var(index).isobj ? o : null;
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
            if(exec.obj(type) instanceof UnitType type){
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
            }else if(exec.obj(type) instanceof Unit u && u.team == exec.team){
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
                            res = Geometry.findClosest(unit.x, unit.y, exec.bool(enemy) ? indexer.getEnemy(unit.team, flag) : indexer.getAllied(unit.team, flag));
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
            if(unitObj instanceof Unit unit && exec.obj(varUnit) == unit && unit.team == exec.team && !unit.isPlayer() && !(unit.controller() instanceof FormationAI)){
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
                    case within -> {
                        exec.setnum(p4, unit.within(x1, y1, d1) ? 1 : 0);
                    }
                    case pathfind -> {
                        ai.control = type;
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
                        if(ai.payTimer > 0) return;

                        if(unit instanceof Payloadc pay && pay.hasPayload()){
                            Call.payloadDropped(unit, unit.x, unit.y);
                            ai.payTimer = LogicAI.transferDelay;
                        }
                    }
                    case payTake -> {
                        if(ai.payTimer > 0) return;

                        if(unit instanceof Payloadc pay){
                            //units
                            if(exec.bool(p1)){
                                Unit result = Units.closest(unit.team, unit.x, unit.y, unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize * 1.2f));

                                if(result != null){
                                    Call.pickedUnitPayload(unit, result);
                                }
                            }else{ //buildings
                                Building tile = world.buildWorld(unit.x, unit.y);

                                //TODO copy pasted code
                                if(tile != null && tile.team == unit.team){
                                    if(tile.block.buildVisibility != BuildVisibility.hidden && tile.canPickup() && pay.canPickup(tile)){
                                        Call.pickedBuildPayload(unit, tile, true);
                                    }else{ //pick up block payload
                                        Payload current = tile.getPayload();
                                        if(current != null && pay.canPickupPayload(current)){
                                            Call.pickedBuildPayload(unit, tile, false);
                                        }
                                    }
                                }
                            }
                            ai.payTimer = LogicAI.transferDelay;
                        }
                    }
                    case build -> {
                        if(state.rules.logicUnitBuild && unit.canBuild() && exec.obj(p3) instanceof Block block){
                            int x = World.toTile(x1 - block.offset/tilesize), y = World.toTile(y1 - block.offset/tilesize);
                            int rot = exec.numi(p4);

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
                        float range = Math.max(unit.range(), buildingRange);
                        if(!unit.within(x1, y1, range)){
                            exec.setobj(p3, null);
                            exec.setobj(p4, null);
                        }else{
                            Tile tile = world.tileWorld(x1, y1);
                            //any environmental solid block is returned as StoneWall, aka "@solid"
                            Block block = tile == null ? null : !tile.synthetic() ? (tile.solid() ? Blocks.stoneWall : Blocks.air) : tile.block();
                            exec.setobj(p3, block);
                            exec.setobj(p4, tile != null && tile.build != null ? tile.build : null);
                        }
                    }
                    case itemDrop -> {
                        if(ai.itemTimer > 0) return;

                        Building build = exec.building(p1);
                        int dropped = Math.min(unit.stack.amount, exec.numi(p2));
                        if(build != null && build.team == unit.team && build.isValid() && dropped > 0 && unit.within(build, logicItemTransferRange + build.block.size * tilesize/2f)){
                            int accepted = build.acceptStack(unit.item(), dropped, unit);
                            if(accepted > 0){
                                Call.transferItemTo(unit, unit.item(), accepted, unit.x, unit.y, build);
                                ai.itemTimer = LogicAI.transferDelay;
                            }
                        }
                    }
                    case itemTake -> {
                        if(ai.itemTimer > 0) return;

                        Building build = exec.building(p1);
                        int amount = exec.numi(p3);

                        if(build != null && build.team == unit.team && build.isValid() && build.items != null &&
                            exec.obj(p2) instanceof Item item && unit.within(build, logicItemTransferRange + build.block.size * tilesize/2f)){
                            int taken = Math.min(build.items.get(item), Math.min(amount, unit.maxAccepted(item)));

                            if(taken > 0){
                                Call.takeItems(build, item, taken, unit);
                                ai.itemTimer = LogicAI.transferDelay;
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
            if(obj instanceof Building b && b.team == exec.team && exec.linkIds.contains(b.id)){
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

            if(from instanceof MemoryBuild mem && from.team == exec.team){

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

            if(from instanceof MemoryBuild mem && from.team == exec.team){

                if(address >= 0 && address < mem.memory.length){
                    mem.memory[address] = exec.num(value);
                }

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

            if(base instanceof Ranged r && r.team() == exec.team &&
                (base instanceof Building || (ai = UnitControlI.checkLogicAI(exec, base)) != null)){ //must be a building or a controllable unit
                float range = r.range();

                Healthc targeted;

                //timers update on a fixed 30 tick interval
                //units update on a special timer per controller instance
                if((base instanceof Building && timer.get(30f)) || (ai != null && ai.checkTargetTimer(this))){
                    //if any of the targets involve enemies
                    boolean enemies = target1 == RadarTarget.enemy || target2 == RadarTarget.enemy || target3 == RadarTarget.enemy;

                    best = null;
                    bestValue = 0;

                    if(enemies){
                        Seq<TeamData> data = state.teams.present;
                        for(int i = 0; i < data.size; i++){
                            if(data.items[i].team != r.team()){
                                find(r, range, sortDir, data.items[i].team);
                            }
                        }
                    }else{
                        find(r, range, sortDir, r.team());
                    }

                    lastTarget = targeted = best;
                }else{
                    targeted = lastTarget;
                }

                exec.setobj(output, targeted);
            }else{
                exec.setobj(output, null);
            }
        }

        void find(Ranged b, float range, int sortDir, Team team){
            Units.nearby(team, b.x(), b.y(), range, u -> {
                if(!u.within(b, range)) return;

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
                exec.setnum(dest, v.isobj == v2.isobj && ((v.isobj && v.objval == v2.objval) || (!v.isobj && v.numval == v2.numval)) ? 1 : 0);
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
            if(Vars.headless) return;

            int num1 = exec.numi(p1);

            if(type == LogicDisplay.commandImage){
                num1 = exec.obj(p1) instanceof UnlockableContent u ? u.iconId : 0;
            }

            //add graphics calls, cap graphics buffer size
            if(exec.graphicsBuffer.size < maxGraphicsBuffer){
                exec.graphicsBuffer.add(DisplayCmd.get(type, packSign(exec.numi(x)), packSign(exec.numi(y)), packSign(num1), packSign(exec.numi(p2)), packSign(exec.numi(p3)), packSign(exec.numi(p4))));
            }
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

            if(exec.building(target) instanceof LogicDisplayBuild d && d.team == exec.team){
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
                String strValue =
                    v.objval == null ? "null" :
                    v.objval instanceof String s ? s :
                    v.objval == Blocks.stoneWall ? "solid" : //special alias
                    v.objval instanceof MappableContent content ? content.name :
                    v.objval instanceof Content ? "[content]" :
                    v.objval instanceof Building build ? build.block.name :
                    v.objval instanceof Unit unit ? unit.type.name :
                    v.objval instanceof Enum<?> e ? e.name() :
                    "[object]";

                exec.textBuffer.append(strValue);
            }else{
                //display integer version when possible
                if(Math.abs(v.numval - (long)v.numval) < 0.000001){
                    exec.textBuffer.append((long)v.numval);
                }else{
                    exec.textBuffer.append(v.numval);
                }
            }
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

            if(exec.building(target) instanceof MessageBuild d && d.team == exec.team){

                d.message.setLength(0);
                d.message.append(exec.textBuffer, 0, Math.min(exec.textBuffer.length(), maxTextBuffer));

                exec.textBuffer.setLength(0);
            }
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
        public double wait;
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

            if(Core.graphics.getFrameId() != frameId){
                curTime += Time.delta / 60f;
                frameId = Core.graphics.getFrameId();
            }
        }
    }

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
            exec.setobj(dest, constants.lookupContent(type, exec.numi(from)));
        }
    }


    //endregion
}
