package mindustry.logic;

import arc.math.*;

public enum LogicOp{
    add("+", (a, b) -> a + b),
    sub("-", (a, b) -> a - b),
    mul("*", (a, b) -> a * b),
    div("/", (a, b) -> a / b),
    idiv("//", (a, b) -> Math.floor(a / b)),
    mod("%", (a, b) -> a % b),
    equal("==", (a, b) -> Math.abs(a - b) < 0.000001 ? 1 : 0, (a, b) -> a == b ? 1 : 0),
    notEqual("not", (a, b) -> Math.abs(a - b) < 0.000001 ? 0 : 1, (a, b) -> a != b ? 1 : 0),
    lessThan("<", (a, b) -> a < b ? 1 : 0),
    lessThanEq("<=", (a, b) -> a <= b ? 1 : 0),
    greaterThan(">", (a, b) -> a > b ? 1 : 0),
    greaterThanEq(">=", (a, b) -> a >= b ? 1 : 0),
    pow("^", Math::pow),
    shl("<<", (a, b) -> (long)a << (long)b),
    shr(">>", (a, b) -> (long)a >> (long)b),
    or("or", (a, b) -> (long)a | (long)b),
    and("and", (a, b) -> (long)a & (long)b),
    xor("xor", (a, b) -> (long)a ^ (long)b),
    max("max", Math::max),
    min("min", Math::min),
    atan2("atan2", (x, y) -> Mathf.atan2((float)x, (float)y) * Mathf.radDeg),
    dst("dst", (x, y) -> Mathf.dst((float)x, (float)y)),
    noise("noise", LExecutor.noise::rawNoise2D),

    not("not", a -> ~(long)(a)),
    abs("abs", a -> Math.abs(a)),
    log("log", Math::log),
    log10("log10", Math::log10),
    sin("sin", d -> Math.sin(d * 0.017453292519943295D)),
    cos("cos", d -> Math.cos(d * 0.017453292519943295D)),
    tan("tan", d -> Math.tan(d * 0.017453292519943295D)),
    floor("floor", Math::floor),
    ceil("ceil", Math::ceil),
    sqrt("sqrt", Math::sqrt),
    rand("rand", d -> Mathf.rand.nextDouble() * d),

    ;

    public static final LogicOp[] all = values();

    public final OpObjLambda2 objFunction2;
    public final OpLambda2 function2;
    public final OpLambda1 function1;
    public final boolean unary;
    public final String symbol;

    LogicOp(String symbol, OpLambda2 function){
        this(symbol, function, null);
    }

    LogicOp(String symbol, OpLambda2 function, OpObjLambda2 objFunction){
        this.symbol = symbol;
        this.function2 = function;
        this.function1 = null;
        this.unary = false;
        this.objFunction2 = objFunction;
    }

    LogicOp(String symbol, OpLambda1 function){
        this.symbol = symbol;
        this.function1 = function;
        this.function2 = null;
        this.unary = true;
        this.objFunction2 = null;
    }

    @Override
    public String toString(){
        return symbol;
    }

    interface OpObjLambda2{
        double get(Object a, Object b);
    }

    interface OpLambda2{
        double get(double a, double b);
    }

    interface OpLambda1{
        double get(double a);
    }
}
