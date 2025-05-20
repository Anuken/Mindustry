package mindustry.world.blocks.logic;

import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.io.TypeIO.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MemoryBlock extends Block{
    public int memoryCapacity = 32;

    public MemoryBlock(String name){
        super(name);
        destructible = true;
        solid = true;
        group = BlockGroup.logic;
        drawDisabled = false;
        envEnabled = Env.any;
        canOverdrive = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.memoryCapacity, memoryCapacity, StatUnit.none);
    }

    public boolean accessible(){
        return !privileged || state.rules.editor || state.rules.allowEditWorldProcessors;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }

    public static class MemorySlot{
        public boolean isobj = false;

        public Object objval = null;
        public double numval = 0;

        public void load(LVar source){
            objval = source.objval;
            numval = source.numval;
            isobj = source.isobj;
        }

        public void store(LVar destination){
            if(isobj){
                destination.setobj(objval);
            }else{
                destination.setnum(numval);
            }
        }

        public void setobj(Object obj){
            objval = obj;
            isobj = true;
        }

        public void setnum(double num){
            objval = null;
            numval = num;
            isobj = false;
        }
    }

    public class MemoryBuild extends Building{
        public MemorySlot[] memory = new MemorySlot[memoryCapacity];

        /** Block of code to run after load. */
        private @Nullable Runnable loadBlock;

        public MemoryBuild(){
            super();
            for(int i = 0; i < memory.length; i++){
                memory[i] = new MemorySlot();
            }
        }

        //massive byte size means picking up causes sync issues
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public void updateTile(){
            //load up code from read()
            if(loadBlock != null){
                loadBlock.run();
                loadBlock = null;
            }
        }

        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public boolean displayable(){
            return accessible();
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case memoryCapacity -> memoryCapacity;
                default -> super.sense(sensor);
            };
        }

        @Override
        public void damage(float damage){
            if(privileged) return;
            super.damage(damage);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(memory.length);
            for(MemorySlot s: memory){
                Object value = s.isobj ? s.objval : s.numval;
                TypeIO.writeObject(write, value);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int amount = read.i();

            if(revision == 0){
                for(int i = 0; i < amount; i++){
                    double val = read.d();
                    if (i < memory.length) memory[i].setnum(val);
                }
                return;
            }
            
            //variables need to be temporarily stored in an array until they can be used
            Object[] values = new Object[Math.min(amount, memory.length)];

            for(int i = 0; i < amount; i++){
                Object value = TypeIO.readObjectBoxed(read, true);
                if (i < values.length) values[i] = value;
            }

            loadBlock = () -> {
                //load up the variables that were stored
                for(int i = 0; i < values.length; i++){
                    var slot = memory[i];
                    var value = values[i];
                    if(value instanceof Boxed<?> boxed) value = boxed.unbox();

                    if(value instanceof Number num){
                        slot.setnum(num.doubleValue());
                    }else{
                        slot.setobj(value);
                    }
                }
            };
        }
    }
}
