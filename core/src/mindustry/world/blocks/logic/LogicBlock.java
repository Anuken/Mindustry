package mindustry.world.blocks.logic;

import arc.func.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
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

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class LogicBlock extends Block{
    public static final int maxInstructions = 2000;

    public int maxInstructionScale = 5;
    public int instructionsPerTick = 1;
    public float range = 8 * 10;

    public LogicBlock(String name){
        super(name);
        update = true;
        configurable = true;

        config(byte[].class, (LogicBuild build, byte[] data) -> build.readCompressed(data, true));

        config(Integer.class, (LogicBuild entity, Integer pos) -> {
            //if there is no valid link in the first place, nobody cares
            if(!entity.validLink(world.build(pos))) return;
            int x = Point2.x(pos), y = Point2.y(pos);
            Building lbuild = world.build(pos);

            LogicLink link = entity.links.find(l -> l.x == x && l.y == y);

            if(link != null){
                link.active = !link.active;
            }else{
                String bname = getLinkName(lbuild.block);
                int maxnum = 0;

                for(LogicLink others : entity.links){
                    if(others.name.startsWith(bname)){

                        String num = others.name.substring(bname.length());
                        try{
                            int parsed = Integer.parseInt(num);
                            maxnum = Math.max(parsed, maxnum);
                        }catch(NumberFormatException ignored){
                            //ignore failed parsing, it isn't relevant
                        }
                    }
                }

                LogicLink out = new LogicLink(x, y, bname + (maxnum + 1), true);
                entity.links.add(out);
            }

            entity.updateCode();
        });
    }

    static String getLinkName(Block block){
        String name = block.name;
        if(name.contains("-")){
            String[] split = name.split("-");
            name = split[split.length - 1];
        }
        if(block.minfo.mod != null){
            name = name.substring(block.minfo.mod.name.length() + 1);
        }
        return name;
    }

    static byte[] compress(String code, Seq<LogicLink> links){
        try{
            byte[] bytes = code.getBytes(charset);

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

    public static class LogicLink{
        public boolean active = true, valid = true;
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

    public class LogicBuild extends Building{
        /** logic "source code" as list of asm statements */
        public String code = "";
        public LExecutor executor = new LExecutor();
        public float accumulator = 0;

        //TODO refactor this system, it's broken.
        public Seq<LogicLink> links = new Seq<>();

        public void readCompressed(byte[] data, boolean relative){
            DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)));

            try{
                //discard version, there's only one
                int version = stream.read();

                int bytelen = stream.readInt();
                byte[] bytes = new byte[bytelen];
                stream.read(bytes);

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

        public void updateCode(){
            updateCode(code);
        }

        public void updateCode(String str){
            updateCodeVars(str, null);
        }

        public void updateCodeVars(String str, Cons<LAssembler> assemble){
            if(str != null){
                if(str.length() >= Short.MAX_VALUE) str = str.substring(0, Short.MAX_VALUE - 1);
                code = str;

                try{
                    //create assembler to store extra variables
                    LAssembler asm = LAssembler.assemble(str, maxInstructions);

                    //store connections
                    for(LogicLink link : links){
                        if(link.active && validLink(world.build(link.x, link.y))){
                            asm.putConst(link.name, world.build(link.x, link.y));
                        }
                    }

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

                    executor.load(asm);
                }catch(Exception e){
                    e.printStackTrace();

                    //handle malformed code and replace it with nothing
                    executor.load("", maxInstructions);
                }
            }
        }

        @Override
        public void updateTile(){

            //check for previously invalid links to add after configuration
            boolean changed = false;

            for(LogicLink l : links){
                if(!l.active) continue;

                boolean valid = validLink(world.build(l.x, l.y));
                if(valid != l.valid ){
                    changed = true;
                    l.valid = valid;
                }
            }

            if(changed){
                updateCode();
            }

            accumulator += edelta() * instructionsPerTick;

            if(accumulator > maxInstructionScale * instructionsPerTick) accumulator = maxInstructionScale * instructionsPerTick;

            for(int i = 0; i < (int)accumulator; i++){
                if(executor.initialized()){
                    executor.runOnce();
                }
                accumulator --;
            }
        }

        @Override
        public byte[] config(){
            return compress(code, relativeConnections());
        }

        public Seq<LogicLink> relativeConnections(){
            Seq<LogicLink> copy = new Seq<>(links);
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
            return other != null && other.isValid() && other.team == team && other.within(this, range + other.block.size*tilesize/2f);
        }

        @Override
        public void drawSelect(){

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
                return true;
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
