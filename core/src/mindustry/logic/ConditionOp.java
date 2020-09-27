package mindustry.logic;

import arc.Core;
import arc.input.*;

public enum ConditionOp {
    equal("==", (a, b) -> Math.abs(a - b) < 0.000001, (a, b) -> a == b, false),
    greaterThan(">", (a, b) -> a > b, false),
    greaterThanEq(">=", (a, b) -> a >= b, false),
    keyPressed("abc", (a, b) -> Core.input.keyDown(KeyCode.byOrdinal((int) b)), true),
    lessThan("<", (a, b) -> a < b, false),
    lessThanEq("<=", (a, b) -> a <= b, false),
    notEqual("not", (a, b) -> Math.abs(a - b) >= 0.000001, (a, b) -> a != b, false);


    public static final ConditionOp[] all = values();

    public final CondObjOpLambda objFunction;
    public final CondOpLambda function;
    public final String symbol;
    public final Boolean alone;

    ConditionOp(String symbol, CondOpLambda function, Boolean alone) {
        this(symbol, function, null, alone);
    }

    ConditionOp(String symbol, CondOpLambda function, CondObjOpLambda objFunction, Boolean alone) {
        this.symbol = symbol;
        this.function = function;
        this.objFunction = objFunction;
        this.alone = alone;
    }


    @Override
    public String toString() {
        return symbol;
    }

    interface CondObjOpLambda {
        boolean get(Object a, Object b);
    }

    interface CondOpLambda {
        boolean get(double a, double b);
    }
}
