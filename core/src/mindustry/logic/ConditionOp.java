package mindustry.logic;

public enum ConditionOp{
    equal("==", (a, b) -> Math.abs(a - b) < 0.000001),
    notEqual("not", (a, b) -> Math.abs(a - b) >= 0.000001),
    lessThan("<", (a, b) -> a < b),
    lessThanEq("<=", (a, b) -> a <= b),
    greaterThan(">", (a, b) -> a > b),
    greaterThanEq(">=", (a, b) -> a >= b);

    public static final ConditionOp[] all = values();

    public final CondOpLambda function;
    public final String symbol;

    ConditionOp(String symbol, CondOpLambda function){
        this.symbol = symbol;
        this.function = function;
    }

    @Override
    public String toString(){
        return symbol;
    }

    interface CondOpLambda{
        boolean get(double a, double b);
    }
}
