package mindustry.logic;

import mindustry.gen.*;

import java.nio.*;

public class LogicExecutor{
    Instruction[] instructions;
    ByteBuffer memory = ByteBuffer.allocate(1024 * 512);
    int counter;

    void step(){
        if(instructions.length == 0) return;

        instructions[counter].exec();
        counter ++;

        //loop counter
        if(counter >= instructions.length) counter = 0;
    }

    Building device(short id){
        return null; //TODO
    }

    interface Instruction{
        void exec();
    }

    static class RegisterI implements Instruction{
        /** operation to perform */
        Op op;
        /** destination memory */
        int dest;
        /** memory to take data from. -1 for immediate values. */
        int left, right;
        /** left/right immediate values, only used if no registers are present. */
        double ileft, iright;

        @Override
        public void exec(){
            //memory.putDouble(dest, op.function.get(left == -1 ? ileft : registers[left], right == -1 ? iright : registers[right]));
        }
    }

    static class ReadI implements Instruction{
        /** register to write result to */
        short dest;
        /** device to read from */
        short device;
        /** the type of data to be read */
        ReadOp op;
        /** any additional read parameters */
        int parameter;

        @Override
        public void exec(){
            //registers[dest] = op.function.get(device(device), parameter);
        }
    }

    static class WriteI implements Instruction{

        @Override
        public void exec(){

        }
    }

    enum ReadOp{
        item((tile, id) -> tile.items == null ? 0 : tile.items.get(id)),
        itemTotal((tile, param) -> tile.items == null ? 0 : tile.items.total());

        final ReadOpLambda function;
        final String symbol;

        ReadOp(ReadOpLambda function){
            this.symbol = name();
            this.function = function;
        }

        interface ReadOpLambda{
            int get(Building tile, int parameter);
        }
    }

    enum Op{
        add("+", (a, b) -> a + b),
        sub("-", (a, b) -> a - b),
        mul("*", (a, b) -> a * b),
        div("/", (a, b) -> a / b),
        mod("%", (a, b) -> a % b),
        pow("^", Math::pow),
        shl(">>", (a, b) -> (int)a >> (int)b),
        shr("<<", (a, b) -> (int)a << (int)b),
        or("or", (a, b) -> (int)a | (int)b),
        and("and", (a, b) -> (int)a & (int)b),
        xor("xor", (a, b) -> (int)a ^ (int)b);

        final OpLambda function;
        final String symbol;

        Op(String symbol, OpLambda function){
            this.symbol = symbol;
            this.function = function;
        }

        interface OpLambda{
            double get(double a, double b);
        }
    }


}
