package mindustry.logic;

import arc.math.*;

public enum UnaryOp{
    negate("-", a -> -a),
    not("not", a -> ~(int)(a)),
    abs("abs", Math::abs),
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

    public static final UnaryOp[] all = values();

    public final UnaryOpLambda function;
    public final String symbol;

    UnaryOp(String symbol, UnaryOpLambda function){
        this.symbol = symbol;
        this.function = function;
    }

    @Override
    public String toString(){
        return symbol;
    }

    interface UnaryOpLambda{
        double get(double a);
    }
}
