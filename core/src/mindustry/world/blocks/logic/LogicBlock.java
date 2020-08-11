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

import static mindustry.Vars.*;

public class LogicBlock extends Block{
    private static final IntSeq removal = new IntSeq();

    public int maxInstructionScale = 8;
    public int instructionsPerTick = 1;
    public float range = 8 * 10;

    public LogicBlock(String name){
        super(name);
        update = true;
        configurable = true;

        config(String.class, (LogicEntity entity, String value) -> {
            if(value.startsWith("{")){ //it's json
                try{
                    LogicConfig conf = JsonIO.read(LogicConfig.class, value);
                    for(int i = 0; i < conf.connections.size; i++){
                        int pos = conf.connections.items[i];
                        conf.connections.items[i] = Point2.pack(Point2.x(pos) + entity.tileX(), Point2.y(pos) + entity.tileY());
                    }
                    entity.connections = conf.connections;
                    entity.updateCode(conf.code);
                }catch(Exception e){
                    //malformed json
                    e.printStackTrace();
                }
            }else{ //it's (probably) asm or just garbage
                entity.updateCode(value);
            }
        });

        config(Integer.class, (LogicEntity entity, Integer pos) -> {
            if(entity.connections.contains(pos)){
                entity.connections.removeValue(pos);
            }else{
                entity.connections.add(pos);
            }

            entity.updateCode(entity.code);
        });
    }

    public class LogicEntity extends Building{
        /** logic "source code" as list of asm statements */
        public String code = "";
        public LExecutor executor = new LExecutor();
        public float accumulator = 0;
        public IntSeq connections = new IntSeq();
        public IntSeq invalidConnections = new IntSeq();
        public boolean loaded = false;

        public void updateCode(String str){
            updateCodeVars(str, null);
        }

        public void updateCodeVars(String str, Cons<LAssembler> assemble){
            if(str != null){
                code = str;

                try{
                    //create assembler to store extra variables
                    LAssembler asm = LAssembler.assemble(str);

                    //store connections
                    for(int i = 0; i < connections.size; i++){
                        asm.putConst("@" + i, world.build(connections.get(i)));
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
                    executor.load("");
                }
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            if(!loaded){
                //properly fetches connections
                updateCode(code);
                loaded = true;
            }
        }

        @Override
        public void updateTile(){
            //check for previously invalid links to add after configuration
            removal.clear();

            for(int i = 0; i < invalidConnections.size; i++){
                int val = invalidConnections.get(i);
                if(validLink(val) && !connections.contains(val)){
                    removal.add(val);
                }
            }

            if(!removal.isEmpty()){
                connections.addAll(removal);
                invalidConnections.removeAll(removal);
                updateCode(code);
            }

            //remove invalid links
            removal.clear();

            for(int i = 0; i < connections.size; i++){
                int val = connections.get(i);
                if(!validLink(val)){
                    removal.add(val);
                }
            }

            if(!removal.isEmpty()){
                invalidConnections.addAll(removal);
                connections.removeAll(removal);
                updateCode(code);
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
        public String config(){
            //set connections to use relative coordinates, not absolute (TODO maybe just store them like this?)
            IntSeq copy = new IntSeq(connections);
            for(int i = 0; i < copy.size; i++){
                int pos = copy.items[i];
                copy.items[i] = Point2.pack(Point2.x(pos) - tileX(), Point2.y(pos) - tileY());
            }
            return JsonIO.write(new LogicConfig(code, copy));
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();

            Drawf.circles(x, y, range);

            for(int i = 0; i < connections.size; i++){
                int pos = connections.get(i);

                if(validLink(pos)){
                    Building other = Vars.world.build(pos);
                    Drawf.square(other.x, other.y, other.block.size * tilesize / 2f + 1f, Pal.place);
                }
            }

            //draw top text on separate layer
            for(int i = 0; i < connections.size; i++){
                int pos = connections.get(i);

                if(validLink(pos)){
                    Building other = Vars.world.build(pos);
                    other.block.drawPlaceText("@" + i, other.tileX(), other.tileY(), true);
                }
            }
        }

        public boolean validLink(int pos){
            Building other = Vars.world.build(pos);
            return other != null && other.team == team && other.within(this, range + other.block.size*tilesize/2f);
        }

        @Override
        public void drawSelect(){

        }

        @Override
        public void buildConfiguration(Table table){
            Table cont = new Table();
            cont.defaults().size(40);

            cont.button(Icon.pencil, Styles.clearTransi, () -> {
                Vars.ui.logic.show(code, this::configure);
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

            if(validLink(other.pos())){
                configure(other.pos());
                return false;
            }

            return super.onConfigureTileTapped(other);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.str(code);
            write.s(connections.size);
            for(int i = 0; i < connections.size; i++){
                write.i(connections.get(i));
            }

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

            code = read.str();
            connections.clear();
            short total = read.s();
            for(int i = 0; i < total; i++){
                connections.add(read.i());
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
