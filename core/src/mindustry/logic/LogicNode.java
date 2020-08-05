package mindustry.logic;

import arc.func.*;

public abstract class LogicNode{

    public void run(){}

    public NodeInput[] inputs(){
        return new NodeInput[0];
    }

    public static class BinaryOpNode{
        public BinaryOp op = BinaryOp.add;
        public double a, b;
    }

    public static class NodeInput<T>{
        public boolean num;
        public String name;
        public SetNum setNum;
        public Cons<T> setObject;
    }

    public interface SetNum{
        void set(double val);
    }

}
