package mindustry.world.blocks.logic;

import arc.math.*;
import mindustry.gen.*;

public class LogicExecutor{
    Instruction[] instructions;
    int[] registers = new int[256];
    int counter;

    void step(){
        if(instructions.length == 0) return;

        instructions[counter].exec();
        counter ++;

        //loop counter
        if(counter >= instructions.length) counter = 0;
    }

    Tilec device(short id){
        return null; //TODO
    }

    interface Instruction{
        void exec();
    }

    class RegisterI implements Instruction{
        /** operation to perform */
        Op op;
        /** destination register */
        short dest;
        /** registers to take data from. -1 for no register. */
        short left, right;
        /** left/right immediate values, only used if no registers are present. */
        int ileft, iright;

        @Override
        public void exec(){
            registers[dest] = op.function.get(left == -1 ? ileft : registers[left], right == -1 ? iright : registers[right]);
        }
    }

    class ReadI implements Instruction{
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
            registers[dest] = op.function.get(device(device), parameter);
        }
    }

    class WriteI implements Instruction{

        @Override
        public void exec(){

        }
    }

    enum ReadOp{
        item((tile, id) -> tile.items() == null ? 0 : tile.items().get(id)),
        itemTotal((tile, param) -> tile.items() == null ? 0 : tile.items().total());

        final ReadOpLambda function;
        final String symbol;

        ReadOp(ReadOpLambda function){
            this.symbol = name();
            this.function = function;
        }

        interface ReadOpLambda{
            int get(Tilec tile, int parameter);
        }
    }

    enum Op{
        add("+", (a, b) -> a + b),
        sub("-", (a, b) -> a - b),
        mul("*", (a, b) -> a * b),
        div("/", (a, b) -> a / b),
        mod("%", (a, b) -> a % b),
        pow("^", Mathf::pow),
        shl(">>", (a, b) -> a >> b),
        shr("<<", (a, b) -> a << b),
        or("or", (a, b) -> a | b),
        and("and", (a, b) -> a & b),
        xor("xor", (a, b) -> a ^ b);

        final OpLambda function;
        final String symbol;

        Op(String symbol, OpLambda function){
            this.symbol = symbol;
            this.function = function;
        }

        interface OpLambda{
            int get(int a, int b);
        }
    }


}
