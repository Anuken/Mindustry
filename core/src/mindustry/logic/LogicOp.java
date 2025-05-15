package mindustry.logic;

import arc.math.*;
import arc.util.*;
import arc.util.noise.*;

public enum LogicOp{
    add("+", (a, b) -> a + b),
    sub("-", (a, b) -> a - b),
    mul("*", (a, b) -> a * b),
    div("/", (a, b) -> a / b),
    idiv("//", (a, b) -> Math.floor(a / b)),
    mod("%", (a, b) -> a % b),
    pow("^", Math::pow),

    equal("==", (a, b) -> Math.abs(a - b) < 0.000001 ? 1 : 0, (a, b) -> Structs.eq(a, b) ? 1 : 0),
    notEqual("not", (a, b) -> Math.abs(a - b) < 0.000001 ? 0 : 1, (a, b) -> !Structs.eq(a, b) ? 1 : 0),
    land("and", (a, b) -> a != 0 && b != 0 ? 1 : 0),
    lessThan("<", (a, b) -> a < b ? 1 : 0),
    lessThanEq("<=", (a, b) -> a <= b ? 1 : 0),
    greaterThan(">", (a, b) -> a > b ? 1 : 0),
    greaterThanEq(">=", (a, b) -> a >= b ? 1 : 0),
    strictEqual("===", (a, b) -> 0), //this lambda is not actually used

    shl("<<", (a, b) -> (long)a << (long)b),
    shr(">>", (a, b) -> (long)a >> (long)b),
    or("or", (a, b) -> (long)a | (long)b),
    and("b-and", (a, b) -> (long)a & (long)b),
    xor("xor", (a, b) -> (long)a ^ (long)b),
    not("flip", a -> ~(long)(a)),

    max("max", true, Math::max),
    min("min", true, Math::min),
    angle("angle", true, (x, y) -> Angles.angle((float)x, (float)y)),
    angleDiff("anglediff", true, (x, y) -> Angles.angleDist((float)x, (float)y)),
    len("len", true, (x, y) -> Mathf.dst((float)x, (float)y)),
    noise("noise", true, (x, y) -> Simplex.raw2d(0, x, y)),
    abs("abs", a -> Math.abs(a)), //not a method reference because it fails to compile for some reason
    sign("sign", Math::signum),
    log("log", Math::log),
    log10("log10", Math::log10),
    floor("floor", Math::floor),
    ceil("ceil", Math::ceil),
    round("round", Math::round),
    sqrt("sqrt", Math::sqrt),
    rand("rand", d -> GlobalVars.rand.nextDouble() * d),

    sin("sin", d -> Math.sin(d * Mathf.doubleDegRad)),
    cos("cos", d -> Math.cos(d * Mathf.doubleDegRad)),
    tan("tan", d -> Math.tan(d * Mathf.doubleDegRad)),

    asin("asin", d -> Math.asin(d) * Mathf.doubleRadDeg),
    acos("acos", d -> Math.acos(d) * Mathf.doubleRadDeg),
    atan("atan", d -> Math.atan(d) * Mathf.doubleRadDeg),

    ;

    public static final LogicOp[] all = values();

    public final OpObjLambda2 objFunction2;
    public final OpLambda2 function2;
    public final OpLambda1 function1;
    public final boolean unary, func;
    public final String symbol;

    LogicOp(String symbol, OpLambda2 function){
        this(symbol, function, null);
    }

    LogicOp(String symbol, boolean func, OpLambda2 function){
        this.symbol = symbol;
        this.function2 = function;
        this.function1 = null;
        this.unary = false;
        this.objFunction2 = null;
        this.func = func;
    }

    LogicOp(String symbol, OpLambda2 function, OpObjLambda2 objFunction){
        this.symbol = symbol;
        this.function2 = function;
        this.function1 = null;
        this.unary = false;
        this.objFunction2 = objFunction;
        this.func = false;
    }

    LogicOp(String symbol, OpLambda1 function){
        this.symbol = symbol;
        this.function1 = function;
        this.function2 = null;
        this.unary = true;
        this.objFunction2 = null;
        this.func = false;
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
