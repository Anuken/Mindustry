package mindustry.logic;

public enum ConditionOp{
    equal("==", (a, b) -> Math.abs(a - b) < 0.000001, (a, b) -> a == b),
    notEqual("not", (a, b) -> Math.abs(a - b) >= 0.000001, (a, b) -> a != b),
    lessThan("<", (a, b) -> a < b),
    lessThanEq("<=", (a, b) -> a <= b),
    greaterThan(">", (a, b) -> a > b),
    greaterThanEq(">=", (a, b) -> a >= b);

    public static final ConditionOp[] all = values();

    public final CondObjOpLambda objFunction;
    public final CondOpLambda function;
    public final String symbol;

    ConditionOp(String symbol, CondOpLambda function){
        this(symbol, function, null);
    }

    ConditionOp(String symbol, CondOpLambda function, CondObjOpLambda objFunction){
        this.symbol = symbol;
        this.function = function;
        this.objFunction = objFunction;
    }

    @Override
    public String toString(){
        return symbol;
    }

    interface CondObjOpLambda{
        boolean get(Object a, Object b);
    }

    interface CondOpLambda{
        boolean get(double a, double b);
    }
}
