package mindustry.logic;

public interface LogicReadable{
    boolean readable(LogicExecutor exec);
    void read(LogicVar position, LogicVar output);
}
