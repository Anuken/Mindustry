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

import java.util.*;

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

    public class MemoryBuild extends Building implements LReadable, LWritable{
        /** Marks a memory slot as being stored in {@code numberMemory} (insted of {@code objectMemory}) */
        private static final Object sentinel = new Object();

        /** Block of code to run after load. */
        private @Nullable Runnable loadBlock;

        /** Objects stored in this memory building */
        private Object[] objectMemory = new Object[memoryCapacity];

        /** Numbers stored in this memory building */
        private double[] numberMemory = new double[memoryCapacity];

        {
            //all memory slots contain 0 when first initialized
            Arrays.fill(objectMemory, sentinel);
        }

        //massive byte size means picking up causes sync issues
        @Override
        public boolean canPickup(){
            return false;
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
        public boolean readable(LExecutor exec){
            return isValid() && (exec.privileged || (this.team == exec.team && !this.block.privileged));
        }

        @Override
        public void read(LVar position, LVar output){
            int address = position.numi();
            //Return null when out of bounds. (instead of 0)
            if(address < 0 || address >= objectMemory.length){
                output.setobj(null);
                return;
            }

            Object value = objectMemory[address];
            if(value == sentinel){
                output.setnum(numberMemory[address]);
            }else{
                output.setobj(value);
            }
        }

        @Override
        public boolean writable(LExecutor exec){
            return readable(exec);
        }

        @Override
        public void write(LVar position, LVar value){
            int address = position.numi();
            if(address < 0 || address >= objectMemory.length) return;

            if(value.isobj) {
                objectMemory[address] = value.objval;
            }else{
                objectMemory[address] = sentinel;
                numberMemory[address] = value.numval;
            }
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

            write.i(objectMemory.length);
            for(int i = 0; i < objectMemory.length; i++){
                Object value = objectMemory[i];
                if(value == sentinel){
                    value = numberMemory[i];
                }

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
                    if(i < objectMemory.length){
                        objectMemory[i] = sentinel;
                        numberMemory[i] = val;
                    }
                }
                return;
            }

            //memory contents need to be temporarily stored in an array until they can be used
            Object[] values = new Object[Math.min(amount, objectMemory.length)];
            for(int i = 0; i < amount; i++){
                Object value = TypeIO.readObjectBoxed(read, true);
                if(i < values.length) values[i] = value;
            }

            loadBlock = () -> {
                //load up the memory contents that were stored
                for(int i = 0; i < values.length; i++){
                    Object value = values[i];
                    if (value instanceof Boxed<?> boxed) value = boxed.unbox();

                    if(value instanceof Number num){
                        objectMemory[i] = sentinel;
                        numberMemory[i] = num.doubleValue();
                    }else{
                        objectMemory[i] = value;
                    }
                }
            };
        }

        @Override
        public void afterReadAll(){
            super.afterReadAll();
            if(loadBlock != null){
                loadBlock.run();
                loadBlock = null;
            }
        }
    }
}
