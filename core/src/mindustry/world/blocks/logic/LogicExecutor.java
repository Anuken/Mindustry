package mindustry.world.blocks.logic;

import arc.math.*;

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

    interface Instruction{
        void exec();
    }

    class RegisterI{
        Op op;
        short from, to;
        int immediate;

        public void exec(){
            registers[to] = op.function.get(registers[from], immediate);
        }
    }

    static class ReadI{

    }

    static class WriteI{

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

        final BinaryOp function;
        final String symbol;

        Op(String symbol, BinaryOp function){
            this.symbol = symbol;
            this.function = function;
        }
    }

    interface BinaryOp{
        int get(int a, int b);
    }
}
