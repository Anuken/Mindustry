package mindustry.world.blocks.logic;

import arc.func.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.Bits;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.logic.LAssembler.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.meta.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class LogicBlock extends Block{
    public int maxInstructionScale = 5;
    public int instructionsPerTick = 1;
    public float range = 8 * 10;

    public LogicBlock(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;

        config(byte[].class, (LogicBuild build, byte[] data) -> build.readCompressed(data, true));

        config(Integer.class, (LogicBuild entity, Integer pos) -> {
            //if there is no valid link in the first place, nobody cares
            if(!entity.validLink(world.build(pos))) return;
            Building lbuild = world.build(pos);
            int x = lbuild.tileX(), y = lbuild.tileY();

            LogicLink link = entity.links.find(l -> l.x == x && l.y == y);
            String bname = getLinkName(lbuild.block);

            if(link != null){
                link.active = !link.active;
                //find a name when the base name differs (new block type)
                if(!link.name.startsWith(bname)){
                    link.name = "";
                    link.name = entity.findLinkName(lbuild.block);
                }
            }else{
                entity.links.remove(l -> world.build(l.x, l.y) == lbuild);
                LogicLink out = new LogicLink(x, y, entity.findLinkName(lbuild.block), true);
                entity.links.add(out);
            }

            entity.updateCode();
        });
    }

    static String getLinkName(Block block){
        String name = block.name;
        if(name.contains("-")){
            String[] split = name.split("-");
            //filter out 'large' at the end of block names
            if(split.length >= 2 && (split[split.length - 1].equals("large") || Strings.canParseFloat(split[split.length - 1]))){
                name = split[split.length - 2];
            }else{
                name = split[split.length - 1];
            }
        }
        return name;
    }

    static byte[] compress(String code, Seq<LogicLink> links){
        return compress(code.getBytes(charset), links);
    }

    static byte[] compress(byte[] bytes, Seq<LogicLink> links){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(new DeflaterOutputStream(baos));

            //current version of config format
            stream.write(1);

            //write string data
            stream.writeInt(bytes.length);
            stream.write(bytes);

            int actives = links.count(l -> l.active);

            stream.writeInt(actives);
            for(LogicLink link : links){
                if(!link.active) continue;

                stream.writeUTF(link.name);
                stream.writeShort(link.x);
                stream.writeShort(link.y);
            }

            stream.close();

            return baos.toByteArray();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.linkRange, range / 8, StatUnit.blocks);
        stats.add(BlockStat.instructions, instructionsPerTick * 60, StatUnit.perSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.circles(x*tilesize + offset, y*tilesize + offset, range);
    }

    @Override
    public Object pointConfig(Object config, Cons<Point2> transformer){
        if(config instanceof byte[]){
            byte[] data = (byte[])config;

            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))){
                //discard version for now
                stream.read();

                int bytelen = stream.readInt();
                byte[] bytes = new byte[bytelen];
                stream.readFully(bytes);

                int total = stream.readInt();

                Seq<LogicLink> links = new Seq<>();

                for(int i = 0; i < total; i++){
                    String name = stream.readUTF();
                    short x = stream.readShort(), y = stream.readShort();

                    transformer.get(Tmp.p1.set(x, y));
                    links.add(new LogicLink(Tmp.p1.x, Tmp.p1.y, name, true));
                }

                return compress(bytes, links);
            }catch(IOException e){
                Log.err(e);
            }
        }
        return config;
    }

    public static class LogicLink{
        public boolean active = true, valid;
        public int x, y;
        public String name;

        public LogicLink(int x, int y, String name, boolean valid){
            this.x = x;
            this.y = y;
            this.name = name;
            this.valid = valid;
        }

        public LogicLink copy(){
            LogicLink out = new LogicLink(x, y, name, valid);
            out.active = active;
            return out;
        }
    }

    public class LogicBuild extends Building implements Ranged{
        /** logic "source code" as list of asm statements */
        public String code = "";
        public LExecutor executor = new LExecutor();
        public float accumulator = 0;
        public Seq<LogicLink> links = new Seq<>();

        public void readCompressed(byte[] data, boolean relative){
            DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)));

            try{
                int version = stream.read();

                int bytelen = stream.readInt();
                byte[] bytes = new byte[bytelen];
                stream.readFully(bytes);

                links.clear();

                int total = stream.readInt();

                if(version == 0){
                    //old version just had links, ignore those

                    for(int i = 0; i < total; i++){
                        stream.readInt();
                    }
                }else{
                    for(int i = 0; i < total; i++){
                        String name = stream.readUTF();
                        short x = stream.readShort(), y = stream.readShort();

                        if(relative){
                            x += tileX();
                            y += tileY();
                        }
                        links.add(new LogicLink(x, y, name, validLink(world.build(x, y))));
                    }
                }

                updateCode(new String(bytes, charset));
            }catch(IOException e){
                Log.err(e);
            }
        }

        public String findLinkName(Block block){
            String bname = getLinkName(block);
            Bits taken = new Bits(links.size);
            int max = 1;

            for(LogicLink others : links){
                if(others.name.startsWith(bname)){

                    String num = others.name.substring(bname.length());
                    try{
                        int val = Integer.parseInt(num);
                        taken.set(val);
                        max = Math.max(val, max);
                    }catch(NumberFormatException ignored){
                        //ignore failed parsing, it isn't relevant
                    }
                }
            }

            int outnum = 0;

            for(int i = 1; i < max + 2; i++){
                if(!taken.get(i)){
                    outnum = i;
                    break;
                }
            }

            return bname + outnum;
        }

        public void updateCode(){
            updateCode(code);
        }

        public void updateCode(String str){
            updateCodeVars(str, null);
        }

        public void updateCodeVars(String str, Cons<LAssembler> assemble){
            if(str != null){
                code = str;

                try{
                    //create assembler to store extra variables
                    LAssembler asm = LAssembler.assemble(str, LExecutor.maxInstructions);

                    //store connections
                    for(LogicLink link : links){
                        if(link.active && (link.valid = validLink(world.build(link.x, link.y)))){
                            asm.putConst(link.name, world.build(link.x, link.y));
                        }
                    }

                    //store link objects
                    executor.links = new Building[links.count(l -> l.valid && l.active)];
                    int index = 0;
                    for(LogicLink link : links){
                        if(link.active && link.valid){
                            executor.links[index ++] = world.build(link.x, link.y);
                        }
                    }

                    asm.putConst("@links", executor.links.length);
                    asm.putConst("@ipt", instructionsPerTick);

                    //store any older variables
                    for(Var var : executor.vars){
                        if(!var.constant){
                            BVar dest = asm.getVar(var.name);
                            if(dest != null && !dest.constant){
                                dest.value = var.isobj ? var.objval : var.numval;
                            }
                        }
                    }

                    //inject any extra variables
                    if(assemble != null){
                        assemble.get(asm);
                    }

                    asm.putConst("@this", this);
                    asm.putConst("@thisx", x);
                    asm.putConst("@thisy", y);

                    executor.load(asm);
                }catch(Exception e){
                    e.printStackTrace();

                    //handle malformed code and replace it with nothing
                    executor.load("", LExecutor.maxInstructions);
                }
            }
        }

        //logic blocks cause write problems when picked up
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){

            //check for previously invalid links to add after configuration
            boolean changed = false;

            for(int i = 0; i < links.size; i++){
                LogicLink l = links.get(i);

                if(!l.active) continue;

                boolean valid = validLink(world.build(l.x, l.y));
                if(valid != l.valid ){
                    changed = true;
                    l.valid = valid;
                    if(valid){
                        Building lbuild = world.build(l.x, l.y);

                        //this prevents conflicts
                        l.name = "";
                        //finds a new matching name after toggling
                        l.name = findLinkName(lbuild.block);

                        //remove redundant links
                        links.removeAll(o -> world.build(o.x, o.y) == lbuild && o != l);

                        //break to prevent concurrent modification
                        break;
                    }
                }
            }

            if(changed){
                updateCode();
            }

            if(enabled){
                accumulator += edelta() * instructionsPerTick * (consValid() ? 1 : 0);

                if(accumulator > maxInstructionScale * instructionsPerTick) accumulator = maxInstructionScale * instructionsPerTick;

                for(int i = 0; i < (int)accumulator; i++){
                    if(executor.initialized()){
                        executor.runOnce();
                    }
                    accumulator --;
                }
            }
        }

        @Override
        public byte[] config(){
            return compress(code, relativeConnections());
        }

        public Seq<LogicLink> relativeConnections(){
            Seq<LogicLink> copy = new Seq<>(links.size);
            for(LogicLink l : links){
                LogicLink c = l.copy();
                c.x -= tileX();
                c.y -= tileY();
                copy.add(c);
            }
            return copy;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();

            Drawf.circles(x, y, range);

            for(LogicLink l : links){
                Building build = world.build(l.x, l.y);
                if(l.active && validLink(build)){
                    Drawf.square(build.x, build.y, build.block.size * tilesize / 2f + 1f, Pal.place);
                }
            }

            //draw top text on separate layer
            for(LogicLink l : links){
                Building build = world.build(l.x, l.y);
                if(l.active && validLink(build)){
                    build.block.drawPlaceText(l.name, build.tileX(), build.tileY(), true);
                }
            }
        }

        public boolean validLink(Building other){
            return other != null && other.isValid() && other.team == team && other.within(this, range + other.block.size*tilesize/2f) && !(other instanceof ConstructBuild);
        }



        @Override
        public void buildConfiguration(Table table){
            Table cont = new Table();
            cont.defaults().size(40);

            cont.button(Icon.pencil, Styles.clearTransi, () -> {
                Vars.ui.logic.show(code, code -> {
                    configure(compress(code, relativeConnections()));
                });
            });

            //cont.button(Icon.refreshSmall, Styles.clearTransi, () -> {

            //});

            table.add(cont);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                deselect();
                return false;
            }

            if(validLink(other)){
                configure(other.pos());
                return false;
            }

            return super.onConfigureTileTapped(other);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            byte[] compressed = compress(code, links);
            write.i(compressed.length);
            write.b(compressed);

            //write only the non-constant variables
            int count = Structs.count(executor.vars, v -> !v.constant);

            write.i(count);
            for(int i = 0; i < executor.vars.length; i++){
                Var v = executor.vars[i];

                if(v.constant) continue;

                //write the name and the object value
                write.str(v.name);

                Object value = v.isobj ? v.objval : v.numval;
                if(value instanceof Unit) value = null; //do not save units.
                TypeIO.writeObject(write, value);
            }

            //no memory
            write.i(0);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 1){
                int compl = read.i();
                byte[] bytes = new byte[compl];
                read.b(bytes);
                readCompressed(bytes, false);
            }else{

                code = read.str();
                links.clear();
                short total = read.s();
                for(int i = 0; i < total; i++){
                    read.i();
                }
            }

            int varcount = read.i();

            //variables need to be temporarily stored in an array until they can be used
            String[] names = new String[varcount];
            Object[] values = new Object[varcount];

            for(int i = 0; i < varcount; i++){
                String name = read.str();
                Object value = TypeIO.readObject(read);

                names[i] = name;
                values[i] = value;
            }

            int memory = read.i();
            //skip memory, it isn't used anymore
            read.skip(memory * 8);

            updateCodeVars(code, asm -> {

                //load up the variables that were stored
                for(int i = 0; i < varcount; i++){
                    BVar dest = asm.getVar(names[i]);
                    if(dest != null && !dest.constant){
                        dest.value = values[i];
                    }
                }
            });
        }
    }

    public static class LogicConfig{
        public String code;
        public IntSeq connections;

        public LogicConfig(String code, IntSeq connections){
            this.code = code;
            this.connections = connections;
        }

        public LogicConfig(){
        }
    }
}
