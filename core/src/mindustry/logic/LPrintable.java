package mindustry.logic;

public interface LPrintable{
    boolean printable(LExecutor exec);
    void print(LVar position, StringBuilder text);
}
