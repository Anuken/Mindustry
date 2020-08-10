package mindustry.logic;

import arc.math.*;

public enum UnaryOp{
    negate("-", a -> -a),
    not("not", a -> ~(int)(a)),
    abs("abs", Math::abs),
    log("log", Math::log),
    log10("log10", Math::log10),
    sin("sin", Math::sin),
    cos("cos", Math::cos),
    tan("tan", Math::tan),
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
