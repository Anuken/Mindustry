package mindustry.logic;

public interface LogicWritable{
    boolean writable(LogicExecutor exec);
    void write(LogicVar position, LogicVar value);
}
