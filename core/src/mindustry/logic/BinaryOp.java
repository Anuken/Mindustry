package mindustry.logic;

public enum BinaryOp{
    add("+", (a, b) -> a + b),
    sub("-", (a, b) -> a - b),
    mul("*", (a, b) -> a * b),
    div("/", (a, b) -> a / b),
    mod("%", (a, b) -> a % b),
    pow("^", Math::pow),
    shl(">>", (a, b) -> (int)a >> (int)b),
    shr("<<", (a, b) -> (int)a << (int)b),
    or("or", (a, b) -> (int)a | (int)b),
    and("and", (a, b) -> (int)a & (int)b),
    xor("xor", (a, b) -> (int)a ^ (int)b);

    final OpLambda function;
    final String symbol;

    BinaryOp(String symbol, OpLambda function){
        this.symbol = symbol;
        this.function = function;
    }

    public double get(double a, double b){
        return function.get(a, b);
    }

    interface OpLambda{
        double get(double a, double b);
    }
}
