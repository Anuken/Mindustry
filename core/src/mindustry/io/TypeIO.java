package mindustry.io;

import arc.audio.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;

import java.io.*;
import java.nio.*;

import static mindustry.Vars.*;

/** Class for specifying read/write methods for code generation. All IO MUST be thread safe!*/
@SuppressWarnings("unused")
@TypeIOHandler
public class TypeIO{

    public static void writeObject(Writes write, Object object){
        if(object == null){
            write.b((byte)0);
        }else if(object instanceof Integer i){
            write.b((byte)1);
            write.i(i);
        }else if(object instanceof Long l){
            write.b((byte)2);
            write.l(l);
        }else if(object instanceof Float f){
            write.b((byte)3);
            write.f(f);
        }else if(object instanceof String s){
            write.b((byte)4);
            writeString(write, s);
        }else if(object instanceof Content map){
            write.b((byte)5);
            write.b((byte)map.getContentType().ordinal());
            write.s(map.id);
        }else if(object instanceof IntSeq arr){
            write.b((byte)6);
            write.s((short)arr.size);
            for(int i = 0; i < arr.size; i++){
                write.i(arr.items[i]);
            }
        }else if(object instanceof Point2 p){
            write.b((byte)7);
            write.i(p.x);
            write.i(p.y);
        }else if(object instanceof Point2[] p){
            write.b((byte)8);
            write.b(p.length);
            for(Point2 point2 : p){
                write.i(point2.pack());
            }
        }else if(object instanceof TechNode map){
            write.b(9);
            write.b((byte)map.content.getContentType().ordinal());
            write.s(map.content.id);
        }else if(object instanceof Boolean b){
            write.b((byte)10);
            write.bool(b);
        }else if(object instanceof Double d){
            write.b((byte)11);
            write.d(d);
        }else if(object instanceof Building b){
            write.b(12);
            write.i(b.pos());
        }else if(object instanceof BuildingBox b){
            write.b(12);
            write.i(b.pos);
        }else if(object instanceof LAccess l){
            write.b((byte)13);
            write.s(l.ordinal());
        }else if(object instanceof byte[] b){
            write.b((byte)14);
            write.i(b.length);
            write.b(b);
        }else if(object instanceof boolean[] b){
            write.b(16);
            write.i(b.length);
            for(boolean bool : b){
                write.bool(bool);
            }
        }else if(object instanceof Unit u){
            write.b(17);
            write.i(u.id);
        }else if(object instanceof UnitBox u){
            write.b(17);
            write.i(u.id);
        }else if(object instanceof Vec2[] vecs){
            write.b(18);
            write.s(vecs.length);
            for(Vec2 v : vecs){
                write.f(v.x);
                write.f(v.y);
            }
        }else if(object instanceof Vec2 v){
            write.b((byte)19);
            write.f(v.x);
            write.f(v.y);
        }else if(object instanceof Team t){
            write.b((byte)20);
            write.b(t.id);
        }else if(object instanceof int[] i){
            write.b((byte)21);
            writeInts(write, i);
        }else if(object instanceof Object[] objs){
            write.b((byte)22);
            write.i(objs.length);
            for(Object obj : objs){
                writeObject(write, obj);
            }
        }else{
            throw new IllegalArgumentException("Unknown object type: " + object.getClass());
        }
    }

    @Nullable
    public static Object readObject(Reads read){
        return readObjectBoxed(read, false);
    }

    /** Reads an object, but boxes buildings. */
    @Nullable
    public static Object readObjectBoxed(Reads read, boolean box){
        byte type = read.b();
        return switch(type){
            case 0 -> null;
            case 1 -> read.i();
            case 2 -> read.l();
            case 3 -> read.f();
            case 4 -> readString(read);
            case 5 -> content.getByID(ContentType.all[read.b()], read.s());
            case 6 -> {
                short length = read.s();
                IntSeq arr = new IntSeq(length);
                for(int i = 0; i < length; i ++) arr.add(read.i());
                yield arr;
            }
            case 7 -> new Point2(read.i(), read.i());
            case 8 -> {
                byte len = read.b();
                Point2[] out = new Point2[len];
                for(int i = 0; i < len; i ++) out[i] = Point2.unpack(read.i());
                yield out;
            }
            case 9 -> content.<UnlockableContent>getByID(ContentType.all[read.b()], read.s()).techNode;
            case 10 -> read.bool();
            case 11 -> read.d();
            case 12 -> !box ? world.build(read.i()) : new BuildingBox(read.i());
            case 13 -> LAccess.all[read.s()];
            case 14 -> {
                int blen = read.i();
                byte[] bytes = new byte[blen];
                read.b(bytes);
                yield bytes;
            }
            //unit command
            case 15 -> {
                read.b();
                yield null;
            }
            case 16 -> {
                int boollen = read.i();
                boolean[] bools = new boolean[boollen];
                for(int i = 0; i < boollen; i ++) bools[i] = read.bool();
                yield bools;
            }
            case 17 -> !box ? Groups.unit.getByID(read.i()) : new UnitBox(read.i());
            case 18 -> {
                int len = read.s();
                Vec2[] out = new Vec2[len];
                for(int i = 0; i < len; i ++) out[i] = new Vec2(read.f(), read.f());
                yield out;
            }
            case 19 -> new Vec2(read.f(), read.f());
            case 20 -> Team.all[read.ub()];
            case 21 -> readInts(read);
            case 22 -> {
                int objlen = read.i();
                Object[] objs = new Object[objlen];
                for(int i = 0; i < objlen; i++) objs[i] = readObjectBoxed(read, box);
                yield objs;
            }
            default -> throw new IllegalArgumentException("Unknown object type: " + type);
        };
    }

    public static void writePayload(Writes writes, Payload payload){
        Payload.write(payload, writes);
    }

    public static Payload readPayload(Reads read){
        return Payload.read(read);
    }

    public static void writeMounts(Writes writes, WeaponMount[] mounts){
        writes.b(mounts.length);
        for(WeaponMount m : mounts){
            writes.b((m.shoot ? 1 : 0) | (m.rotate ? 2 : 0));
            writes.f(m.aimX);
            writes.f(m.aimY);
        }
    }

    public static WeaponMount[] readMounts(Reads read, WeaponMount[] mounts){
        byte len = read.b();
        for(int i = 0; i < len; i++){
            byte state = read.b();
            float ax = read.f(), ay = read.f();

            if(i <= mounts.length - 1){
                WeaponMount m = mounts[i];
                m.aimX = ax;
                m.aimY = ay;
                m.shoot = (state & 1) != 0;
                m.rotate = (state & 2) != 0;
            }
        }

        return mounts;
    }

    //this is irrelevant.
    static final WeaponMount[] noMounts = {};
    
    public static WeaponMount[] readMounts(Reads read){
        read.skip(read.b() * (1 + 4 + 4));

        return noMounts;
    }

    public static Ability[] readAbilities(Reads read, Ability[] abilities){
        byte len = read.b();
        for(int i = 0; i < len; i++){
            float data = read.f();
            if(abilities.length > i){
                abilities[i].data = data;
            }
        }
        return abilities;
    }

    public static void writeAbilities(Writes write, Ability[] abilities){
        write.b(abilities.length);
        for(var a : abilities){
            write.f(a.data);
        }
    }

    static final Ability[] noAbilities = {};

    public static Ability[] readAbilities(Reads read){
        read.skip(read.b());
        return noAbilities;
    }

    public static void writeUnit(Writes write, Unit unit){
        write.b(unit == null || unit.isNull() ? 0 : unit instanceof BlockUnitc ? 1 : 2);

        //block units are special
        if(unit instanceof BlockUnitc){
            write.i(((BlockUnitc)unit).tile().pos());
        }else if(unit == null){
            write.i(0);
        }else{
            write.i(unit.id);
        }
    }

    public static Unit readUnit(Reads read){
        byte type = read.b();
        int id = read.i();
        //nothing
        if(type == 0) return Nulls.unit;
        if(type == 2){ //standard unit
            Unit unit = Groups.unit.getByID(id);
            return unit == null ? Nulls.unit : unit;
        }else if(type == 1){ //block
            Building tile = world.build(id);
            return tile instanceof ControlBlock cont ? cont.unit() : Nulls.unit;
        }
        return Nulls.unit;
    }

    public static void writeCommand(Writes write, UnitCommand command){
        write.b(command.id);
    }

    public static UnitCommand readCommand(Reads read){
        return UnitCommand.all.get(read.ub());
    }

    public static void writeEntity(Writes write, Entityc entity){
        write.i(entity == null ? -1 : entity.id());
    }

    public static <T extends Entityc> T readEntity(Reads read){
        return (T)Groups.sync.getByID(read.i());
    }

    public static void writeBuilding(Writes write, Building tile){
        write.i(tile == null ? -1 : tile.pos());
    }

    public static Building readBuilding(Reads read){
        return world.build(read.i());
    }

    public static void writeTile(Writes write, Tile tile){
        write.i(tile == null ? Point2.pack(-1, -1) : tile.pos());
    }

    public static Tile readTile(Reads read){
        return world.tile(read.i());
    }

    public static void writeBlock(Writes write, Block block){
        write.s(block.id);
    }

    public static Block readBlock(Reads read){
        return content.block(read.s());
    }

    /** @return the maximum acceptable amount of plans to send over the network */
    public static int getMaxPlans(Queue<BuildPlan> plans){
        //limit to prevent buffer overflows
        int used = Math.min(plans.size, 20);
        int totalLength = 0;

        //prevent buffer overflow by checking config length
        for(int i = 0; i < used; i++){
            BuildPlan plan = plans.get(i);
            if(plan.config instanceof byte[] b){
                totalLength += b.length;
            }

            if(plan.config instanceof String b){
                totalLength += b.length();
            }

            if(totalLength > 500){
                used = i + 1;
                break;
            }
        }

        return used;
    }

    //on the network, plans must be capped by size
    public static void writePlansQueueNet(Writes write, Queue<BuildPlan> plans){
        if(plans == null){
            write.i(-1);
            return;
        }

        int used = getMaxPlans(plans);

        write.i(used);
        for(int i = 0; i < used; i++){
            writePlan(write, plans.get(i));
        }
    }

    public static Queue<BuildPlan> readPlansQueue(Reads read){
        int used = read.i();
        if(used == -1) return null;
        var out = new Queue<BuildPlan>();
        for(int i = 0; i < used; i++){
            out.add(readPlan(read));
        }
        return out;
    }

    public static void writePlan(Writes write, BuildPlan plan){
        write.b(plan.breaking ? (byte)1 : 0);
        write.i(Point2.pack(plan.x, plan.y));
        if(!plan.breaking){
            write.s(plan.block.id);
            write.b((byte)plan.rotation);
            write.b(1); //always has config
            writeObject(write, plan.config);
        }
    }

    public static BuildPlan readPlan(Reads read){
        BuildPlan current;

        byte type = read.b();
        int position = read.i();

        if(world.tile(position) == null){
            return null;
        }

        if(type == 1){ //remove
            current = new BuildPlan(Point2.x(position), Point2.y(position));
        }else{ //place
            short block = read.s();
            byte rotation = read.b();
            boolean hasConfig = read.b() == 1;
            Object config = readObject(read);
            current = new BuildPlan(Point2.x(position), Point2.y(position), rotation, content.block(block));
            //should always happen, but is kept for legacy reasons just in case
            if(hasConfig){
                current.config = config;
            }
        }

        return current;
    }

    public static void writePlans(Writes write, BuildPlan[] plans){
        if(plans == null){
            write.s(-1);
            return;
        }
        write.s((short)plans.length);
        for(BuildPlan plan : plans){
            writePlan(write, plan);
        }
    }

    public static BuildPlan[] readPlans(Reads read){
        short reqamount = read.s();
        if(reqamount == -1){
            return null;
        }

        BuildPlan[] reqs = new BuildPlan[reqamount];
        for(int i = 0; i < reqamount; i++){
            BuildPlan plan = readPlan(read);
            if(plan != null){
                reqs[i] = plan;
            }
        }

        return reqs;
    }

    public static void writeController(Writes write, UnitController control){
        //no real unit controller state is written, only the type
        if(control instanceof Player p){
            write.b(0);
            write.i(p.id);
        }else if(control instanceof LogicAI logic && logic.controller != null){
            write.b(3);
            write.i(logic.controller.pos());
        }else if(control instanceof CommandAI ai){
            write.b(6);
            write.bool(ai.attackTarget != null);
            write.bool(ai.targetPos != null);

            if(ai.targetPos != null){
                write.f(ai.targetPos.x);
                write.f(ai.targetPos.y);
            }
            if(ai.attackTarget != null){
                write.b(ai.attackTarget instanceof Building ? 1 : 0);
                if(ai.attackTarget instanceof Building b){
                    write.i(b.pos());
                }else{
                    write.i(((Unit)ai.attackTarget).id);
                }
            }
            write.b(ai.command == null ? -1 : ai.command.id);
        }else if(control instanceof AssemblerAI){  //hate
            write.b(5);
        }else{
            write.b(2);
        }
    }

    public static UnitController readController(Reads read, UnitController prev){
        byte type = read.b();
        if(type == 0){ //is player
            int id = read.i();
            Player player = Groups.player.getByID(id);
            //make sure player exists
            if(player == null) return prev;
            return player;
        }else if(type == 1){ //formation controller (ignored)
            read.i();
            return prev;
        }else if(type == 3){
            int pos = read.i();
            if(prev instanceof LogicAI pai){
                pai.controller = world.build(pos);
                return pai;
            }else{
                //create new AI for assignment
                LogicAI out = new LogicAI();
                //instantly time out when updated.
                out.controlTimer = LogicAI.logicControlTimeout;
                out.controller = world.build(pos);
                return out;
            }
            //type 4 is the old CommandAI with no commandIndex, type 6 is the new one with the index as a single byte.
        }else if(type == 4 || type == 6){
            CommandAI ai = prev instanceof CommandAI pai ? pai : new CommandAI();

            boolean hasAttack = read.bool(), hasPos = read.bool();
            if(hasPos){
                if(ai.targetPos == null) ai.targetPos = new Vec2();
                ai.targetPos.set(read.f(), read.f());
            }else{
                ai.targetPos = null;
            }
            ai.setupLastPos();

            if(hasAttack){
                byte entityType = read.b();
                if(entityType == 1){
                    ai.attackTarget = world.build(read.i());
                }else{
                    ai.attackTarget = Groups.unit.getByID(read.i());
                }
            }else{
                ai.attackTarget = null;
            }

            if(type == 6){
                byte id = read.b();
                ai.command = id < 0 ? null : UnitCommand.all.get(id);
            }

            return ai;
        }else if(type == 5){
            //augh
            return prev instanceof AssemblerAI ? prev : new AssemblerAI();
        }else{
            //there are two cases here:
            //1: prev controller was not a player, carry on
            //2: prev controller was a player, so replace this controller with *anything else*
            //...since AI doesn't update clientside it doesn't matter
            //TODO I hate this
            return (!(prev instanceof AIController) || (prev instanceof LogicAI)) ? new GroundAI() : prev;
        }
    }

    public static void writeKick(Writes write, KickReason reason){
        write.b((byte)reason.ordinal());
    }

    public static KickReason readKick(Reads read){
        return KickReason.values()[read.b()];
    }

    public static void writeRules(Writes write, Rules rules){
        String string = JsonIO.write(rules);
        byte[] bytes = string.getBytes(charset);
        write.i(bytes.length);
        write.b(bytes);
    }

    public static Rules readRules(Reads read){
        int length = read.i();
        String string = new String(read.b(new byte[length]), charset);
        return JsonIO.read(Rules.class, string);
    }

    public static void writeObjectives(Writes write, MapObjectives executor){
        String string = JsonIO.write(executor);
        byte[] bytes = string.getBytes(charset);
        write.i(bytes.length);
        write.b(bytes);
    }

    public static MapObjectives readObjectives(Reads read){
        int length = read.i();
        String string = new String(read.b(new byte[length]), charset);
        return JsonIO.read(MapObjectives.class, string);
    }

    public static void writeVecNullable(Writes write, @Nullable Vec2 v){
        if(v == null){
            write.f(Float.NaN);
            write.f(Float.NaN);
        }else{
            write.f(v.x);
            write.f(v.y);
        }
    }

    public static @Nullable Vec2 readVecNullable(Reads read){
        float x = read.f(), y = read.f();
        return Float.isNaN(x) || Float.isNaN(y) ? null : new Vec2(x, y);
    }

    public static void writeVec2(Writes write, Vec2 v){
        if(v == null){
            write.f(0);
            write.f(0);
        }else{
            write.f(v.x);
            write.f(v.y);
        }
    }

    public static Vec2 readVec2(Reads read, Vec2 base){
        return base.set(read.f(), read.f());
    }

    public static Vec2 readVec2(Reads read){
        return new Vec2(read.f(), read.f());
    }

    public static void writeStatus(Writes write, StatusEntry entry){
        write.s(entry.effect.id);
        write.f(entry.time);
    }

    public static StatusEntry readStatus(Reads read){
        return new StatusEntry().set(content.getByID(ContentType.status, read.s()), read.f());
    }

    public static void writeItems(Writes write, ItemStack stack){
        writeItem(write, stack.item);
        write.i(stack.amount);
    }

    public static ItemStack readItems(Reads read, ItemStack stack){
        return stack.set(readItem(read), read.i());
    }

    public static ItemStack readItems(Reads read){
        return new ItemStack(readItem(read), read.i());
    }

    public static void writeTeam(Writes write, Team team){
        write.b(team == null ? 0 : team.id);
    }

    public static Team readTeam(Reads read){
        return Team.get(read.b());
    }

    public static void writeAction(Writes write, AdminAction reason){
        write.b((byte)reason.ordinal());
    }

    public static AdminAction readAction(Reads read){
        return AdminAction.values()[read.b()];
    }

    public static void writeUnitType(Writes write, UnitType effect){
        write.s(effect.id);
    }

    public static UnitType readUnitType(Reads read){
        return content.getByID(ContentType.unit, read.s());
    }

    public static void writeEffect(Writes write, Effect effect){
        write.s(effect.id);
    }

    public static Effect readEffect(Reads read){
        return Effect.get(read.us());
    }

    public static void writeColor(Writes write, Color color){
        write.i(color.rgba());
    }

    public static Color readColor(Reads read){
        return new Color(read.i());
    }

    public static Color readColor(Reads read, Color color){
        return color.set(read.i());
    }

    public static void writeIntSeq(Writes write, IntSeq seq){
        write.i(seq.size);
        for(int i = 0; i < seq.size; i++){
            write.i(seq.items[i]);
        }
    }

    public static IntSeq readIntSeq(Reads read){
        int size = read.i();
        IntSeq result = new IntSeq(size);
        for(int i = 0; i < size; i++){
            result.items[i] = read.i();
        }
        result.size = size;
        return result;
    }

    public static void writeContent(Writes write, Content cont){
        write.b(cont.getContentType().ordinal());
        write.s(cont.id);
    }

    public static Content readContent(Reads read){
        byte id = read.b();
        return content.getByID(ContentType.all[id], read.s());
    }

    public static void writeLiquid(Writes write, Liquid liquid){
        write.s(liquid == null ? -1 : liquid.id);
    }

    public static Liquid readLiquid(Reads read){
        short id = read.s();
        return id == -1 ? null : content.liquid(id);
    }

    public static void writeBulletType(Writes write, BulletType type){
        write.s(type.id);
    }

    public static BulletType readBulletType(Reads read){
        return content.getByID(ContentType.bullet, read.s());
    }

    public static void writeItem(Writes write, Item item){
        write.s(item == null ? -1 : item.id);
    }

    public static Item readItem(Reads read){
        short id = read.s();
        return id == -1 ? null : content.item(id);
    }

    //note that only the standard sound constants in Sounds are supported; modded sounds are not.
    public static void writeSound(Writes write, Sound sound){
        write.s(Sounds.getSoundId(sound));
    }

    public static Sound readSound(Reads read){
        return Sounds.getSound(read.s());
    }

    public static void writeWeather(Writes write, Weather item){
        write.s(item == null ? -1 : item.id);
    }

    public static Weather readWeather(Reads read){
        short id = read.s();
        return id == -1 ? null : content.getByID(ContentType.weather, id);
    }

    public static void writeString(Writes write, String string){
        if(string != null){
            write.b(1);
            write.str(string);
        }else{
            write.b(0);
        }
    }

    public static String readString(Reads read){
        byte exists = read.b();
        if(exists != 0){
            return read.str();
        }else{
            return null;
        }
    }

    public static void writeString(ByteBuffer write, String string){
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            write.putShort((short)bytes.length);
            write.put(bytes);
        }else{
            write.putShort((short)-1);
        }
    }

    public static String readString(ByteBuffer read){
        short slength = read.getShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            read.get(bytes);
            return new String(bytes, charset);
        }else{
            return null;
        }
    }

    public static void writeBytes(Writes write, byte[] bytes){
        write.s((short)bytes.length);
        write.b(bytes);
    }

    public static byte[] readBytes(Reads read){
        short length = read.s();
        return read.b(new byte[length]);
    }

    public static void writeInts(Writes write, int[] ints){
        write.s((short)ints.length);
        for(int i : ints){
            write.i(i);
        }
    }

    public static int[] readInts(Reads read){
        short length = read.s();
        int[] out = new int[length];
        for(int i = 0; i < length; i++){
            out[i] = read.i();
        }
        return out;
    }

    public static void writeTraceInfo(Writes write, TraceInfo trace){
        writeString(write, trace.ip);
        writeString(write, trace.uuid);
        write.b(trace.modded ? (byte)1 : 0);
        write.b(trace.mobile ? (byte)1 : 0);
        write.i(trace.timesJoined);
        write.i(trace.timesKicked);
    }

    public static TraceInfo readTraceInfo(Reads read){
        return new TraceInfo(readString(read), readString(read), read.b() == 1, read.b() == 1, read.i(), read.i());
    }

    public static void writeStrings(Writes write, String[][] strings){
        write.b(strings.length);
        for(String[] string : strings){
            write.b(string.length);
            for(String s : string){
                writeString(write, s);
            }
        }
    }

    public static String[][] readStrings(Reads read){
        int rows = read.ub();

        String[][] strings = new String[rows][];
        for(int i = 0; i < rows; i++){
            int columns = read.ub();
            strings[i] = new String[columns];
            for(int j = 0; j < columns; j++){
                strings[i][j] = readString(read);
            }
        }
        return strings;
    }

    public static void writeStringData(DataOutput buffer, String string) throws IOException{
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            buffer.writeShort((short)bytes.length);
            buffer.write(bytes);
        }else{
            buffer.writeShort((short)-1);
        }
    }

    public static String readStringData(DataInput buffer) throws IOException{
        short slength = buffer.readShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            buffer.readFully(bytes);
            return new String(bytes, charset);
        }else{
            return null;
        }
    }

    /** Represents a building that has not been resolved yet. */
    public static class BuildingBox{
        public int pos;

        public BuildingBox(int pos){
            this.pos = pos;
        }

        public Building unbox(){
            return world.build(pos);
        }

        @Override
        public String toString(){
            return "BuildingBox{" +
            "pos=" + pos +
            '}';
        }
    }

    /** Represents a unit that has not been resolved yet. TODO unimplemented / unused*/
    public static class UnitBox{
        public int id;

        public UnitBox(int id){
            this.id = id;
        }

        public Unit unbox(){
            return Groups.unit.getByID(id);
        }

        @Override
        public String toString(){
            return "UnitBox{" +
            "id=" + id +
            '}';
        }
    }
}
