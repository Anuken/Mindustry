package mindustry.logic;

public interface LReadable{
    boolean readable(LExecutor exec);
    void read(LVar position, LVar output);
}
