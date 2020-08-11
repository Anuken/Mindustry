package mindustry.logic;

import arc.math.*;

public enum BinaryOp{
    add("+", (a, b) -> a + b),
    sub("-", (a, b) -> a - b),
    mul("*", (a, b) -> a * b),
    div("/", (a, b) -> a / b),
    mod("%", (a, b) -> a % b),
    equal("==", (a, b) -> Math.abs(a - b) < 0.000001 ? 1 : 0),
    notEqual("not", (a, b) -> Math.abs(a - b) < 0.000001 ? 0 : 1),
    lessThan("<", (a, b) -> a < b ? 1 : 0),
    lessThanEq("<=", (a, b) -> a <= b ? 1 : 0),
    greaterThan(">", (a, b) -> a > b ? 1 : 0),
    greaterThanEq(">=", (a, b) -> a >= b ? 1 : 0),
    pow("^", Math::pow),
    shl(">>", (a, b) -> (int)a >> (int)b),
    shr("<<", (a, b) -> (int)a << (int)b),
    or("or", (a, b) -> (int)a | (int)b),
    and("and", (a, b) -> (int)a & (int)b),
    xor("xor", (a, b) -> (int)a ^ (int)b),
    max("max", Math::max),
    min("min", Math::min),
    atan2("atan2", (x, y) -> Mathf.atan2((float)x, (float)y) * Mathf.radDeg),
    dst("dst", (x, y) -> Mathf.dst((float)x, (float)y));

    public static final BinaryOp[] all = values();

    public final OpLambda function;
    public final String symbol;

    BinaryOp(String symbol, OpLambda function){
        this.symbol = symbol;
        this.function = function;
    }

    @Override
    public String toString(){
        return symbol;
    }

    interface OpLambda{
        double get(double a, double b);
    }
}
